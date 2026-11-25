package com.kotdex.compose.internal

import com.kotdex.ClientExtension
import net.dv8tion.jda.api.JDABuilder

internal class ComposeExtension(private val runtime: ComposeRuntime = ComposeRuntime.default) : ClientExtension {
    override fun install(builder: JDABuilder) {
        builder.addEventListeners(runtime.listener)
    }
}
