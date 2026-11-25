package com.kotdex.annotations

import com.kotdex.guards.Guard
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Guarded(vararg val value: KClass<out Guard>)
