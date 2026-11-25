package com.kotdex.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import com.kotdex.compose.internal.ButtonNode
import com.kotdex.compose.internal.LinkButtonNode
import com.kotdex.compose.internal.NodeApplier
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

@Composable
public fun Button(
    label: String,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    emoji: Emoji? = null,
    disabled: Boolean = false,
    onClick: suspend (ButtonInteractionEvent) -> Unit,
) {
    ComposeNode<ButtonNode, NodeApplier>(
        factory = ::ButtonNode,
        update = {
            set(label) { text = it }
            set(style) { this.style = it }
            set(emoji) { this.emoji = it }
            set(disabled) { this.disabled = it }
            set(onClick) { this.onClick = it }
        },
    )
}

@Composable
public fun LinkButton(url: String, label: String, emoji: Emoji? = null) {
    ComposeNode<LinkButtonNode, NodeApplier>(
        factory = ::LinkButtonNode,
        update = {
            set(url) { this.url = it }
            set(label) { text = it }
            set(emoji) { this.emoji = it }
        },
    )
}
