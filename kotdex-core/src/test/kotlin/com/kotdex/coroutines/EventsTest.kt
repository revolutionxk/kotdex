package com.kotdex.coroutines

import com.kotdex.events.once
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class EventsTest {
    private val listeners = mutableListOf<EventListener>()

    private val jda = mockk<JDA>(relaxed = true) {
        every { addEventListener(*anyVararg()) } answers {
            (args[0] as Array<*>).forEach { listeners += it as EventListener }
        }
        every { removeEventListener(*anyVararg()) } answers {
            (args[0] as Array<*>).forEach { listeners -= it as EventListener }
        }
    }

    private fun fire(event: GenericEvent) = listeners.toList().forEach { it.onEvent(event) }

    private fun message(id: Long) = mockk<MessageReceivedEvent> { every { messageIdLong } returns id }

    @Test
    fun `await returns the first matching event`() = runTest {
        val result = async { jda.await<MessageReceivedEvent> { it.messageIdLong == 2L } }
        runCurrent()

        fire(message(1))
        fire(message(2))
        fire(message(2))

        assertEquals(2L, result.await().messageIdLong)
        assertTrue(listeners.isEmpty())
    }

    @Test
    fun `await times out`() = runTest {
        assertFailsWith<TimeoutCancellationException> {
            jda.await<MessageReceivedEvent>(timeout = 1.seconds)
        }
        assertTrue(listeners.isEmpty())
    }

    @Test
    fun `once runs a single time`() {
        var calls = 0
        jda.once<MessageReceivedEvent> { calls++ }

        fire(message(1))
        fire(message(2))

        assertEquals(1, calls)
        assertTrue(listeners.isEmpty())
    }
}
