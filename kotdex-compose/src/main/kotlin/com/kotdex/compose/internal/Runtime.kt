package com.kotdex.compose.internal

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import com.kotdex.coroutines.await
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal const val VIEW_PREFIX = "kd:"
internal const val EXPIRED = "This message has expired."

internal class ComposeRuntime(val context: CoroutineContext) {
    val scope = CoroutineScope(SupervisorJob() + context + CoroutineName("kotdex-compose"))

    private val frames = Channel<Unit>(Channel.CONFLATED)
    private val clock = BroadcastFrameClock { frames.trySend(Unit) }
    val recomposer = Recomposer(scope.coroutineContext + clock)

    private val views = ConcurrentHashMap<String, LiveView>()
    private val installed = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<JDA, Boolean>()))
    private val applyScheduled = AtomicBoolean()
    private val observer: ObserverHandle

    init {
        scope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        scope.launch { for (frame in frames) clock.sendFrame(System.nanoTime()) }
        observer = Snapshot.registerGlobalWriteObserver {
            if (applyScheduled.compareAndSet(false, true)) {
                scope.launch {
                    applyScheduled.set(false)
                    Snapshot.sendApplyNotifications()
                }
            }
        }
    }

    fun view(timeout: Duration, onlyFor: Long?, minEditInterval: Duration = 1.seconds): LiveView {
        val id = Random.nextLong().toULong().toString(36)
        return LiveView(id, this, timeout, onlyFor, minEditInterval).also { views[id] = it }
    }

    fun remove(view: LiveView) {
        views.remove(view.id)
    }

    fun install(jda: JDA) {
        if (!installed.add(jda)) return
        jda.addEventListener(EventListener { event ->
            if (event is GenericComponentInteractionCreateEvent && event.componentId.startsWith(VIEW_PREFIX)) {
                scope.launch { route(event) }
            }
        })
    }

    suspend fun route(event: GenericComponentInteractionCreateEvent) = withContext(context) {
        val parts = event.componentId.removePrefix(VIEW_PREFIX).split(':')
        val view = parts.getOrNull(0)?.let(views::get)
        val slot = parts.getOrNull(1)?.toIntOrNull()
        if (view == null || slot == null) {
            event.reply(EXPIRED).setEphemeral(true).await()
        } else {
            view.click(slot, event)
        }
    }

    suspend fun awaitRecomposition() {
        Snapshot.sendApplyNotifications()
        recomposer.awaitIdle()
    }

    fun close() {
        observer.dispose()
        recomposer.cancel()
        scope.cancel()
    }

    companion object {
        val default: ComposeRuntime by lazy { ComposeRuntime(Dispatchers.Default.limitedParallelism(1)) }
    }
}
