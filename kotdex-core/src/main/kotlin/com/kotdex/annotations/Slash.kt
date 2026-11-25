package com.kotdex.annotations

import net.dv8tion.jda.api.Permission

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Slash(
    val name: String,
    val description: String,
    val guilds: LongArray = [],
    val permissions: Array<Permission> = [],
    val guildOnly: Boolean = false,
    val nsfw: Boolean = false,
)
