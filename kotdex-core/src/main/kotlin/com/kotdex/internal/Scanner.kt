package com.kotdex.internal

import com.kotdex.annotations.Discord
import io.github.classgraph.ClassGraph
import kotlin.reflect.KClass

internal fun scan(packages: Array<out String>, loader: ClassLoader = Thread.currentThread().contextClassLoader): List<KClass<*>> =
    ClassGraph()
        .enableClasspath()
        .enableClassInfo()
        .enableAnnotationInfo()
        .acceptPackages(*packages)
        .scan()
        .use { result -> result.getClassesWithAnnotation(Discord::class.java).names }
        .map { Class.forName(it, true, loader).kotlin }
