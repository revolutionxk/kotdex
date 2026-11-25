package com.kotdex.lib.builders

import com.kotdex.lib.builders.interaction.components.stringselectmenu
import kotlin.test.Test
import kotlin.test.assertEquals

class StringSelectMenuTest {
    @Test
    fun `keeps defaults set on options`() {
        val menu = stringselectmenu("menu") {
            option("A", "a")
            option("B", "b", default = true)
        }

        assertEquals(listOf(false, true), menu.options.map { it.isDefault })
    }

    @Test
    fun `default value selects the matching option`() {
        val menu = stringselectmenu("menu") {
            option("A", "a")
            option("B", "b")
            defaultValue = "a"
        }

        assertEquals(listOf(true, false), menu.options.map { it.isDefault })
    }
}
