package com.kotdex.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import com.kotdex.compose.ComposeView
import com.kotdex.compose.LocalView
import com.kotdex.coroutines.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.components.MessageTopLevelComponent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import org.slf4j.LoggerFactory
import java.util.IdentityHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private val logger = LoggerFactory.getLogger("com.kotdex.compose.View")
private val ACKNOWLEDGE_DEADLINE = 2500.milliseconds
private const val NOT_YOURS = "This isn't for you."
private const val FAILURE = "Something went wrong."

internal class LiveView(
    override val id: String,
    private val runtime: ComposeRuntime,
    private val timeout: Duration,
    private val onlyFor: Long?,
    private val minEditInterval: Duration,
) : ComposeView {
    private val root = RootNode()
    private val dirty = Channel<Unit>(Channel.CONFLATED)
    private val composition = Composition(NodeApplier(root) { dirty.trySend(Unit) }, runtime.recomposer)
    private val slots = IdentityHashMap<InteractiveNode, Int>()
    private var nextSlot = 0
    private var nodes: Map<Int, InteractiveNode> = emptyMap()
    private val renderLock = Mutex()
    private var target: EditTarget? = null
    private var lastSent: String? = null
    private var finalComponents: List<MessageTopLevelComponent> = emptyList()
    private var loop: Job? = null
    private var expiry: Job? = null
    private var clicks = 0

    @Volatile
    private var closed = false

    suspend fun start(content: @Composable () -> Unit): List<MessageTopLevelComponent> = withContext(runtime.context) {
        composition.setContent { CompositionLocalProvider(LocalView provides this@LiveView, content = content) }
        render().also { lastSent = fingerprint(it) }
    }

    fun attach(target: EditTarget) {
        this.target = target
        loop = runtime.scope.launch {
            for (signal in dirty) {
                if (closed) break
                if (clicks == 0 && push()) delay(minEditInterval)
            }
        }
        touch()
    }

    suspend fun click(slot: Int, event: GenericComponentInteractionCreateEvent) {
        if (onlyFor != null && event.user.idLong != onlyFor) {
            event.reply(NOT_YOURS).setEphemeral(true).await()
            return
        }
        if (closed) {
            event.reply(EXPIRED).setEphemeral(true).await()
            return
        }
        val node = nodes[slot] ?: run {
            event.deferEdit().await()
            return
        }

        touch()
        clicks++
        val acknowledgement = Mutex()
        var settled = false
        val watchdog = runtime.scope.launch {
            delay(ACKNOWLEDGE_DEADLINE)
            acknowledgement.withLock {
                if (!settled && !event.isAcknowledged) {
                    event.deferEdit().await()
                    target = HookTarget(event.hook)
                }
            }
        }
        try {
            node.handle(event)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            report(event, e)
        }

        try {
            if (!closed) runtime.awaitRecomposition()
            acknowledgement.withLock {
                settled = true
                watchdog.cancel()
                finish(event)
            }
        } finally {
            clicks--
        }
    }

    private suspend fun finish(event: GenericComponentInteractionCreateEvent) = renderLock.withLock {
        val components = if (closed) finalComponents else render()
        if (!event.isAcknowledged) {
            event.editMessage(editData(components)).await()
            target = HookTarget(event.hook)
            lastSent = fingerprint(components)
        } else if (fingerprint(components) != lastSent) {
            send(components)
        }
    }

    override suspend fun close(): Unit = withContext(runtime.context + NonCancellable) {
        if (closed) return@withContext
        val components = render(disabled = true)
        closed = true
        finalComponents = components
        loop?.cancel()
        expiry?.cancel()
        composition.dispose()
        runtime.remove(this@LiveView)
        if (clicks == 0 && fingerprint(components) != lastSent) {
            renderLock.withLock { send(components) }
        }
    }

    private suspend fun push(): Boolean = renderLock.withLock {
        if (closed) return@withLock false
        val components = render()
        if (fingerprint(components) == lastSent) return@withLock false
        send(components)
    }

    private suspend fun send(components: List<MessageTopLevelComponent>): Boolean {
        val target = target ?: return false
        return try {
            target.edit(editData(components))
            lastSent = fingerprint(components)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.warn("Stopping view {} because its message could not be edited", id, e)
            runtime.scope.launch { close() }
            false
        }
    }

    private fun render(disabled: Boolean = false): List<MessageTopLevelComponent> {
        val components = renderComponents(root, disabled) { node -> "$VIEW_PREFIX$id:${slots.getOrPut(node) { nextSlot++ }}" }
        val current = root.interactiveNodes().toList()
        slots.keys.retainAll(current.toSet())
        nodes = current.associateBy { slots.getValue(it) }
        return components
    }

    private fun touch() {
        expiry?.cancel()
        expiry = runtime.scope.launch {
            delay(timeout)
            close()
        }
    }

    private suspend fun report(event: GenericComponentInteractionCreateEvent, error: Throwable) {
        logger.error("Component handler failed in view {}", id, error)
        runCatching {
            if (event.isAcknowledged) event.hook.sendMessage(FAILURE).setEphemeral(true).await()
            else event.reply(FAILURE).setEphemeral(true).await()
        }
    }
}
