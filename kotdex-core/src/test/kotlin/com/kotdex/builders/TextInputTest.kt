package com.kotdex.builders

import com.kotdex.builders.modal.paragraphtextinput
import com.kotdex.builders.modal.shorttextinput
import kotlin.test.Test
import kotlin.test.assertEquals

class TextInputTest {
    @Test
    fun `short text input applies its block`() {
        val label = shorttextinput("name", "Name") { placeholder = "Your name" }

        assertEquals("Name", label.label)
        assertEquals("Your name", label.child.asTextInput().placeHolder)
    }

    @Test
    fun `paragraph text input applies its block`() {
        val label = paragraphtextinput("bio", "Bio") { placeholder = "About you" }

        assertEquals("About you", label.child.asTextInput().placeHolder)
    }

    @Test
    fun `length is unbounded unless set`() {
        val input = shorttextinput("name", "Name") {}.child.asTextInput()

        assertEquals(-1, input.minLength)
        assertEquals(-1, input.maxLength)
    }

    @Test
    fun `length uses the required range`() {
        val input = shorttextinput("name", "Name") { requiredLength = 2..32 }.child.asTextInput()

        assertEquals(2, input.minLength)
        assertEquals(32, input.maxLength)
    }
}
