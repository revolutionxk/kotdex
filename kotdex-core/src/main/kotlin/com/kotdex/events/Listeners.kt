package com.kotdex.events

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener

public inline fun <reified E : GenericEvent> JDA.on(crossinline block: (E) -> Unit): EventListener {
    val listener = EventListener { event -> if (event is E) block(event) }
    addEventListener(listener)
    return listener
}

public inline fun <reified E : GenericEvent> JDA.once(crossinline block: (E) -> Unit): EventListener {
    val listener = object : EventListener {
        override fun onEvent(event: GenericEvent) {
            if (event !is E) return
            removeEventListener(this)
            block(event)
        }
    }
    addEventListener(listener)
    return listener
}
