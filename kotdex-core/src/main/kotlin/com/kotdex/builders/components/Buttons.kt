package com.kotdex.builders.components

import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.entities.emoji.Emoji

/**
 * Returns a new [Button] instance with the provided properties.
 *
 * @param id the unique identifier of the button
 * @param label the label text of the button
 * @param emoji the emoji icon to display next to the label
 * @param style the visual style of the button
 * @param disabled whether the button is disabled or not
 * @return a new [Button] instance
 */
public fun button(
    id: String,
    label: String?,
    emoji: Emoji? = null,
    style: ButtonStyle = ButtonStyle.SECONDARY,
    disabled: Boolean = false,
): Button {
    val button = when {
        label != null -> Button.of(style, id, label).withEmoji(emoji)
        emoji != null -> Button.of(style, id, emoji)
        else -> throw IllegalArgumentException("A button needs a label or an emoji")
    }

    return button.withDisabled(disabled)
}

/**
 * Returns a new primary [Button] instance with the provided properties.
 *
 * @param id the unique identifier of the button
 * @param label the label text of the button
 * @param emoji the emoji icon to display next to the label
 * @return a new primary [Button] instance
 */
public fun primary(id: String, label: String, emoji: Emoji? = null): Button = button(id, label, emoji, ButtonStyle.PRIMARY)

/**
 * Returns a new secondary [Button] instance with the provided properties.
 *
 * @param id the unique identifier of the button
 * @param label the label text of the button
 * @param emoji the emoji icon to display next to the label
 * @return a new secondary [Button] instance
 */
public fun secondary(id: String, label: String, emoji: Emoji? = null): Button = button(id, label, emoji, ButtonStyle.SECONDARY)

/**
 * Returns a new success [Button] instance with the provided properties.
 *
 * @param id the unique identifier of the button
 * @param label the label text of the button
 * @param emoji the emoji icon to display next to the label
 * @return a new success [Button] instance
 */
public fun success(id: String, label: String, emoji: Emoji? = null): Button = button(id, label, emoji, ButtonStyle.SUCCESS)

/**
 * Returns a new danger [Button] instance with the provided properties.
 *
 * @param id the unique identifier of the button
 * @param label the label text of the button
 * @param emoji the emoji icon to display next to the label
 * @return a new danger [Button] instance
 */
public fun danger(id: String, label: String, emoji: Emoji? = null): Button = button(id, label, emoji, ButtonStyle.DANGER)

/**
 * Returns a new link [Button] instance with the provided properties.
 *
 * @param url the URL to link to when the button is clicked
 * @param label the label text of the button
 * @param emoji the emoji icon to display next to the label
 * @return a new link [Button] instance
 */
public fun link(url: String, label: String, emoji: Emoji? = null): Button = button(url, label, emoji, ButtonStyle.LINK)
