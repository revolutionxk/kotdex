package com.kotdex.metadata

import com.kotdex.internal.handlers.Compiled
import com.kotdex.internal.handlers.CompiledClass
import com.kotdex.internal.handlers.HandlerCompiler
import kotlin.reflect.KClass

public class MetadataStorage internal constructor(injectables: Set<KClass<*>>) {
    private val compiler = HandlerCompiler(injectables)

    internal var slash: List<Compiled<SlashDescriptor>> = emptyList()
        private set
    internal var groups: List<SlashGroupDescriptor> = emptyList()
        private set
    internal var contextMenus: List<Compiled<ContextMenuDescriptor>> = emptyList()
        private set
    internal var components: List<Compiled<ComponentDescriptor>> = emptyList()
        private set
    internal var simple: List<Compiled<SimpleCommandDescriptor>> = emptyList()
        private set
    internal var events: List<Compiled<EventDescriptor>> = emptyList()
        private set

    public val slashCommands: List<SlashDescriptor> get() = slash.map { it.descriptor }
    public val slashGroups: List<SlashGroupDescriptor> get() = groups
    public val contextMenuCommands: List<ContextMenuDescriptor> get() = contextMenus.map { it.descriptor }
    public val componentHandlers: List<ComponentDescriptor> get() = components.map { it.descriptor }
    public val simpleCommands: List<SimpleCommandDescriptor> get() = simple.map { it.descriptor }
    public val eventListeners: List<EventDescriptor> get() = events.map { it.descriptor }

    @Synchronized
    internal fun register(vararg instances: Any) {
        val compiled = instances.map(compiler::compile)
        var slash = slash
        var groups = groups
        var contextMenus = contextMenus
        var components = components
        var simple = simple

        for (type in compiled) {
            type.slash.forEach { slash = slash.withSlash(it) }
            groups = groups + type.groups.filter { group -> groups.none { it.name == group.name && it.root == group.root } }
            type.contextMenus.forEach { contextMenus = contextMenus.withContextMenu(it) }
            type.components.forEach { components = components.withComponent(it) }
            type.simpleCommands.forEach { simple = simple.withSimple(it) }
        }

        this.slash = slash
        this.groups = groups
        this.contextMenus = contextMenus
        this.components = components
        this.simple = simple
        this.events = events + compiled.flatMap(CompiledClass::events)
    }

    private fun List<Compiled<SlashDescriptor>>.withSlash(entry: Compiled<SlashDescriptor>): List<Compiled<SlashDescriptor>> {
        val path = entry.descriptor.path
        firstOrNull { it.descriptor.path == path }?.let {
            error("Slash command /${entry.descriptor.name} is declared by ${it.handler.name} and ${entry.handler.name}")
        }
        firstOrNull { it.descriptor.path.isPrefixOf(path) || path.isPrefixOf(it.descriptor.path) }?.let {
            error(
                "Slash command /${it.descriptor.name} (${it.handler.name}) conflicts with /${entry.descriptor.name} (${entry.handler.name})",
            )
        }
        return this + entry
    }

    private fun List<Compiled<ContextMenuDescriptor>>.withContextMenu(
        entry: Compiled<ContextMenuDescriptor>,
    ): List<Compiled<ContextMenuDescriptor>> {
        firstOrNull { it.descriptor.name == entry.descriptor.name && it.descriptor.type == entry.descriptor.type }?.let {
            error("Context menu ${entry.descriptor.name} is declared by ${it.handler.name} and ${entry.handler.name}")
        }
        return this + entry
    }

    private fun List<Compiled<ComponentDescriptor>>.withComponent(
        entry: Compiled<ComponentDescriptor>,
    ): List<Compiled<ComponentDescriptor>> {
        firstOrNull {
            it.descriptor.kind == entry.descriptor.kind && it.descriptor.pattern.pattern == entry.descriptor.pattern.pattern
        }?.let {
            error("Component id ${entry.descriptor.pattern} is handled by ${it.handler.name} and ${entry.handler.name}")
        }
        return this + entry
    }

    private fun List<Compiled<SimpleCommandDescriptor>>.withSimple(
        entry: Compiled<SimpleCommandDescriptor>,
    ): List<Compiled<SimpleCommandDescriptor>> {
        val names = entry.descriptor.keys()
        firstOrNull { existing -> existing.descriptor.keys().any(names::contains) }?.let {
            error(
                "Simple command ${entry.descriptor.name} clashes with ${it.descriptor.name}: ${it.handler.name} and ${entry.handler.name}",
            )
        }
        return this + entry
    }

    private fun SimpleCommandDescriptor.keys() = (aliases + name).map(String::lowercase).toSet()

    private fun List<String>.isPrefixOf(other: List<String>) = size < other.size && other.subList(0, size) == this
}
