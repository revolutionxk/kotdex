package com.kotdex

import com.kotdex.internal.dispatch.defaultErrorHandler
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.requests.GatewayIntent

public class ClientOptions internal constructor() {
    public val intents: MutableSet<GatewayIntent> = GatewayIntent.getIntents(GatewayIntent.DEFAULT)
    public val guilds: MutableSet<Long> = mutableSetOf()
    public var prefix: String = "!"
    public var caseSensitive: Boolean = false
    public var registerCommands: Boolean = true

    internal var errorHandler: suspend (GenericEvent, Throwable) -> Unit = ::defaultErrorHandler
    internal val builders = mutableListOf<JDABuilder.() -> Unit>()

    public fun onError(block: suspend (event: GenericEvent, error: Throwable) -> Unit) {
        errorHandler = block
    }

    public fun jda(block: JDABuilder.() -> Unit) {
        builders += block
    }
}
