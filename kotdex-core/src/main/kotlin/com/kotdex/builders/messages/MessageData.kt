package com.kotdex.builders.messages

import com.kotdex.builders.IBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageData

/**
 * Abstract class representing a builder for creating a message data object.
 *
 * @param T the type of message data that this builder creates
 */
public abstract class MessageData<T : MessageData> : IBuilder<T> {
    public var content: String? = null
    public var embeds: List<MessageEmbed>? = null
    public var files: List<FileUpload>? = null
    public var components: List<MessageTopLevelComponent>? = null
    public var mentions: List<Message.MentionType>? = null
    public var tts: Boolean = false

    /**
     * Builds and returns the message data object created by this builder.
     *
     * @return the message data object created by this builder
     */
    abstract override fun build(): T
}
