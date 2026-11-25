package com.kotdex.internal.options

import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal sealed interface OptionKind {
    val type: OptionType

    fun configure(data: OptionData) {}

    fun read(mapping: OptionMapping): Any?

    fun parse(raw: String, message: Message): Any?
}

internal fun optionKindOf(type: KType): OptionKind? {
    val classifier = type.classifier as? KClass<*> ?: return null
    return when (classifier) {
        String::class -> StringKind
        Int::class -> IntKind
        Long::class -> LongKind
        Double::class -> DoubleKind
        Boolean::class -> BooleanKind
        User::class -> UserKind
        Member::class -> MemberKind
        Role::class -> RoleKind
        IMentionable::class -> MentionableKind
        Message.Attachment::class -> AttachmentKind
        else -> when {
            classifier.java.isEnum -> EnumKind(classifier.java)
            GuildChannel::class.java.isAssignableFrom(classifier.java) -> ChannelKind(classifier.java)
            else -> null
        }
    }
}

private val mention = Regex("""^<(?:@!?|@&|#)(\d+)>$""")

private fun idOf(raw: String): Long? = mention.matchEntire(raw)?.groupValues?.get(1)?.toLongOrNull() ?: raw.toLongOrNull()

private fun Message.guildOrNull() = if (isFromGuild) guild else null

private object StringKind : OptionKind {
    override val type = OptionType.STRING
    override fun read(mapping: OptionMapping) = mapping.asString
    override fun parse(raw: String, message: Message) = raw
}

private object IntKind : OptionKind {
    override val type = OptionType.INTEGER
    override fun configure(data: OptionData) {
        data.setRequiredRange(Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong())
    }
    override fun read(mapping: OptionMapping) = mapping.asInt
    override fun parse(raw: String, message: Message) = raw.toIntOrNull()
}

private object LongKind : OptionKind {
    override val type = OptionType.INTEGER
    override fun read(mapping: OptionMapping) = mapping.asLong
    override fun parse(raw: String, message: Message) = raw.toLongOrNull()
}

private object DoubleKind : OptionKind {
    override val type = OptionType.NUMBER
    override fun read(mapping: OptionMapping) = mapping.asDouble
    override fun parse(raw: String, message: Message) = raw.toDoubleOrNull()
}

private object BooleanKind : OptionKind {
    override val type = OptionType.BOOLEAN
    override fun read(mapping: OptionMapping) = mapping.asBoolean
    override fun parse(raw: String, message: Message) = when (raw.lowercase()) {
        "true", "yes", "on" -> true
        "false", "no", "off" -> false
        else -> null
    }
}

private object UserKind : OptionKind {
    override val type = OptionType.USER
    override fun read(mapping: OptionMapping) = mapping.asUser
    override fun parse(raw: String, message: Message) = idOf(raw)?.let { id ->
        message.mentions.users.firstOrNull { it.idLong == id } ?: message.jda.getUserById(id)
    }
}

private object MemberKind : OptionKind {
    override val type = OptionType.USER
    override fun read(mapping: OptionMapping) = mapping.asMember
    override fun parse(raw: String, message: Message) = idOf(raw)?.let { id ->
        message.mentions.members.firstOrNull { it.idLong == id } ?: message.guildOrNull()?.getMemberById(id)
    }
}

private object RoleKind : OptionKind {
    override val type = OptionType.ROLE
    override fun read(mapping: OptionMapping) = mapping.asRole
    override fun parse(raw: String, message: Message) = idOf(raw)?.let { id ->
        message.mentions.roles.firstOrNull { it.idLong == id } ?: message.guildOrNull()?.getRoleById(id)
    }
}

private object MentionableKind : OptionKind {
    override val type = OptionType.MENTIONABLE
    override fun read(mapping: OptionMapping) = mapping.asMentionable
    override fun parse(raw: String, message: Message): IMentionable? = idOf(raw)?.let { id ->
        UserKind.parse(raw, message) ?: RoleKind.parse(raw, message) ?: message.mentions.channels.firstOrNull { it.idLong == id }
    }
}

private object AttachmentKind : OptionKind {
    override val type = OptionType.ATTACHMENT
    override fun read(mapping: OptionMapping) = mapping.asAttachment
    override fun parse(raw: String, message: Message) = null
}

private class EnumKind(type: Class<*>) : OptionKind {
    private val constants = type.enumConstants.map { it as Enum<*> }

    override val type = OptionType.STRING
    override fun configure(data: OptionData) {
        data.addChoices(constants.map { Command.Choice(it.name, it.name) })
    }
    override fun read(mapping: OptionMapping) = constants.first { it.name == mapping.asString }
    override fun parse(raw: String, message: Message) = constants.firstOrNull { it.name.equals(raw, ignoreCase = true) }
}

private class ChannelKind(private val channel: Class<*>) : OptionKind {
    private val channelTypes = ChannelType.entries.filter { it.isGuild && channel.isAssignableFrom(it.getInterface()) }

    override val type = OptionType.CHANNEL
    override fun configure(data: OptionData) {
        if (channel != GuildChannel::class.java) data.setChannelTypes(channelTypes)
    }
    override fun read(mapping: OptionMapping): Any = channel.cast(mapping.asChannel)
    override fun parse(raw: String, message: Message) = idOf(raw)?.let { id ->
        (message.mentions.channels.firstOrNull { it.idLong == id } ?: message.guildOrNull()?.getGuildChannelById(id))
            ?.takeIf(channel::isInstance)
    }
}
