package com.kotdex.internal.options

import io.mockk.every
import io.mockk.mockk
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import kotlin.reflect.typeOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class OptionKindTest {
    private enum class Unit { SECONDS, MINUTES }

    private fun kind(type: kotlin.reflect.KType) = requireNotNull(optionKindOf(type))

    private fun configured(type: kotlin.reflect.KType): OptionData {
        val kind = kind(type)
        return OptionData(kind.type, "value", "Value").also(kind::configure)
    }

    @Test
    fun `String maps to STRING`() {
        assertEquals(OptionType.STRING, kind(typeOf<String>()).type)
    }

    @Test
    fun `Int maps to INTEGER with int bounds`() {
        val data = configured(typeOf<Int>())

        assertEquals(OptionType.INTEGER, data.type)
        assertEquals(Int.MIN_VALUE.toLong(), data.minValue)
        assertEquals(Int.MAX_VALUE.toLong(), data.maxValue)
    }

    @Test
    fun `enum maps to STRING choices`() {
        val data = configured(typeOf<Unit>())

        assertEquals(OptionType.STRING, data.type)
        assertEquals(listOf("SECONDS", "MINUTES"), data.choices.map { it.asString })
    }

    @Test
    fun `enum reads the selected constant`() {
        val mapping = mockk<OptionMapping> { every { asString } returns "MINUTES" }

        assertEquals(Unit.MINUTES, kind(typeOf<Unit>()).read(mapping))
    }

    @Test
    fun `TextChannel maps to CHANNEL with TEXT type`() {
        val data = configured(typeOf<TextChannel>())

        assertEquals(OptionType.CHANNEL, data.type)
        assertEquals(setOf(ChannelType.TEXT), data.channelTypes)
    }

    @Test
    fun `parse reads user mentions`() {
        val user = mockk<User> { every { idLong } returns 123L }
        val message = mockk<Message> { every { mentions.users } returns listOf(user) }

        assertSame(user, kind(typeOf<User>()).parse("<@123>", message))
    }

    @Test
    fun `parse rejects garbage for Int`() {
        assertNull(kind(typeOf<Int>()).parse("twelve", mockk()))
    }

    @Test
    fun `parse matches enum constants ignoring case`() {
        assertEquals(Unit.SECONDS, kind(typeOf<Unit>()).parse("seconds", mockk()))
    }

    @Test
    fun `unsupported type returns null`() {
        assertNull(optionKindOf(typeOf<Thread>()))
    }
}
