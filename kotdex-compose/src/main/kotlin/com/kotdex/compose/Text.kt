package com.kotdex.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import com.kotdex.compose.internal.NodeApplier
import com.kotdex.compose.internal.TextNode

@Composable
public fun Text(content: String) {
    ComposeNode<TextNode, NodeApplier>(factory = ::TextNode, update = { set(content) { this.content = it } })
}
