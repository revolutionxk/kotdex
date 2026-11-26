package com.kotdex.internal.dispatch

import com.kotdex.coroutines.await
import com.kotdex.internal.handlers.Compiled
import com.kotdex.internal.handlers.Handler
import com.kotdex.metadata.MetadataStorage
import com.kotdex.metadata.SimpleCommandDescriptor
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

internal class SimpleCommands(
    private val metadata: MetadataStorage,
    private val prefix: String,
    private val caseSensitive: Boolean,
    private val run: suspend (Handler, GenericEvent, Map<String, Any?>) -> Unit,
) {
    suspend fun handle(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val content = event.message.contentRaw
        if (!content.startsWith(prefix)) return

        val tokens = tokenize(content.substring(prefix.length))
        val name = tokens.firstOrNull() ?: return
        val command = metadata.simple.firstOrNull { it.descriptor.answersTo(name) } ?: return
        val arguments = parse(command, tokens.drop(1), event)
        if (arguments == null) {
            event.message.reply("Usage: ${command.descriptor.usage(prefix)}").await()
            return
        }
        run(command.handler, event, arguments)
    }

    private fun SimpleCommandDescriptor.answersTo(name: String) = (aliases + this.name).any { it.equals(name, ignoreCase = !caseSensitive) }

    private fun parse(command: Compiled<SimpleCommandDescriptor>, tokens: List<String>, event: MessageReceivedEvent): Map<String, Any?>? {
        val arguments = mutableMapOf<String, Any?>()
        var index = 0
        for (option in command.descriptor.options) {
            if (option.rest) {
                val rest = tokens.drop(index)
                if (option.required && rest.isEmpty()) return null
                arguments[option.name] = rest
                index = tokens.size
                continue
            }
            val raw = tokens.getOrNull(index++)
            if (raw == null) {
                if (option.required) return null
                arguments[option.name] = null
                continue
            }
            arguments[option.name] = option.kind?.parse(raw, event.message) ?: return null
        }
        return arguments
    }
}
