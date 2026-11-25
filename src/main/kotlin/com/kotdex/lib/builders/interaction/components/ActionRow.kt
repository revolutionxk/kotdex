package com.kotdex.lib.builders.interaction.components

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent

/**
 * Creates an [ActionRow] containing the provided [components].
 *
 * Example usage:
 *
 * ```kotlin
 * val button1 = primary("button1", "Click me")
 * val button2 = secondary("button2", "No, click me!")
 * val actionRow = row(button1, button2)
 * ```
 *
 * @param components the [ActionRowChildComponent] objects to add to the row.
 * @return the created [ActionRow] object.
 */
fun row(vararg components: ActionRowChildComponent): ActionRow {
    return ActionRow.of(components.asList())
}

/**
 * Creates an [ActionRow] containing the [ActionRowChildComponent] objects in this collection.
 *
 * Example usage:
 *
 * ```kotlin
 * val buttons = listOf(
 *     primary("button1", "Button 1"),
 *     secondary("button2", "Button 2"),
 *     success("button3", "Button 3")
 * ).row()
 * ```
 *
 * @return the created [ActionRow] object.
 */
fun Collection<ActionRowChildComponent>.row(): ActionRow = ActionRow.of(this)
