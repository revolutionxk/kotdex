package com.kotdex.example

import com.kotdex.Client
import net.dv8tion.jda.api.requests.GatewayIntent

suspend fun main() {
    val client = Client {
        intents += GatewayIntent.MESSAGE_CONTENT
        prefix = "!"
        env("DISCORD_GUILD")?.toLongOrNull()?.let(guilds::add)
    }

    client.scan("com.kotdex.example")
    client.login(env("DISCORD_TOKEN") ?: error("Set DISCORD_TOKEN in .env or in the environment"))
}
