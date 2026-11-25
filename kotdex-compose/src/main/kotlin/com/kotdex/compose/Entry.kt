package com.kotdex.compose

import androidx.compose.runtime.Composable
import com.kotdex.compose.internal.ComposeRuntime
import com.kotdex.compose.internal.HookTarget
import com.kotdex.compose.internal.MessageTarget
import com.kotdex.compose.internal.createData
import com.kotdex.coroutines.await
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public suspend fun IReplyCallback.replyCompose(
    ephemeral: Boolean = false,
    timeout: Duration = 14.minutes,
    onlyFor: UserSnowflake? = null,
    content: @Composable () -> Unit,
): ComposeView {
    val runtime = ComposeRuntime.default
    runtime.install(jda)
    val view = runtime.view(timeout, onlyFor?.idLong)
    try {
        val data = createData(view.start(content))
        val target = if (isAcknowledged) {
            HookTarget(hook, hook.sendMessage(data).setEphemeral(ephemeral).await().id)
        } else {
            HookTarget(reply(data).setEphemeral(ephemeral).await())
        }
        view.attach(target)
    } catch (e: Throwable) {
        view.close()
        throw e
    }
    return view
}

public suspend fun MessageChannel.sendCompose(
    timeout: Duration = 14.minutes,
    onlyFor: UserSnowflake? = null,
    content: @Composable () -> Unit,
): ComposeView {
    val runtime = ComposeRuntime.default
    runtime.install(jda)
    val view = runtime.view(timeout, onlyFor?.idLong)
    try {
        view.attach(MessageTarget(sendMessage(createData(view.start(content))).await()))
    } catch (e: Throwable) {
        view.close()
        throw e
    }
    return view
}
