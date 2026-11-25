package com.kotdex.compose

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

public interface ComposeView {
    public val id: String

    public suspend fun close()
}

public val LocalView: ProvidableCompositionLocal<ComposeView> =
    staticCompositionLocalOf { error("LocalView is only available inside replyCompose or sendCompose") }
