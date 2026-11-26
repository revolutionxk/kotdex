package com.kotdex.internal.dispatch

import com.kotdex.coroutines.await
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import org.slf4j.LoggerFactory

internal val logger = LoggerFactory.getLogger("com.kotdex.Dispatcher")

private const val FAILURE = "Something went wrong while running this command."

internal suspend fun defaultErrorHandler(event: GenericEvent, error: Throwable) {
    logger.error("Handler failed while processing {}", event.javaClass.simpleName, error)
    runCatching {
        when (event) {
            is IReplyCallback ->
                if (event.isAcknowledged) {
                    event.hook.sendMessage(FAILURE).setEphemeral(true).await()
                } else {
                    event.reply(FAILURE).setEphemeral(true).await()
                }
            is MessageReceivedEvent -> event.message.reply(FAILURE).await()
        }
    }.onFailure { logger.warn("Could not report the failure to the user", it) }
}
