package com.kotdex.compose.internal

import com.kotdex.coroutines.await
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.data.SerializableData
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder
import net.dv8tion.jda.api.utils.messages.MessageEditData

internal fun interface EditTarget {
    suspend fun edit(data: MessageEditData)
}

internal class HookTarget(private val hook: InteractionHook, private val messageId: String = "@original") : EditTarget {
    override suspend fun edit(data: MessageEditData) {
        hook.editMessageById(messageId, data).await()
    }
}

internal class MessageTarget(private val message: Message) : EditTarget {
    override suspend fun edit(data: MessageEditData) {
        message.editMessage(data).await()
    }
}

internal fun createData(components: List<MessageTopLevelComponent>): MessageCreateData =
    MessageCreateBuilder().useComponentsV2(true).setComponents(components).build()

internal fun editData(components: List<MessageTopLevelComponent>): MessageEditData =
    MessageEditBuilder().useComponentsV2(true).setComponents(components).build()

internal fun fingerprint(components: List<MessageTopLevelComponent>): String =
    components.joinToString(",") { (it as SerializableData).toData().toString() }
