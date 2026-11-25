package com.kotdex.compose

import com.kotdex.ClientExtension
import com.kotdex.compose.internal.ComposeExtension
import com.kotdex.compose.internal.ComposeRuntime
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import java.util.ServiceLoader
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExtensionTest {
    @Test
    fun `compose registers itself as a client extension`() {
        assertTrue(ServiceLoader.load(ClientExtension::class.java).stream().anyMatch { it.type() == ComposeExtension::class.java })
    }

    @Test
    fun `installed listener answers clicks on views from before a restart`() = runTest {
        val runtime = ComposeRuntime(StandardTestDispatcher(testScheduler))
        val listeners = mutableListOf<EventListener>()
        val builder = mockk<JDABuilder>(relaxed = true)
        every { builder.addEventListeners(*varargAllNullable { true }) } answers {
            listeners += (args[0] as Array<*>).map { it as EventListener }
            builder
        }
        val reply = mockk<ReplyCallbackAction>()
        every { reply.setEphemeral(any()) } returns reply
        every { reply.submit() } returns CompletableFuture.completedFuture(mockk())
        val click = mockk<ButtonInteractionEvent>(relaxed = true) {
            every { componentId } returns "kd:before-restart:0"
            every { reply(any<String>()) } returns reply
        }

        ComposeExtension(runtime).install(builder)
        listeners.forEach { it.onEvent(click) }
        runCurrent()

        verify { click.reply("This message has expired.") }
        runtime.close()
    }

    @Test
    fun `install skips a jda that already routes views`() = runTest {
        val runtime = ComposeRuntime(StandardTestDispatcher(testScheduler))
        val jda = mockk<JDA>(relaxed = true) { every { registeredListeners } returns listOf(runtime.listener) }

        runtime.install(jda)

        verify(exactly = 0) { jda.addEventListener(*anyVararg()) }
        runtime.close()
    }
}
