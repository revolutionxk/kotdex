package com.kotdex.compose.internal

import androidx.compose.runtime.AbstractApplier

internal class NodeApplier(root: RootNode, private val onChanged: () -> Unit) : AbstractApplier<Node>(root) {
    override fun insertTopDown(index: Int, instance: Node) = Unit

    override fun insertBottomUp(index: Int, instance: Node) {
        current.children.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.children.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.children.move(from, to, count)
    }

    override fun onClear() {
        root.children.clear()
    }

    override fun onEndChanges() {
        onChanged()
    }
}
