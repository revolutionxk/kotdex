package com.kotdex.internal.dispatch

internal fun tokenize(input: String): List<String> {
    val tokens = mutableListOf<String>()
    val current = StringBuilder()
    var quoted = false
    var pending = false

    for (char in input) {
        when {
            char == '"' -> {
                quoted = !quoted
                pending = true
            }
            char.isWhitespace() && !quoted -> {
                if (pending) tokens += current.toString()
                current.clear()
                pending = false
            }
            else -> {
                current.append(char)
                pending = true
            }
        }
    }
    if (pending) tokens += current.toString()
    return tokens
}
