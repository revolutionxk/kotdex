package com.kotdex.annotations

import com.kotdex.autocomplete.Autocompleter
import kotlin.reflect.KClass

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
public annotation class Option(
    val name: String,
    val description: String = "",
    val autocomplete: KClass<out Autocompleter> = Autocompleter::class,
    val min: Double = Double.NaN,
    val max: Double = Double.NaN,
    val minLength: Int = -1,
    val maxLength: Int = -1,
)
