package com.kotdex.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kotdex.compose.internal.ComposeRuntime
import com.kotdex.compose.internal.EditTarget
import com.kotdex.coroutines.await
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.requests.restaction.WebhookMessageEditAction
import net.dv8tion.jda.api.requests.restaction.interactions.MessageEditCallbackAction
import net.dv8tion.jda.api.requests.restaction.interactions.ModalCallbackAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.api.utils.messages.MessageEditData
import java.util.concurrent.CompletableFuture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class ViewTest {
    private class FakeTarget : EditTarget {
        val edits = mutableListOf<MessageEditData>()

        override suspend fun edit(data: MessageEditData) {
            edits += data
        }
    }

    private fun TestScope.runtime() = ComposeRuntime(StandardTestDispatcher(testScheduler))

    private fun texts(components: List<Any>): List<String> = components.flatMap { component ->
        when (component) {
            is TextDisplay -> listOf(component.content)
            is Container -> texts(component.components)
            else -> emptyList()
        }
    }

    private fun texts(data: MessageEditData) = texts(data.components)

    private fun buttonIds(components: List<MessageTopLevelComponent>): List<String> = components.flatMap { component ->
        when (component) {
            is ActionRow -> component.buttons.map { it.customId!! }
            is Container -> buttonIds(component.components.filterIsInstance<MessageTopLevelComponent>())
            else -> emptyList()
        }
    }

    private fun click(
        id: String,
        user: Long = 1,
        onEdit: (MessageEditData) -> Unit = {},
        acknowledgement: CompletableFuture<InteractionHook> = CompletableFuture.completedFuture(mockk()),
        hookEdits: MutableList<MessageEditData> = mutableListOf(),
    ): ButtonInteractionEvent {
        var acknowledged = false
        val event = mockk<ButtonInteractionEvent>(relaxed = true)
        every { event.componentId } returns id
        every { event.user.idLong } returns user
        every { event.isAcknowledged } answers { acknowledged }
        val edit = mockk<MessageEditCallbackAction>()
        every { edit.submit() } answers {
            acknowledged = true
            acknowledgement
        }
        val hookEdit = mockk<WebhookMessageEditAction<Message>>()
        every { hookEdit.submit() } returns CompletableFuture.completedFuture(mockk())
        every { event.hook.editMessageById(any<String>(), any<MessageEditData>()) } answers {
            hookEdits += secondArg<MessageEditData>()
            hookEdit
        }
        every { event.editMessage(any<MessageEditData>()) } answers {
            onEdit(firstArg())
            edit
        }
        every { event.deferEdit() } returns edit
        val reply = mockk<ReplyCallbackAction>()
        every { reply.setEphemeral(any()) } returns reply
        every { reply.submit() } answers {
            acknowledged = true
            CompletableFuture.completedFuture(mockk())
        }
        every { event.reply(any<String>()) } returns reply
        val modal = mockk<ModalCallbackAction>()
        every { modal.submit() } answers {
            acknowledged = true
            CompletableFuture.completedFuture(null)
        }
        every { event.replyModal(any()) } returns modal
        return event
    }

    @Composable
    private fun Counter() {
        var count by remember { mutableIntStateOf(0) }
        Container {
            Text("Count: $count")
            ActionRow { Button("+1") { count++ } }
        }
    }

    @Test
    fun `click updates state and acknowledges with edit`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        val initial = view.start { Counter() }
        val target = FakeTarget()
        view.attach(target)
        runCurrent()
        val acknowledged = mutableListOf<MessageEditData>()

        runtime.route(click(buttonIds(initial).single(), onEdit = acknowledged::add))
        runCurrent()

        assertEquals(listOf("Count: 0"), texts(initial))
        assertEquals(listOf("Count: 1"), texts(acknowledged.single()))
        assertTrue(target.edits.isEmpty())
        runtime.close()
    }

    @Test
    fun `lambda that opens a modal is not double acknowledged`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        val initial = view.start { ActionRow { Button("Rename") { it.replyModal(mockk<Modal>()).await() } } }
        view.attach(FakeTarget())
        val event = click(buttonIds(initial).single())

        runtime.route(event)
        runCurrent()

        verify(exactly = 1) { event.replyModal(any()) }
        verify(exactly = 0) { event.editMessage(any<MessageEditData>()) }
        verify(exactly = 0) { event.deferEdit() }
        runtime.close()
    }

    @Test
    fun `LaunchedEffect ticker edits are coalesced to the interval and the last state lands`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        view.start {
            var ticks by remember { mutableIntStateOf(0) }
            LaunchedEffect(Unit) {
                repeat(10) {
                    delay(100)
                    ticks++
                }
            }
            Text("Ticks $ticks")
        }
        val target = FakeTarget()
        view.attach(target)

        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(listOf("Ticks 10"), texts(target.edits.last()))
        assertTrue(target.edits.size <= 2, "expected at most 2 edits, got ${target.edits.size}")
        runtime.close()
    }

    @Test
    fun `identical render is not re-sent`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        val value = mutableIntStateOf(0)
        view.start { Text(if (value.intValue > 0) "positive" else "zero") }
        val target = FakeTarget()
        view.attach(target)
        runCurrent()

        value.intValue = 1
        advanceTimeBy(2_000)
        value.intValue = 2
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(listOf("positive"), target.edits.map { texts(it).single() })
        runtime.close()
    }

    @Test
    fun `expiry disposes and disables components`() = runTest {
        val runtime = runtime()
        val view = runtime.view(1.minutes, null)
        var disposed = false
        val initial = view.start {
            DisposableEffect(Unit) { onDispose { disposed = true } }
            ActionRow { Button("Go") {} }
        }
        val target = FakeTarget()
        view.attach(target)

        advanceTimeBy(61_000)
        runCurrent()

        assertTrue(disposed)
        assertTrue((target.edits.last().components.single() as ActionRow).buttons.single().isDisabled)
        val late = click(buttonIds(initial).single())
        runtime.route(late)
        verify { late.reply("This message has expired.") }
        runtime.close()
    }

    @Test
    fun `unknown view click answers expired`() = runTest {
        val runtime = runtime()
        val event = click("kd:missing:0")

        runtime.route(event)

        verify { event.reply("This message has expired.") }
        runtime.close()
    }

    @Test
    fun `onlyFor rejects other users`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, onlyFor = 1L)
        val initial = view.start { Counter() }
        val target = FakeTarget()
        view.attach(target)
        val stranger = click(buttonIds(initial).single(), user = 2)

        runtime.route(stranger)
        advanceTimeBy(2_000)
        runCurrent()

        verify { stranger.reply("This isn't for you.") }
        verify(exactly = 0) { stranger.editMessage(any<MessageEditData>()) }
        assertTrue(target.edits.isEmpty())
        runtime.close()
    }

    @Test
    fun `cancellation thrown from a click does not freeze the view`() = runTest {
        val runtime = runtime()
        val view = runtime.view(1.minutes, null)
        val value = mutableIntStateOf(0)
        val initial = view.start {
            Text("Ticks ${value.intValue}")
            ActionRow { Button("Wait") { withTimeout(1_000) { awaitCancellation() } } }
        }
        val target = FakeTarget()
        view.attach(target)
        runCurrent()
        val hookEdits = mutableListOf<MessageEditData>()

        runtime.route(click(buttonIds(initial).single(), hookEdits = hookEdits))
        value.intValue = 5
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue((target.edits + hookEdits).any { texts(it) == listOf("Ticks 5") })
        advanceTimeBy(61_000)
        runCurrent()
        assertTrue(((target.edits + hookEdits).last().components.last() as ActionRow).buttons.single().isDisabled)
        runtime.close()
    }

    @Test
    fun `state changed while a click is being acknowledged still lands`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        val value = mutableIntStateOf(0)
        val initial = view.start {
            Text("v ${value.intValue}")
            ActionRow { Button("Go") {} }
        }
        val target = FakeTarget()
        view.attach(target)
        runCurrent()
        val pending = CompletableFuture<InteractionHook>()
        val hookEdits = mutableListOf<MessageEditData>()

        val click = launch { runtime.route(click(buttonIds(initial).single(), acknowledgement = pending, hookEdits = hookEdits)) }
        runCurrent()
        value.intValue = 1
        advanceTimeBy(100)
        pending.complete(mockk())
        advanceTimeBy(5_000)
        runCurrent()

        assertTrue((target.edits + hookEdits).any { texts(it) == listOf("v 1") })
        click.join()
        runtime.close()
    }

    @Test
    fun `live updates continue while a clicked lambda is still running`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        val value = mutableIntStateOf(0)
        val initial = view.start {
            Text("v ${value.intValue}")
            ActionRow {
                Button("Ask") {
                    it.reply("asked").await()
                    delay(5.minutes)
                }
            }
        }
        val target = FakeTarget()
        view.attach(target)
        runCurrent()

        val click = launch { runtime.route(click(buttonIds(initial).single())) }
        runCurrent()
        value.intValue = 1
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue(target.edits.any { texts(it) == listOf("v 1") })
        click.cancel()
        runtime.close()
    }

    @Test
    fun `closing after a failed first render disposes the composition`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        var disposed = false
        var ticks = 0

        assertFailsWith<IllegalStateException> {
            view.start {
                LaunchedEffect(Unit) {
                    repeat(50) {
                        delay(100)
                        ticks++
                    }
                }
                DisposableEffect(Unit) { onDispose { disposed = true } }
                Container { Button("Loose") {} }
            }
        }
        view.close()
        advanceTimeBy(1_000)
        runCurrent()

        assertTrue(disposed)
        assertEquals(0, ticks)
        runtime.close()
    }

    @Test
    fun `render failure after a state change closes the view`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        val count = mutableIntStateOf(5)
        var disposed = false
        view.start {
            DisposableEffect(Unit) { onDispose { disposed = true } }
            ActionRow { repeat(count.intValue) { Button("$it") {} } }
        }
        view.attach(FakeTarget())
        runCurrent()

        count.intValue = 6
        advanceTimeBy(2_000)
        runCurrent()

        assertTrue(disposed)
        runtime.close()
    }

    @Test
    fun `click whose result cannot render is still acknowledged`() = runTest {
        val runtime = runtime()
        val view = runtime.view(14.minutes, null)
        var disposed = false
        val initial = view.start {
            var count by remember { mutableIntStateOf(5) }
            DisposableEffect(Unit) { onDispose { disposed = true } }
            ActionRow { repeat(count) { Button("$it") { count++ } } }
        }
        view.attach(FakeTarget())
        runCurrent()
        val event = click(buttonIds(initial).first())

        runtime.route(event)
        runCurrent()

        verify { event.deferEdit() }
        assertTrue(disposed)
        runtime.close()
    }
}
