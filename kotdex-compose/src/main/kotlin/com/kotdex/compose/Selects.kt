package com.kotdex.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import com.kotdex.compose.internal.EntitySelectNode
import com.kotdex.compose.internal.NodeApplier
import com.kotdex.compose.internal.StringSelectNode
import net.dv8tion.jda.api.components.selections.EntitySelectMenu.SelectTarget
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

@Composable
public fun StringSelect(
    options: List<SelectOption>,
    placeholder: String? = null,
    range: IntRange = 1..1,
    disabled: Boolean = false,
    onSelect: suspend (StringSelectInteractionEvent) -> Unit,
) {
    ComposeNode<StringSelectNode, NodeApplier>(
        factory = ::StringSelectNode,
        update = {
            set(options) { this.options = it }
            set(placeholder) { this.placeholder = it }
            set(range) { this.range = it }
            set(disabled) { this.disabled = it }
            set(onSelect) { this.onSelect = it }
        },
    )
}

@Composable
public fun EntitySelect(
    targets: Set<SelectTarget>,
    placeholder: String? = null,
    range: IntRange = 1..1,
    disabled: Boolean = false,
    onSelect: suspend (EntitySelectInteractionEvent) -> Unit,
) {
    ComposeNode<EntitySelectNode, NodeApplier>(
        factory = ::EntitySelectNode,
        update = {
            set(targets) { this.targets = it }
            set(placeholder) { this.placeholder = it }
            set(range) { this.range = it }
            set(disabled) { this.disabled = it }
            set(onSelect) { this.onSelect = it }
        },
    )
}
