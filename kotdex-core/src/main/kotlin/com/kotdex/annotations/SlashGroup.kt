package com.kotdex.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
public annotation class SlashGroup(
    val name: String,
    val description: String,
    val root: String = "",
)
