package com.kotdex.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import com.kotdex.compose.internal.AccessoryNode
import com.kotdex.compose.internal.ActionRowNode
import com.kotdex.compose.internal.ContainerNode
import com.kotdex.compose.internal.NodeApplier
import com.kotdex.compose.internal.SectionNode
import com.kotdex.compose.internal.SeparatorNode
import net.dv8tion.jda.api.components.separator.Separator
import java.awt.Color

@Composable
public fun Container(accent: Color? = null, spoiler: Boolean = false, content: @Composable () -> Unit) {
    ComposeNode<ContainerNode, NodeApplier>(
        factory = ::ContainerNode,
        update = {
            set(accent) { this.accent = it }
            set(spoiler) { this.spoiler = it }
        },
        content = content,
    )
}

@Composable
public fun Section(accessory: @Composable () -> Unit, content: @Composable () -> Unit) {
    ComposeNode<SectionNode, NodeApplier>(factory = ::SectionNode, update = {}) {
        ComposeNode<AccessoryNode, NodeApplier>(factory = ::AccessoryNode, update = {}, content = accessory)
        content()
    }
}

@Composable
public fun ActionRow(content: @Composable () -> Unit) {
    ComposeNode<ActionRowNode, NodeApplier>(factory = ::ActionRowNode, update = {}, content = content)
}

@Composable
public fun Separator(divider: Boolean = true, spacing: Separator.Spacing = Separator.Spacing.SMALL) {
    ComposeNode<SeparatorNode, NodeApplier>(
        factory = ::SeparatorNode,
        update = {
            set(divider) { this.divider = it }
            set(spacing) { this.spacing = it }
        },
    )
}
