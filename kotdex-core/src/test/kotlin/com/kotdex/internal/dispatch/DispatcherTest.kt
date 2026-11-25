package com.kotdex.internal.dispatch

import com.kotdex.annotations.ButtonComponent
import com.kotdex.annotations.Guarded
import com.kotdex.annotations.Once
import com.kotdex.annotations.Option
import com.kotdex.annotations.Slash
import com.kotdex.annotations.SlashGroup
import com.kotdex.autocomplete.Autocompleter
import com.kotdex.guards.Guard
import com.kotdex.metadata.MetadataStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.requests.restaction.WebhookMessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

val calls = mutableListOf<String>()

object Deny : Guard {
    override suspend fun check(event: GenericEvent) = false
}

object First : Guard {
    override suspend fun check(event: GenericEvent): Boolean {
        calls += "first"
        return true
    }
}

object Second : Guard {
    override suspend fun check(event: GenericEvent): Boolean {
        calls += "second"
        return true
    }
}

object Colors : Autocompleter {
    override suspend fun complete(event: CommandAutoCompleteInteractionEvent) = listOf(Command.Choice("Red", "red"))
}

@Guarded(First::class)
class Commands {
    @Slash("echo", "Echo")
    fun echo(@Option("text") text: String, @Option("count") count: Int?, @Option("color", autocomplete = Colors::class) color: String?) {
        calls += "echo $text $count"
    }

    @Slash("guarded", "Guarded")
    @Guarded(Second::class)
    fun guarded() {
        calls += "guarded"
    }

    @Slash("denied", "Denied")
    @Guarded(Deny::class)
    fun denied() {
        calls += "denied"
    }

    @Slash("fail", "Fails")
    fun fail(): Unit = error("boom")

    @ButtonComponent("vote-\\d+")
    fun vote(event: ButtonInteractionEvent) {
        calls += "vote ${event.componentId}"
    }

    @ButtonComponent(".*")
    fun any(event: ButtonInteractionEvent) {
        calls += "any ${event.componentId}"
    }

    @Once
    fun ready(event: ReadyEvent) {
        calls += "ready"
    }
}

@SlashGroup("config", "Config")
class ConfigCommands {
    @Slash("set", "Set")
    fun set(@Option("key") key: String) {
        calls += "set $key"
    }
}

class DispatcherTest {
    private val storage = MetadataStorage(emptySet()).apply { register(Commands(), ConfigCommands()) }
    private val dispatcher = Dispatcher(storage, { emptyList() }, ::defaultErrorHandler)

    init {
        calls.clear()
    }

    private fun mapping(value: String) = mockk<OptionMapping> {
        every { asString } returns value
        every { asInt } answers { value.toInt() }
    }

    private fun slash(name: String, subcommand: String? = null, options: Map<String, String> = emptyMap()) =
        mockk<SlashCommandInteractionEvent>(relaxed = true) {
            every { this@mockk.name } returns name
            every { subcommandGroup } returns null
            every { subcommandName } returns subcommand
            every { getOption(any<String>()) } answers { options[firstArg()]?.let(::mapping) }
        }

    private fun button(id: String) = mockk<ButtonInteractionEvent>(relaxed = true) { every { componentId } returns id }

    @Test
    fun `routes slash with typed options`() = runTest {
        dispatcher.dispatch(slash("echo", options = mapOf("text" to "hi", "count" to "3")))

        assertEquals(listOf("first", "echo hi 3"), calls)
    }

    @Test
    fun `routes subcommand path`() = runTest {
        dispatcher.dispatch(slash("config", subcommand = "set", options = mapOf("key" to "prefix")))

        assertEquals(listOf("set prefix"), calls)
    }

    @Test
    fun `routes button by regex`() = runTest {
        dispatcher.dispatch(button("vote-3"))

        assertEquals("vote vote-3", calls.last())
    }

    @Test
    fun `ignores kd ids`() = runTest {
        dispatcher.dispatch(button("kd:1:2"))

        assertTrue(calls.none { it.startsWith("any") || it.startsWith("vote") })
    }

    @Test
    fun `guard false skips handler`() = runTest {
        dispatcher.dispatch(slash("denied"))

        assertTrue("denied" !in calls)
    }

    @Test
    fun `class guard runs before function guard`() = runTest {
        dispatcher.dispatch(slash("guarded"))

        assertEquals(listOf("first", "second", "guarded"), calls)
    }

    @Test
    fun `error after acknowledge uses follow-up`() = runTest {
        val followUp = mockk<WebhookMessageCreateAction<Message>>()
        every { followUp.setEphemeral(true) } returns followUp
        every { followUp.submit() } returns CompletableFuture.completedFuture(mockk())
        val event = slash("fail")
        every { event.isAcknowledged } returns true
        every { event.hook.sendMessage(any<String>()) } returns followUp

        dispatcher.dispatch(event)

        verify { event.hook.sendMessage("Something went wrong while running this command.") }
        verify(exactly = 0) { event.reply(any<String>()) }
    }

    @Test
    fun `once handler runs once`() = runTest {
        val ready = mockk<ReadyEvent>()

        dispatcher.dispatch(ready)
        dispatcher.dispatch(ready)

        assertEquals(1, calls.count { it == "ready" })
    }

    @Test
    fun `autocomplete replies with choices`() = runTest {
        val action = mockk<AutoCompleteCallbackAction> { every { submit() } returns CompletableFuture.completedFuture(null) }
        val event = mockk<CommandAutoCompleteInteractionEvent>(relaxed = true) {
            every { name } returns "echo"
            every { subcommandGroup } returns null
            every { subcommandName } returns null
            every { focusedOption.name } returns "color"
            every { replyChoices(any<Collection<Command.Choice>>()) } returns action
        }

        dispatcher.dispatch(event)

        verify { event.replyChoices(listOf(Command.Choice("Red", "red"))) }
    }
}

class SpecificityTest {
    class Buttons {
        @ButtonComponent("ticket-.*")
        fun aLoose(event: ButtonInteractionEvent) {
            calls += "loose"
        }

        @ButtonComponent("ticket-close")
        fun zExact(event: ButtonInteractionEvent) {
            calls += "exact"
        }

        @ButtonComponent("ticket-close-.*")
        fun mLonger(event: ButtonInteractionEvent) {
            calls += "longer"
        }
    }

    private val dispatcher = Dispatcher(MetadataStorage(emptySet()).apply { register(Buttons()) }, { emptyList() }, ::defaultErrorHandler)

    init {
        calls.clear()
    }

    private fun button(id: String) = mockk<ButtonInteractionEvent>(relaxed = true) { every { componentId } returns id }

    @Test
    fun `exact id wins over patterns`() = runTest {
        dispatcher.dispatch(button("ticket-close"))

        assertEquals(listOf("exact"), calls)
    }

    @Test
    fun `longest matching pattern wins`() = runTest {
        dispatcher.dispatch(button("ticket-close-42"))

        assertEquals(listOf("longer"), calls)
    }
}
