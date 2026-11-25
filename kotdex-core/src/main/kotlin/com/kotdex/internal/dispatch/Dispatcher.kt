package com.kotdex.internal.dispatch

import com.kotdex.coroutines.await
import com.kotdex.internal.handlers.Handler
import com.kotdex.internal.handlers.InvocationContext
import com.kotdex.metadata.ComponentKind
import com.kotdex.metadata.MetadataStorage
import kotlinx.coroutines.CancellationException
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.GenericContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload
import java.util.concurrent.ConcurrentHashMap

internal const val RESERVED_PREFIX = "kd:"

internal class Dispatcher(
    private val metadata: MetadataStorage,
    private val injectables: (GenericEvent) -> List<Any>,
    private val onError: suspend (GenericEvent, Throwable) -> Unit,
) {
    private val fired = ConcurrentHashMap.newKeySet<Handler>()

    suspend fun dispatch(event: GenericEvent) {
        when (event) {
            is SlashCommandInteractionEvent -> slash(event)
            is CommandAutoCompleteInteractionEvent -> autocomplete(event)
            is GenericContextInteractionEvent<*> -> contextMenu(event)
            is ButtonInteractionEvent -> component(ComponentKind.BUTTON, event.componentId, event)
            is GenericSelectMenuInteractionEvent<*, *> -> component(ComponentKind.SELECT_MENU, event.componentId, event)
            is ModalInteractionEvent -> component(ComponentKind.MODAL, event.modalId, event)
        }
        listeners(event)
    }

    suspend fun run(handler: Handler, event: GenericEvent, arguments: Map<String, Any?> = emptyMap()) {
        try {
            for (guard in handler.guards) {
                if (!guard.check(event)) return
            }
            handler.invoke(InvocationContext(event, arguments, injectables(event)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            onError(event, e)
        }
    }

    private suspend fun slash(event: SlashCommandInteractionEvent) {
        val command = metadata.slash.firstOrNull { it.descriptor.path == event.path() } ?: return
        val arguments = command.descriptor.options.associate { option ->
            option.name to event.getOption(option.name)?.let { option.kind?.read(it) }
        }
        run(command.handler, event, arguments)
    }

    private suspend fun autocomplete(event: CommandAutoCompleteInteractionEvent) {
        val focused = event.focusedOption.name
        val completer = metadata.slash
            .firstOrNull { it.descriptor.path == event.path() }
            ?.descriptor?.options?.firstOrNull { it.name == focused }
            ?.autocompleter
        try {
            val choices = completer?.complete(event).orEmpty()
            event.replyChoices(choices.take(25)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.error("Autocomplete failed for option {}", focused, e)
        }
    }

    private suspend fun contextMenu(event: GenericContextInteractionEvent<*>) {
        val menu = metadata.contextMenus.firstOrNull {
            it.descriptor.name == event.name && it.descriptor.type == event.commandType
        } ?: return
        run(menu.handler, event)
    }

    private suspend fun component(kind: ComponentKind, id: String, event: GenericEvent) {
        if (id.startsWith(RESERVED_PREFIX)) return
        val component = metadata.components
            .filter { it.descriptor.kind == kind && it.descriptor.pattern.matches(id) && it.handler.accepts(event) }
            .maxWithOrNull(compareBy({ it.descriptor.pattern.pattern == id }, { it.descriptor.pattern.pattern.length }))
            ?: return
        run(component.handler, event)
    }

    private suspend fun listeners(event: GenericEvent) {
        for (listener in metadata.events) {
            if (!listener.handler.accepts(event)) continue
            if (listener.descriptor.once && !fired.add(listener.handler)) continue
            run(listener.handler, event)
        }
    }

    private fun CommandInteractionPayload.path() = listOfNotNull(name, subcommandGroup, subcommandName)
}
