package com.kotdex.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import com.kotdex.compose.internal.MediaGalleryNode
import com.kotdex.compose.internal.MediaItemNode
import com.kotdex.compose.internal.NodeApplier
import com.kotdex.compose.internal.ThumbnailNode

@Composable
public fun Thumbnail(url: String, description: String? = null, spoiler: Boolean = false) {
    ComposeNode<ThumbnailNode, NodeApplier>(
        factory = ::ThumbnailNode,
        update = {
            set(url) { this.url = it }
            set(description) { this.description = it }
            set(spoiler) { this.spoiler = it }
        },
    )
}

@Composable
public fun MediaGallery(content: @Composable () -> Unit) {
    ComposeNode<MediaGalleryNode, NodeApplier>(factory = ::MediaGalleryNode, update = {}, content = content)
}

@Composable
public fun MediaItem(url: String, description: String? = null, spoiler: Boolean = false) {
    ComposeNode<MediaItemNode, NodeApplier>(
        factory = ::MediaItemNode,
        update = {
            set(url) { this.url = it }
            set(description) { this.description = it }
            set(spoiler) { this.spoiler = it }
        },
    )
}
