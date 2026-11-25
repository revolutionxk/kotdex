package com.kotdex.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.kotdex.theme.Theme
import java.awt.Color

public data class ColorScheme(
    val primary: Color,
    val success: Color,
    val error: Color,
    val warning: Color,
    val info: Color,
) {
    public companion object {
        public fun fromTheme(): ColorScheme = ColorScheme(
            primary = Color.decode(Theme.primary),
            success = Color.decode(Theme.success),
            error = Color.decode(Theme.error),
            warning = Color.decode(Theme.warning),
            info = Color.decode(Theme.info),
        )
    }
}

public val LocalTheme: ProvidableCompositionLocal<ColorScheme> = staticCompositionLocalOf { ColorScheme.fromTheme() }

@Composable
public fun KotdexTheme(colors: ColorScheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTheme provides colors, content = content)
}
