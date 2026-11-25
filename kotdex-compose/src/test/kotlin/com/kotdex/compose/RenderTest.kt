package com.kotdex.compose

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import com.kotdex.compose.internal.InteractiveNode
import com.kotdex.compose.internal.NodeApplier
import com.kotdex.compose.internal.RootNode
import com.kotdex.compose.internal.renderComponents
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import java.awt.Color
import java.util.IdentityHashMap
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class RenderTest {
    private class Harness(scope: TestScope) {
        private val clock = BroadcastFrameClock()
        val recomposer = Recomposer(scope.coroutineContext + clock)
        val root = RootNode()
        private val ids = IdentityHashMap<InteractiveNode, Int>()
        private val composition = Composition(NodeApplier(root) {}, recomposer)

        init {
            scope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        }

        fun setContent(content: @Composable () -> Unit) = composition.setContent(content)

        fun render(): List<MessageTopLevelComponent> = renderComponents(root) { "kd:test:" + ids.getOrPut(it) { ids.size } }

        fun TestScope.frame() {
            Snapshot.sendApplyNotifications()
            runCurrent()
            clock.sendFrame(0L)
            runCurrent()
        }

        fun close() {
            composition.dispose()
            recomposer.cancel()
        }
    }

    private fun render(content: @Composable () -> Unit): List<MessageTopLevelComponent> {
        var result: List<MessageTopLevelComponent> = emptyList()
        runTest {
            val harness = Harness(this)
            harness.setContent(content)
            result = harness.render()
            harness.close()
        }
        return result
    }

    @Test
    fun `container renders text and buttons`() {
        val container = render {
            Container(accent = Color.RED) {
                Text("Count: 0")
                ActionRow {
                    Button("+1") {}
                    Button("Reset", style = ButtonStyle.DANGER) {}
                }
            }
        }.single() as Container

        assertEquals(Color.RED, container.accentColor)
        assertEquals("Count: 0", (container.components[0] as TextDisplay).content)
        val buttons = (container.components[1] as ActionRow).buttons
        assertEquals(listOf("+1", "Reset"), buttons.map { it.label })
        assertEquals(listOf(ButtonStyle.PRIMARY, ButtonStyle.DANGER), buttons.map { it.style })
        assertEquals(listOf("kd:test:0", "kd:test:1"), buttons.map { it.customId })
    }

    @Test
    fun `button ids are stable across recompositions`() = runTest {
        val harness = Harness(this)
        val count = mutableIntStateOf(0)
        harness.setContent { ActionRow { Button("Count ${count.intValue}") {} } }
        val before = (harness.render().single() as ActionRow).buttons.single()

        count.intValue++
        with(harness) { frame() }
        val after = (harness.render().single() as ActionRow).buttons.single()

        assertEquals("Count 1", after.label)
        assertEquals(before.customId, after.customId)
        harness.close()
    }

    @Test
    fun `section renders accessory`() {
        val section = render {
            Section(accessory = { Button("Go") {} }) { Text("Hello") }
        }.single() as Section

        assertEquals("Go", (section.accessory as Button).label)
        assertEquals("Hello", (section.contentComponents.single() as TextDisplay).content)
    }

    @Test
    fun `select renders options`() {
        val menu = (render {
            ActionRow {
                StringSelect(options = listOf(SelectOption.of("A", "a")), placeholder = "Pick") {}
            }
        }.single() as ActionRow).components.single() as StringSelectMenu

        assertEquals("Pick", menu.placeholder)
        assertEquals(listOf("a"), menu.options.map { it.value })
        assertEquals("kd:test:0", menu.customId)
    }

    @Test
    fun `theme local reaches children`() {
        val container = render {
            KotdexTheme(LocalTheme.current.copy(primary = Color.GREEN)) {
                Container(accent = LocalTheme.current.primary) { Text("Themed") }
            }
        }.single() as Container

        assertEquals(Color.GREEN, container.accentColor)
    }

    @Test
    fun `button outside an action row fails clearly`() {
        val error = assertFailsWith<IllegalStateException> {
            render { Container { Button("Loose") {} } }
        }

        assertContains(error.message!!, "Button cannot be placed inside Container")
    }
}
