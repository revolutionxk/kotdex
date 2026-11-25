package com.kotdex.internal.handlers

import com.kotdex.annotations.ButtonComponent
import com.kotdex.annotations.ContextMenu
import com.kotdex.annotations.Guarded
import com.kotdex.annotations.ModalComponent
import com.kotdex.annotations.On
import com.kotdex.annotations.Once
import com.kotdex.annotations.Option
import com.kotdex.annotations.SelectMenuComponent
import com.kotdex.annotations.SimpleCommand
import com.kotdex.annotations.Slash
import com.kotdex.annotations.SlashGroup
import com.kotdex.autocomplete.Autocompleter
import com.kotdex.guards.Guard
import com.kotdex.internal.options.optionKindOf
import com.kotdex.metadata.ComponentDescriptor
import com.kotdex.metadata.ComponentKind
import com.kotdex.metadata.ContextMenuDescriptor
import com.kotdex.metadata.EventDescriptor
import com.kotdex.metadata.OptionDescriptor
import com.kotdex.metadata.SimpleCommandDescriptor
import com.kotdex.metadata.SlashDescriptor
import com.kotdex.metadata.SlashGroupDescriptor
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

internal class CompiledClass(
    val slash: List<Compiled<SlashDescriptor>>,
    val groups: List<SlashGroupDescriptor>,
    val contextMenus: List<Compiled<ContextMenuDescriptor>>,
    val components: List<Compiled<ComponentDescriptor>>,
    val simpleCommands: List<Compiled<SimpleCommandDescriptor>>,
    val events: List<Compiled<EventDescriptor>>,
)

internal class HandlerCompiler(private val injectables: Set<KClass<*>>) {
    private val singletons = ConcurrentHashMap<KClass<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> single(type: KClass<out T>): T = singletons.getOrPut(type) { instantiate(type) } as T

    fun compile(instance: Any): CompiledClass {
        val type = instance::class
        val group = type.findAnnotation<SlashGroup>()
        val classGuards = type.findAnnotation<Guarded>()?.value.orEmpty().map { single(it) }

        val slashCommands = mutableListOf<Compiled<SlashDescriptor>>()
        val contextMenus = mutableListOf<Compiled<ContextMenuDescriptor>>()
        val components = mutableListOf<Compiled<ComponentDescriptor>>()
        val simpleCommands = mutableListOf<Compiled<SimpleCommandDescriptor>>()
        val events = mutableListOf<Compiled<EventDescriptor>>()

        for (function in type.memberFunctions.sortedBy { it.name }) {
            val method = Method(type, instance, function, classGuards)

            function.findAnnotation<Slash>()?.let { slash ->
                val (handler, options) = method.compile(SlashCommandInteractionEvent::class.java, allowOptions = true)
                val path = when {
                    group == null -> listOf(slash.name)
                    group.root.isEmpty() -> listOf(group.name, slash.name)
                    else -> listOf(group.root, group.name, slash.name)
                }
                val descriptor = SlashDescriptor(
                    path = path,
                    description = slash.description,
                    options = options,
                    guilds = slash.guilds.toSet(),
                    permissions = slash.permissions.toSet(),
                    guildOnly = slash.guildOnly,
                    nsfw = slash.nsfw,
                )
                Compiled(descriptor, handler)
            }?.let(slashCommands::add)

            function.findAnnotation<ContextMenu>()?.let { menu ->
                val event = when (menu.type) {
                    Command.Type.USER -> UserContextInteractionEvent::class.java
                    Command.Type.MESSAGE -> MessageContextInteractionEvent::class.java
                    else -> error("Context menu ${method.name} must be USER or MESSAGE")
                }
                Compiled(ContextMenuDescriptor(menu.name, menu.type), method.compile(event).first)
            }?.let(contextMenus::add)

            function.findAnnotation<ButtonComponent>()?.let {
                method.component(ComponentKind.BUTTON, it.id, ButtonInteractionEvent::class.java)
            }?.let(components::add)

            function.findAnnotation<SelectMenuComponent>()?.let {
                method.component(ComponentKind.SELECT_MENU, it.id, GenericSelectMenuInteractionEvent::class.java)
            }?.let(components::add)

            function.findAnnotation<ModalComponent>()?.let {
                method.component(ComponentKind.MODAL, it.id, ModalInteractionEvent::class.java)
            }?.let(components::add)

            function.findAnnotation<SimpleCommand>()?.let { command ->
                val (handler, options) = method.compile(MessageReceivedEvent::class.java, allowOptions = true, allowRest = true)
                val descriptor = SimpleCommandDescriptor(
                    name = command.name.ifEmpty { function.name },
                    aliases = command.aliases.toList(),
                    description = command.description,
                    options = options,
                )
                Compiled(descriptor, handler)
            }?.let(simpleCommands::add)

            function.findAnnotation<On>()?.let { method.event(once = false) }?.let(events::add)
            function.findAnnotation<Once>()?.let { method.event(once = true) }?.let(events::add)
        }

        val groups = listOfNotNull(group?.let { SlashGroupDescriptor(it.name, it.description, it.root.ifEmpty { null }) })
        return CompiledClass(slashCommands, groups, contextMenus, components, simpleCommands, events)
    }

    private inner class Method(
        type: KClass<*>,
        private val instance: Any,
        private val function: KFunction<*>,
        private val classGuards: List<Guard>,
    ) {
        val name = "${type.simpleName}.${function.name}"

        fun component(kind: ComponentKind, id: String, event: Class<*>): Compiled<ComponentDescriptor> =
            Compiled(ComponentDescriptor(kind, Regex(id.ifEmpty { function.name })), compile(event).first)

        @Suppress("UNCHECKED_CAST")
        fun event(once: Boolean): Compiled<EventDescriptor> {
            val handler = compile(GenericEvent::class.java).first
            val type = function.parameters
                .firstOrNull { it.kind == KParameter.Kind.VALUE && GenericEvent::class.java.isAssignableFrom(it.type.jvmErasure.java) }
                ?.type?.jvmErasure as? KClass<out GenericEvent>
                ?: error("Event listener $name needs an event parameter")
            return Compiled(EventDescriptor(type, once), handler)
        }

        fun compile(
            event: Class<*>,
            allowOptions: Boolean = false,
            allowRest: Boolean = false,
        ): Pair<Handler, List<OptionDescriptor>> {
            function.isAccessible = true
            var eventType = event
            val resolvers = mutableListOf<ParameterResolver>()
            val options = mutableListOf<OptionDescriptor>()

            for (parameter in function.parameters.filter { it.kind == KParameter.Kind.VALUE }) {
                val option = parameter.findAnnotation<Option>()
                if (option != null) {
                    check(allowOptions) { "@Option is not supported on ${parameter.name} in $name" }
                    check(options.lastOrNull()?.rest != true) { "Only the last option of $name can take the remaining arguments" }
                    val descriptor = option(parameter, option, allowRest)
                    check(!descriptor.required || options.none { !it.required }) {
                        "Required option ${descriptor.name} must come before optional options in $name"
                    }
                    options += descriptor
                    resolvers += OptionParameter(parameter, descriptor)
                    continue
                }

                val type = parameter.type.jvmErasure
                when {
                    type.java.isAssignableFrom(eventType) -> Unit
                    eventType.isAssignableFrom(type.java) -> eventType = type.java
                    type in injectables -> Unit
                    else -> error("Cannot provide parameter ${parameter.name}: ${parameter.type} in $name")
                }
                resolvers += InjectedParameter(parameter, type.javaObjectType)
            }

            val guards = classGuards + function.findAnnotation<Guarded>()?.value.orEmpty().map { single(it) }
            return Handler(name, function, instance, eventType, guards, resolvers) to options
        }

        private fun option(parameter: KParameter, option: Option, allowRest: Boolean): OptionDescriptor {
            val required = !parameter.type.isMarkedNullable && !parameter.isOptional
            val description = option.description.ifEmpty { option.name }
            val rest = allowRest && parameter.type.jvmErasure == List::class &&
                parameter.type.arguments.singleOrNull()?.type?.jvmErasure == String::class
            if (rest) {
                return OptionDescriptor(option.name, description, OptionType.STRING, required, true, null, option, null)
            }
            val kind = optionKindOf(parameter.type)
                ?: error("Unsupported option type ${parameter.type} for ${parameter.name} in $name")
            val autocompleter = option.autocomplete.takeIf { it != Autocompleter::class }?.let { single(it) }
            return OptionDescriptor(option.name, description, kind.type, required, false, kind, option, autocompleter)
        }
    }
}
