package com.kotdex.example

import com.kotdex.annotations.ButtonComponent
import com.kotdex.annotations.Discord
import com.kotdex.annotations.Guarded
import com.kotdex.annotations.Option
import com.kotdex.annotations.Slash
import com.kotdex.annotations.SlashGroup
import com.kotdex.builders.components.danger
import com.kotdex.builders.components.row
import com.kotdex.coroutines.await
import com.kotdex.guards.Guard
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback

object Moderators : Guard {
    override suspend fun check(event: GenericEvent): Boolean {
        val allowed = (event as? Interaction)?.member?.hasPermission(Permission.MODERATE_MEMBERS) == true
        if (!allowed && event is IReplyCallback) event.reply("Moderators only.").setEphemeral(true).await()
        return allowed
    }
}

@Discord
@SlashGroup("mod", "Moderation tools")
@Guarded(Moderators::class)
class Moderation {
    @Slash("warn", "Warn a member", guildOnly = true)
    suspend fun warn(
        event: SlashCommandInteractionEvent,
        @Option("member", "Who to warn") member: Member,
        @Option("reason", "Why", maxLength = 200) reason: String?,
    ) {
        event.reply("${member.asMention} was warned${reason?.let { ": $it" } ?: "."}")
            .addComponents(row(danger("undo-warn:${member.id}", "Undo")))
            .await()
    }

    @ButtonComponent("undo-warn:\\d+")
    suspend fun undo(event: ButtonInteractionEvent) {
        event.editMessage("Warning removed.").setComponents().await()
    }
}
