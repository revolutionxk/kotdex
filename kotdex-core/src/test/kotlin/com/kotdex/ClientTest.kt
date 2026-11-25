package com.kotdex

import com.kotdex.annotations.Slash
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.seconds

class Who(private val seen: CompletableDeferred<Client>) {
    @Slash("who", "Who runs this")
    fun who(client: Client) {
        seen.complete(client)
    }
}

class ClientTest {
    private val failures = CompletableDeferred<Throwable>()
    private val client = Client { onError { _, error -> failures.complete(error) } }

    @AfterTest
    fun close() = client.shutdown()

    private val listeners = mutableListOf<EventListener>()

    private fun attachedBuilder(): JDABuilder {
        val builder = mockk<JDABuilder>(relaxed = true)
        every { builder.addEventListeners(*varargAllNullable { true }) } answers {
            listeners += (args[0] as Array<*>).map { it as EventListener }
            builder
        }
        return client.configure(builder)
    }

    private fun attachedListeners(): List<EventListener> {
        attachedBuilder()
        return listeners
    }

    private fun slash(name: String) = mockk<SlashCommandInteractionEvent>(relaxed = true) {
        every { this@mockk.name } returns name
        every { subcommandGroup } returns null
        every { subcommandName } returns null
    }

    @Test
    fun `extensions found on the classpath configure the builder`() {
        RecordingExtension.builders.clear()

        val builder = attachedBuilder()

        assertEquals(listOf(builder), RecordingExtension.builders)
    }

    @Test
    fun `jda throws before login`() {
        assertFailsWith<IllegalStateException> { client.jda }
    }

    @Test
    fun `scan registers discord classes from a package`() {
        client.scan("com.kotdex.scanned")

        assertEquals(setOf("scanned", "singleton"), client.metadata.slashCommands.map { it.name }.toSet())
    }

    @Test
    fun `listeners added before login are attached on login`() = runBlocking {
        val received = CompletableDeferred<ReadyEvent>()
        client.on<ReadyEvent> { received.complete(it) }
        val ready = mockk<ReadyEvent>(relaxed = true)

        attachedListeners().forEach { it.onEvent(ready) }

        assertSame(ready, withTimeout(5.seconds) { received.await() })
    }

    @Test
    fun `listener errors reach onError`() = runBlocking {
        client.on<GenericEvent> { error("boom") }

        attachedListeners().forEach { it.onEvent(mockk<ReadyEvent>(relaxed = true)) }

        assertEquals("boom", withTimeout(5.seconds) { failures.await() }.message)
    }

    @Test
    fun `handlers can receive the client`() = runBlocking {
        val seen = CompletableDeferred<Client>()
        client.register(Who(seen))

        attachedListeners().forEach { it.onEvent(slash("who")) }

        assertSame(client, withTimeout(5.seconds) { seen.await() })
    }
}
