package com.kotdex

import net.dv8tion.jda.api.JDABuilder

class RecordingExtension : ClientExtension {
    override fun install(builder: JDABuilder) {
        builders += builder
    }

    companion object {
        val builders = mutableListOf<JDABuilder>()
    }
}
