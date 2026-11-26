package com.kotdex.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kotdex.annotations.Discord
import com.kotdex.annotations.Option
import com.kotdex.annotations.Slash
import com.kotdex.compose.ActionRow
import com.kotdex.compose.Button
import com.kotdex.compose.Container
import com.kotdex.compose.LocalTheme
import com.kotdex.compose.LocalView
import com.kotdex.compose.Text
import com.kotdex.compose.replyCompose
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import kotlin.time.Duration.Companion.seconds

@Discord
class Live {
    @Slash("counter", "A counter only you can press")
    suspend fun counter(event: SlashCommandInteractionEvent) {
        event.replyCompose(onlyFor = event.user) { Counter() }
    }

    @Slash("countdown", "Counts down in place")
    suspend fun countdown(event: SlashCommandInteractionEvent, @Option("seconds", "How long", min = 1.0, max = 60.0) seconds: Int = 10) {
        event.replyCompose { Countdown(seconds) }
    }
}

@Composable
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    val view = LocalView.current

    Container(accent = LocalTheme.current.primary) {
        Text("## Counter\nCurrent value: **$count**")
        ActionRow {
            Button("-1", style = ButtonStyle.SECONDARY) { count-- }
            Button("+1") { count++ }
            Button("Done", style = ButtonStyle.DANGER) { view.close() }
        }
    }
}

@Composable
fun Countdown(seconds: Int) {
    var left by remember { mutableIntStateOf(seconds) }

    LaunchedEffect(Unit) {
        while (left > 0) {
            delay(1.seconds)
            left--
        }
    }

    val colors = LocalTheme.current
    Container(accent = if (left == 0) colors.success else colors.warning) {
        Text(if (left == 0) "Time's up!" else "**$left** seconds left")
    }
}
