package com.kotdex.internal.dispatch

import com.kotdex.annotations.Option
import com.kotdex.annotations.SimpleCommand
import com.kotdex.metadata.MetadataStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrefixCommands {
    val received = mutableListOf<Any?>()

    @SimpleCommand("ping", aliases = ["p"])
    fun ping(event: MessageReceivedEvent) {
        received += "pong"
    }

    @SimpleCommand("add")
    fun add(@Option("a") a: Int, @Option("b") b: Int?) {
        received += a + (b ?: 0)
    }

    @SimpleCommand("say")
    fun say(@Option("words") words: List<String>) {
        received.add(words)
    }
}

class SimpleCommandsTest {
    private val commands = PrefixCommands()
    private val dispatcher = Dispatcher(MetadataStorage(emptySet()).apply { register(commands) }, { emptyList() }, ::defaultErrorHandler)

    private fun message(content: String, bot: Boolean = false) = mockk<MessageReceivedEvent>(relaxed = true) {
        every { author.isBot } returns bot
        every { message.contentRaw } returns content
        val action = mockk<MessageCreateAction> { every { submit() } returns CompletableFuture.completedFuture(mockk()) }
        every { message.reply(any<String>()) } returns action
    }

    @Test
    fun `tokenize groups quotes`() {
        assertEquals(listOf("say", "hello world", "now"), tokenize("""say "hello world"   now"""))
    }

    @Test
    fun `routes by alias`() = runTest {
        dispatcher.dispatch(message("!p"))

        assertEquals(listOf<Any?>("pong"), commands.received)
    }

    @Test
    fun `matches names ignoring case`() = runTest {
        dispatcher.dispatch(message("!PING"))

        assertEquals(listOf<Any?>("pong"), commands.received)
    }

    @Test
    fun `ignores bots and messages without prefix`() = runTest {
        dispatcher.dispatch(message("!ping", bot = true))
        dispatcher.dispatch(message("ping"))

        assertTrue(commands.received.isEmpty())
    }

    @Test
    fun `parses typed arguments`() = runTest {
        dispatcher.dispatch(message("!add 2 3"))
        dispatcher.dispatch(message("!add 2"))

        assertEquals(listOf<Any?>(5, 2), commands.received)
    }

    @Test
    fun `missing argument replies with usage`() = runTest {
        val event = message("!add")

        dispatcher.dispatch(event)

        verify { event.message.reply("Usage: !add <a> [b]") }
        assertTrue(commands.received.isEmpty())
    }

    @Test
    fun `bad int replies with usage`() = runTest {
        val event = message("!add two")

        dispatcher.dispatch(event)

        verify { event.message.reply("Usage: !add <a> [b]") }
    }

    @Test
    fun `rest list collects remaining words`() = runTest {
        dispatcher.dispatch(message("!say hello big world"))

        assertEquals(listOf<Any?>(listOf("hello", "big", "world")), commands.received)
    }
}
