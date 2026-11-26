package com.kotdex.metadata

import com.kotdex.annotations.Option
import com.kotdex.autocomplete.Autocompleter
import com.kotdex.internal.options.OptionKind
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.reflect.KClass

public class OptionDescriptor internal constructor(
    public val name: String,
    public val description: String,
    public val type: OptionType,
    public val required: Boolean,
    public val rest: Boolean,
    internal val kind: OptionKind?,
    internal val annotation: Option,
    internal val autocompleter: Autocompleter?,
) {
    public val autocomplete: Boolean get() = autocompleter != null
}

public class SlashDescriptor internal constructor(
    public val path: List<String>,
    public val description: String,
    public val options: List<OptionDescriptor>,
    public val guilds: Set<Long>,
    public val permissions: Set<Permission>,
    public val guildOnly: Boolean,
    public val nsfw: Boolean,
) {
    public val name: String get() = path.joinToString(" ")
}

public class SlashGroupDescriptor internal constructor(public val name: String, public val description: String, public val root: String?)

public class ContextMenuDescriptor internal constructor(public val name: String, public val type: Command.Type)

public enum class ComponentKind { BUTTON, SELECT_MENU, MODAL }

public class ComponentDescriptor internal constructor(public val kind: ComponentKind, public val pattern: Regex)

public class SimpleCommandDescriptor internal constructor(
    public val name: String,
    public val aliases: List<String>,
    public val description: String,
    public val options: List<OptionDescriptor>,
) {
    public fun usage(prefix: String): String =
        (listOf("$prefix$name") + options.map { if (it.required) "<${it.name}>" else "[${it.name}]" }).joinToString(" ")
}

public class EventDescriptor internal constructor(public val type: KClass<out GenericEvent>, public val once: Boolean)
