package com.kotdex.internal.handlers

import com.kotdex.metadata.OptionDescriptor
import kotlin.reflect.KParameter

internal sealed interface ParameterResolver {
    val parameter: KParameter

    fun resolve(context: InvocationContext): Any?
}

internal class OptionParameter(
    override val parameter: KParameter,
    val descriptor: OptionDescriptor,
) : ParameterResolver {
    override fun resolve(context: InvocationContext): Any? = context.arguments[descriptor.name]
}

internal class InjectedParameter(
    override val parameter: KParameter,
    private val type: Class<*>,
) : ParameterResolver {
    override fun resolve(context: InvocationContext): Any? =
        context.event.takeIf(type::isInstance) ?: context.injectables.firstOrNull(type::isInstance)
}
