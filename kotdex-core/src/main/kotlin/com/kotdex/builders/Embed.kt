package com.kotdex.builders

import com.kotdex.theme.Theme
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color
import java.time.temporal.TemporalAccessor

@DslMarker
public annotation class EmbedDslMarker

@EmbedDslMarker
public data class EmbedAuthorInfo(
    var name: String? = null,
    var url: String? = null,
    var iconUrl: String? = null
)

@EmbedDslMarker
public data class EmbedFieldInfo(
    var name: String,
    var value: String,
    var inline: Boolean = false
)

@EmbedDslMarker
public class EmbedBuilder(
    public var title: String? = null,
    public var description: String? = null,
    public var color: Color = Color.decode(Theme.success),
    public var thumbnailUrl: String? = null,
    public var imageUrl: String? = null,
    public var timestamp: TemporalAccessor? = null
) : IBuilder<MessageEmbed> {
    private val embedBuilder = EmbedBuilder()

    public fun author(block: EmbedAuthorInfo.() -> Unit) {
        val authorInfo = EmbedAuthorInfo().apply(block)

        embedBuilder.setAuthor(authorInfo.name, authorInfo.url, authorInfo.iconUrl)
    }

    public fun field(block: EmbedFieldInfo.() -> Unit) {
        val fieldInfo = EmbedFieldInfo("", "").apply(block)

        embedBuilder.addField(fieldInfo.name, fieldInfo.value, fieldInfo.inline)
    }

    public fun footer(text: String, iconUrl: String? = null) {
        embedBuilder.setFooter(text, iconUrl)
    }

    override fun build(): MessageEmbed {
        return embedBuilder.run {
            setTitle(title)
            setDescription(description)
            setColor(color)
            setThumbnail(thumbnailUrl)
            setImage(imageUrl)
            setTimestamp(timestamp)
        }.build()
    }
}

public fun Embed(block: com.kotdex.builders.EmbedBuilder.() -> Unit): MessageEmbed {
    return com.kotdex.builders.EmbedBuilder().apply(block).build()
}
