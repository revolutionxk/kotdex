package com.kotdex.internal.handlers

import com.kotdex.annotations.Option
import com.kotdex.annotations.SimpleCommand
import com.kotdex.annotations.Slash
import com.kotdex.annotations.SlashGroup
import com.kotdex.metadata.MetadataStorage
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class Service

class Greetings {
    val calls = mutableListOf<List<Any?>>()

    @Slash("greet", "Greets someone")
    fun greet(
        event: SlashCommandInteractionEvent,
        @Option("name", "Who") name: String,
        @Option("times", "How often") times: Int?,
        @Option("loud", "Shout") loud: Boolean = true,
    ) {
        calls += listOf(event, name, times, loud)
    }

    @Slash("inject", "Injects a service")
    suspend fun inject(service: Service) {
        calls += listOf(service)
    }
}

@SlashGroup("config", "Bot settings")
class Config {
    @Slash("set", "Set a value")
    fun set(@Option("key", "Key") key: String) {}
}

@SlashGroup("prefix", "Prefix settings", root = "config")
class PrefixConfig {
    @Slash("show", "Show the prefix")
    fun show() {}
}

class DuplicateGreet {
    @Slash("greet", "Again")
    fun again() {}
}

class ConfigCommand {
    @Slash("config", "Clashes with the group")
    fun config() {}
}

class Unsupported {
    @Slash("thread", "Bad option")
    fun thread(@Option("t", "Thread") t: Thread) {}
}

class Ping {
    @SimpleCommand("ping", aliases = ["p"])
    fun ping(event: MessageReceivedEvent) {}
}

class Pong {
    @SimpleCommand("pong", aliases = ["P"])
    fun pong(event: MessageReceivedEvent) {}
}

class HandlerCompilerTest {
    private val storage = MetadataStorage(setOf(Service::class))

    private fun slash(path: String) = storage.slash.single { it.descriptor.path.joinToString(" ") == path }

    @Test
    fun `compiles options in declaration order`() {
        storage.register(Greetings())

        val options = slash("greet").descriptor.options
        assertEquals(listOf("name", "times", "loud"), options.map { it.name })
        assertEquals(listOf(OptionType.STRING, OptionType.INTEGER, OptionType.BOOLEAN), options.map { it.type })
    }

    @Test
    fun `nullable option is optional`() {
        storage.register(Greetings())

        assertEquals(listOf(true, false, false), slash("greet").descriptor.options.map { it.required })
    }

    @Test
    fun `default value option is optional and uses the kotlin default`() = runTest {
        val greetings = Greetings()
        storage.register(greetings)
        val event = mockk<SlashCommandInteractionEvent>()

        slash("greet").handler.invoke(InvocationContext(event, mapOf("name" to "Ana", "times" to null), emptyList()))

        assertEquals(listOf(event, "Ana", null, true), greetings.calls.single())
    }

    @Test
    fun `injects event and services by type`() = runTest {
        val greetings = Greetings()
        val service = Service()
        storage.register(greetings)

        slash("inject").handler.invoke(InvocationContext(mockk<SlashCommandInteractionEvent>(), emptyMap(), listOf(service)))

        assertSame(service, greetings.calls.single().single())
    }

    @Test
    fun `slash group class prefixes the path`() {
        storage.register(Config(), PrefixConfig())

        assertEquals(listOf(listOf("config", "set"), listOf("config", "prefix", "show")), storage.slashCommands.map { it.path })
        assertEquals(listOf("config"), storage.slashGroups.filter { it.root == null }.map { it.name })
    }

    @Test
    fun `rejects duplicate slash path naming both functions`() {
        storage.register(Greetings())

        val error = assertFailsWith<IllegalStateException> { storage.register(DuplicateGreet()) }
        assertContains(error.message!!, "Greetings.greet")
        assertContains(error.message!!, "DuplicateGreet.again")
    }

    @Test
    fun `rejects a command that is also a group`() {
        storage.register(Config())

        val error = assertFailsWith<IllegalStateException> { storage.register(ConfigCommand()) }
        assertContains(error.message!!, "ConfigCommand.config")
    }

    @Test
    fun `rejects unsupported parameter type naming the function`() {
        val error = assertFailsWith<IllegalStateException> { storage.register(Unsupported()) }
        assertContains(error.message!!, "Unsupported.thread")
    }

    @Test
    fun `rejects duplicate simple command alias`() {
        storage.register(Ping())

        val error = assertFailsWith<IllegalStateException> { storage.register(Pong()) }
        assertContains(error.message!!, "Ping.ping")
        assertContains(error.message!!, "Pong.pong")
    }
}
