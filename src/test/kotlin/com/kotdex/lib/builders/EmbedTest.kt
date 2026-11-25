package com.kotdex.lib.builders

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EmbedTest {
    @Test
    fun `builds without a title`() {
        val embed = Embed { description = "Hello" }

        assertNull(embed.title)
        assertEquals("Hello", embed.description)
    }

    @Test
    fun `builds without a description`() {
        val embed = Embed { title = "Title" }

        assertEquals("Title", embed.title)
        assertNull(embed.description)
    }
}
