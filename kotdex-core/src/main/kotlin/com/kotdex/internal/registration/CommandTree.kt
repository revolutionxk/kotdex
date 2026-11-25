package com.kotdex.internal.registration

import com.kotdex.metadata.MetadataStorage
import com.kotdex.metadata.OptionDescriptor
import com.kotdex.metadata.SlashDescriptor
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData

internal fun buildCommands(metadata: MetadataStorage, devGuilds: Set<Long>): Map<Long?, List<CommandData>> {
    fun placement(guilds: Set<Long>): Set<Long?> = when {
        devGuilds.isNotEmpty() -> devGuilds
        guilds.isEmpty() -> setOf(null)
        else -> guilds
    }

    val slash = metadata.slashCommands.flatMap { command -> placement(command.guilds).map { it to command } }
    val menus = metadata.contextMenuCommands.flatMap { menu -> placement(emptySet()).map { it to menu } }
    val keys = (slash.map { it.first } + menus.map { it.first }).toSet()

    return keys.associateWith { key ->
        val roots = slash.filter { it.first == key }.map { it.second }.groupBy { it.path.first() }
        val commands = roots.map { (root, commands) -> root(root, commands, metadata) }
        commands + menus.filter { it.first == key }.map { (_, menu) -> Commands.context(menu.type, menu.name) }
    }
}

private fun root(name: String, commands: List<SlashDescriptor>, metadata: MetadataStorage): SlashCommandData {
    val plain = commands.singleOrNull()?.takeIf { it.path.size == 1 }
    val description = plain?.description
        ?: metadata.slashGroups.firstOrNull { it.name == name && it.root == null }?.description
        ?: name
    val data = Commands.slash(name, description)

    if (plain != null) {
        data.addOptions(plain.options.map(OptionDescriptor::toData))
    } else {
        commands.filter { it.path.size == 2 }.forEach { data.addSubcommands(it.subcommand()) }
        commands.filter { it.path.size == 3 }.groupBy { it.path[1] }.forEach { (group, subcommands) ->
            val groupDescription = metadata.slashGroups.firstOrNull { it.name == group && it.root == name }?.description ?: group
            data.addSubcommandGroups(SubcommandGroupData(group, groupDescription).addSubcommands(subcommands.map { it.subcommand() }))
        }
    }

    val permissions = commands.flatMap { it.permissions }.toSet()
    if (permissions.isNotEmpty()) data.setDefaultPermissions(DefaultMemberPermissions.enabledFor(permissions))
    if (commands.any { it.guildOnly }) data.setContexts(InteractionContextType.GUILD)
    if (commands.any { it.nsfw }) data.setNSFW(true)
    return data
}

private fun SlashDescriptor.subcommand(): SubcommandData =
    SubcommandData(path.last(), description).addOptions(options.map(OptionDescriptor::toData))

private fun OptionDescriptor.toData(): OptionData {
    val data = OptionData(type, name, description, required, autocomplete)
    kind?.configure(data)
    val integer = type == OptionType.INTEGER
    annotation.min.takeUnless(Double::isNaN)?.let { if (integer) data.setMinValue(it.toLong()) else data.setMinValue(it) }
    annotation.max.takeUnless(Double::isNaN)?.let { if (integer) data.setMaxValue(it.toLong()) else data.setMaxValue(it) }
    annotation.minLength.takeIf { it >= 0 }?.let(data::setMinLength)
    annotation.maxLength.takeIf { it >= 0 }?.let(data::setMaxLength)
    return data
}
