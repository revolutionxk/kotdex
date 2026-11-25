package com.kotdex.compose.internal

import net.dv8tion.jda.api.components.MessageTopLevelComponent

internal fun renderComponents(
    root: RootNode,
    disabled: Boolean = false,
    idOf: (InteractiveNode) -> String,
): List<MessageTopLevelComponent> = root.renderChildren(RenderContext(disabled, idOf))

internal fun Node.interactiveNodes(): Sequence<InteractiveNode> = sequence {
    if (this@interactiveNodes is InteractiveNode) yield(this@interactiveNodes)
    children.forEach { yieldAll(it.interactiveNodes()) }
}
