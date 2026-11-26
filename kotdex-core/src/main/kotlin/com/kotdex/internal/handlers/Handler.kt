package com.kotdex.internal.handlers

import com.kotdex.guards.Guard
import net.dv8tion.jda.api.events.GenericEvent
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter

internal class InvocationContext(val event: GenericEvent, val arguments: Map<String, Any?>, val injectables: List<Any>)

internal class Handler(
    val name: String,
    private val function: KFunction<*>,
    private val instance: Any,
    private val eventType: Class<*>,
    val guards: List<Guard>,
    val parameters: List<ParameterResolver>,
) {
    fun accepts(event: GenericEvent): Boolean = eventType.isInstance(event)

    suspend fun invoke(context: InvocationContext): Any? {
        val arguments = HashMap<KParameter, Any?>()
        function.instanceParameter?.let { arguments[it] = instance }
        for (resolver in parameters) {
            val value = resolver.resolve(context)
            if (value == null && resolver.parameter.isOptional) continue
            arguments[resolver.parameter] = value
        }
        return try {
            function.callSuspendBy(arguments)
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }
}

internal class Compiled<out D>(val descriptor: D, val handler: Handler)
