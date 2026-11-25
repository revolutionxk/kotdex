package com.kotdex

import net.dv8tion.jda.api.JDABuilder

public interface ClientExtension {
    public fun install(builder: JDABuilder)
}
