package com.kotdex.autocomplete

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command

public fun interface Autocompleter {
    public suspend fun complete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice>
}
