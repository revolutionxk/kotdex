package com.kotdex.example

import com.kotdex.annotations.Discord
import com.kotdex.annotations.On
import com.kotdex.annotations.Option
import com.kotdex.annotations.SimpleCommand
import com.kotdex.annotations.Slash
import com.kotdex.autocomplete.Autocompleter
import com.kotdex.coroutines.await
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.interactions.commands.Command
import org.slf4j.LoggerFactory
import kotlin.random.Random

object Languages : Autocompleter {
    private val names = listOf("Kotlin", "Rust", "Swift", "C++", "Go", "Dart")

    override suspend fun complete(event: CommandAutoCompleteInteractionEvent): List<Command.Choice> =
        names.filter { it.startsWith(event.focusedOption.value, ignoreCase = true) }.map { Command.Choice(it, it) }
}

@Discord
class General {
    private val logger = LoggerFactory.getLogger(General::class.java)

    @On
    fun ready(event: ReadyEvent) {
        logger.info("Logged in as {}", event.jda.selfUser.name)
    }

    @Slash("ping", "Check the gateway latency")
    suspend fun ping(event: SlashCommandInteractionEvent) {
        event.reply("Pong! ${event.jda.gatewayPing}ms").setEphemeral(true).await()
    }

    @Slash("roll", "Roll a die")
    suspend fun roll(event: SlashCommandInteractionEvent, @Option("sides", "Number of sides", min = 2.0, max = 100.0) sides: Int = 6) {
        event.reply("You rolled ${Random.nextInt(1, sides + 1)}").await()
    }

    @Slash("favorite", "Pick your favorite language")
    suspend fun favorite(
        event: SlashCommandInteractionEvent,
        @Option("language", "Language", autocomplete = Languages::class) language: String,
    ) {
        event.reply("$language, good choice.").await()
    }

    @SimpleCommand("hello", "Say hello", aliases = ["hi"])
    suspend fun hello(event: MessageReceivedEvent, author: User) {
        event.message.reply("Hello, ${author.asMention}!").await()
    }
}
