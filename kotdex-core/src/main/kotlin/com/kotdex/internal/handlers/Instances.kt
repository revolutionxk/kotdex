package com.kotdex.internal.handlers

import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible

internal fun instantiate(type: KClass<*>): Any {
    type.objectInstance?.let { return it }
    val constructor = type.constructors.firstOrNull { constructor -> constructor.parameters.all { it.isOptional } }
        ?: error("${type.qualifiedName} needs a constructor without parameters, or register an instance with client.register(...)")
    constructor.isAccessible = true
    return constructor.callBy(emptyMap())
}
