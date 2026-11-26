package com.kotdex.coroutines

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback
import net.dv8tion.jda.api.modals.Modal
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public suspend inline fun <reified E : GenericEvent> JDA.await(
    timeout: Duration = 1.minutes,
    crossinline filter: (E) -> Boolean = { true },
): E = awaitEvent(E::class.java, timeout) { filter(it) }

@PublishedApi
internal suspend fun <E : GenericEvent> JDA.awaitEvent(type: Class<E>, timeout: Duration, filter: (E) -> Boolean): E =
    withTimeout(timeout) {
        suspendCancellableCoroutine { continuation ->
            val done = AtomicBoolean()
            val listener = object : EventListener {
                override fun onEvent(event: GenericEvent) {
                    if (!type.isInstance(event)) return
                    val typed = type.cast(event)
                    if (!filter(typed) || !done.compareAndSet(false, true)) return
                    removeEventListener(this)
                    continuation.resume(typed)
                }
            }
            addEventListener(listener)
            continuation.invokeOnCancellation { removeEventListener(listener) }
        }
    }

public suspend fun IModalCallback.awaitModal(modal: Modal, timeout: Duration = 5.minutes): ModalInteractionEvent = coroutineScope {
    val submission = async(start = CoroutineStart.UNDISPATCHED) {
        jda.await<ModalInteractionEvent>(timeout) { it.modalId == modal.id && it.user.idLong == user.idLong }
    }
    replyModal(modal).await()
    submission.await()
}
