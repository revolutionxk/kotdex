package com.kotdex.compose.internal

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.section.SectionAccessoryComponent
import net.dv8tion.jda.api.components.section.SectionContentComponent
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import java.awt.Color

internal class RenderContext(val disabled: Boolean, val idOf: (InteractiveNode) -> String)

internal abstract class Node {
    val children: MutableList<Node> = mutableListOf()

    abstract val label: String

    abstract fun render(context: RenderContext): Any
}

internal abstract class InteractiveNode : Node() {
    abstract suspend fun handle(event: GenericComponentInteractionCreateEvent)
}

internal inline fun <reified T> Node.renderChildren(context: RenderContext): List<T> =
    children.map { child -> child.render(context) as? T ?: error("${child.label} cannot be placed inside $label") }

internal class RootNode : Node() {
    override val label = "a message"
    override fun render(context: RenderContext): Any = error("The root is rendered with renderComponents")
}

internal class ContainerNode : Node() {
    var accent: Color? = null
    var spoiler = false

    override val label = "Container"
    override fun render(context: RenderContext): Container =
        Container.of(renderChildren<ContainerChildComponent>(context)).withAccentColor(accent).withSpoiler(spoiler)
}

internal class TextNode : Node() {
    var content = ""

    override val label = "Text"
    override fun render(context: RenderContext): TextDisplay = TextDisplay.of(content)
}

internal class SectionNode : Node() {
    override val label = "Section"
    override fun render(context: RenderContext): Section {
        val accessory = children.filterIsInstance<AccessoryNode>().singleOrNull()
            ?: error("Section needs exactly one accessory")
        val content = children.filter { it !is AccessoryNode }.map { child ->
            child.render(context) as? SectionContentComponent ?: error("${child.label} cannot be placed inside Section")
        }
        return Section.of(accessory.render(context), content)
    }
}

internal class AccessoryNode : Node() {
    override val label = "Section accessory"
    override fun render(context: RenderContext): SectionAccessoryComponent =
        renderChildren<SectionAccessoryComponent>(context).singleOrNull() ?: error("A section accessory holds exactly one Button or Thumbnail")
}

internal class ThumbnailNode : Node() {
    var url = ""
    var description: String? = null
    var spoiler = false

    override val label = "Thumbnail"
    override fun render(context: RenderContext): Thumbnail =
        Thumbnail.fromUrl(url).withDescription(description).withSpoiler(spoiler)
}

internal class SeparatorNode : Node() {
    var divider = true
    var spacing = Separator.Spacing.SMALL

    override val label = "Separator"
    override fun render(context: RenderContext): Separator = Separator.create(divider, spacing)
}

internal class MediaGalleryNode : Node() {
    override val label = "MediaGallery"
    override fun render(context: RenderContext): MediaGallery = MediaGallery.of(renderChildren<MediaGalleryItem>(context))
}

internal class MediaItemNode : Node() {
    var url = ""
    var description: String? = null
    var spoiler = false

    override val label = "MediaItem"
    override fun render(context: RenderContext): MediaGalleryItem =
        MediaGalleryItem.fromUrl(url).withDescription(description).withSpoiler(spoiler)
}

internal class ActionRowNode : Node() {
    override val label = "ActionRow"
    override fun render(context: RenderContext): ActionRow = ActionRow.of(renderChildren<ActionRowChildComponent>(context))
}

internal class ButtonNode : InteractiveNode() {
    var text = ""
    var style = ButtonStyle.PRIMARY
    var emoji: Emoji? = null
    var disabled = false
    var onClick: suspend (ButtonInteractionEvent) -> Unit = {}

    override val label = "Button"
    override fun render(context: RenderContext): Button =
        Button.of(style, context.idOf(this), text).withEmoji(emoji).withDisabled(disabled || context.disabled)

    override suspend fun handle(event: GenericComponentInteractionCreateEvent) {
        if (event is ButtonInteractionEvent) onClick(event)
    }
}

internal class LinkButtonNode : Node() {
    var url = ""
    var text = ""
    var emoji: Emoji? = null

    override val label = "LinkButton"
    override fun render(context: RenderContext): Button = Button.link(url, text).withEmoji(emoji)
}

internal class StringSelectNode : InteractiveNode() {
    var options: List<SelectOption> = emptyList()
    var placeholder: String? = null
    var range = 1..1
    var disabled = false
    var onSelect: suspend (StringSelectInteractionEvent) -> Unit = {}

    override val label = "StringSelect"
    override fun render(context: RenderContext): StringSelectMenu =
        StringSelectMenu.create(context.idOf(this))
            .addOptions(options)
            .setPlaceholder(placeholder)
            .setRequiredRange(range.first, range.last)
            .setDisabled(disabled || context.disabled)
            .build()

    override suspend fun handle(event: GenericComponentInteractionCreateEvent) {
        if (event is StringSelectInteractionEvent) onSelect(event)
    }
}

internal class EntitySelectNode : InteractiveNode() {
    var targets: Set<EntitySelectMenu.SelectTarget> = setOf(EntitySelectMenu.SelectTarget.USER)
    var placeholder: String? = null
    var range = 1..1
    var disabled = false
    var onSelect: suspend (EntitySelectInteractionEvent) -> Unit = {}

    override val label = "EntitySelect"
    override fun render(context: RenderContext): EntitySelectMenu =
        EntitySelectMenu.create(context.idOf(this), targets)
            .setPlaceholder(placeholder)
            .setRequiredRange(range.first, range.last)
            .setDisabled(disabled || context.disabled)
            .build()

    override suspend fun handle(event: GenericComponentInteractionCreateEvent) {
        if (event is EntitySelectInteractionEvent) onSelect(event)
    }
}
