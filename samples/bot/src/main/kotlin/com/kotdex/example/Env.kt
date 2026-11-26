package com.kotdex.example

import java.io.File

private val dotenv: Map<String, String> by lazy {
    val file = File(".env")
    if (!file.isFile) return@lazy emptyMap()
    file.readLines()
        .map(String::trim)
        .filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
        .associate { it.substringBefore('=').trim() to it.substringAfter('=').trim().removeSurrounding("\"") }
}

fun env(name: String): String? = (System.getenv(name) ?: dotenv[name])?.takeIf(String::isNotBlank)
