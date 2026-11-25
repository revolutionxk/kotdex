package com.kotdex.internal.registration

import com.kotdex.annotations.ContextMenu
import com.kotdex.annotations.Option
import com.kotdex.annotations.Slash
import com.kotdex.annotations.SlashGroup
import com.kotdex.autocomplete.Autocompleter
import com.kotdex.metadata.MetadataStorage
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.utils.data.DataObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

object Fruits : Autocompleter {
    override suspend fun complete(event: CommandAutoCompleteInteractionEvent) = listOf(Command.Choice("Apple", "apple"))
}

class Basic {
    @Slash("echo", "Echo text")
    fun echo(@Option("text", "Text") text: String, @Option("fruit", "Fruit", autocomplete = Fruits::class) fruit: String?) {}

    @Slash("ban", "Ban someone", permissions = [Permission.BAN_MEMBERS], guildOnly = true)
    fun ban() {}

    @Slash("local", "Only in one guild", guilds = [42])
    fun local() {}

    @ContextMenu("Inspect", type = Command.Type.USER)
    fun inspect() {}
}

@SlashGroup("config", "Bot settings")
class Settings {
    @Slash("set", "Set a value")
    fun set(@Option("key", "Key") key: String) {}
}

@SlashGroup("prefix", "Prefix settings", root = "config")
class PrefixSettings {
    @Slash("show", "Show the prefix")
    fun show() {}
}

class CommandTreeTest {
    private val storage = MetadataStorage(emptySet())

    private fun global(devGuilds: Set<Long> = emptySet()) = buildCommands(storage, devGuilds)[null].orEmpty().map { it.toData() }

    private fun List<DataObject>.named(name: String) = single { it.getString("name") == name }

    private fun DataObject.options() = getArray("options").let { array -> (0 until array.length()).map(array::getObject) }

    @Test
    fun `plain slash`() {
        storage.register(Basic())

        val echo = global().named("echo")
        assertEquals("Echo text", echo.getString("description"))
        assertEquals(listOf("text", "fruit"), echo.options().map { it.getString("name") })
        assertEquals(listOf(true, false), echo.options().map { it.getBoolean("required") })
    }

    @Test
    fun `autocomplete flag set when an autocompleter is declared`() {
        storage.register(Basic())

        assertEquals(listOf(false, true), global().named("echo").options().map { it.getBoolean("autocomplete") })
    }

    @Test
    fun `group becomes subcommands`() {
        storage.register(Settings())

        val config = global().named("config")
        assertEquals("Bot settings", config.getString("description"))
        val set = config.options().single()
        assertEquals(1, set.getInt("type"))
        assertEquals("set", set.getString("name"))
        assertEquals("key", set.options().single().getString("name"))
    }

    @Test
    fun `root nests subcommand group`() {
        storage.register(Settings(), PrefixSettings())

        val options = global().named("config").options()
        val prefix = options.named("prefix")
        assertEquals(2, prefix.getInt("type"))
        assertEquals("Prefix settings", prefix.getString("description"))
        assertEquals("show", prefix.options().single().getString("name"))
        assertEquals(1, options.named("set").getInt("type"))
    }

    @Test
    fun `permissions and guildOnly set default permissions and contexts`() {
        storage.register(Basic())

        val ban = global().named("ban")
        assertEquals(Permission.BAN_MEMBERS.rawValue.toString(), ban.getString("default_member_permissions"))
        assertEquals(listOf(0), ban.getArray("contexts").let { array -> (0 until array.length()).map(array::getInt) })
        assertTrue(global().named("echo").isNull("default_member_permissions"))
    }

    @Test
    fun `context menus included`() {
        storage.register(Basic())

        assertEquals(2, global().named("Inspect").getInt("type"))
    }

    @Test
    fun `per command guilds split from global`() {
        storage.register(Basic())

        val commands = buildCommands(storage, emptySet())
        assertEquals(listOf("local"), commands[42L].orEmpty().map { it.name })
        assertNull(commands[null].orEmpty().firstOrNull { it.name == "local" })
    }

    @Test
    fun `dev guilds receive everything`() {
        storage.register(Basic(), Settings())

        val commands = buildCommands(storage, setOf(7L))
        assertEquals(setOf<Long?>(7L), commands.keys)
        assertEquals(setOf("echo", "ban", "local", "Inspect", "config"), commands.getValue(7L).map { it.name }.toSet())
    }
}
