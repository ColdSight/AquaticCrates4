package gg.aquatic.crates.data.editor

import gg.aquatic.crates.data.PreviewType
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.serialization.editor.meta.EditorFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.FieldEditResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object PreviewSectionFieldAdapter : EditorFieldAdapter {
    override fun createItem(context: EditorFieldContext, defaultItem: () -> ItemStack): ItemStack {
        val currentType = currentType(context)
        return stackedItem(Material.ENDER_EYE) {
            displayName = text("Preview", NamedTextColor.AQUA)
            if (context.description.isNotEmpty()) {
                lore += text("Description", NamedTextColor.DARK_AQUA)
                lore += context.description.map { text(it, NamedTextColor.GRAY) }
            }
            lore += text("Current Type: $currentType", NamedTextColor.WHITE)
            lore += text("Left Click: Edit preview settings", NamedTextColor.GREEN)
            lore += text("Right Click: Change preview type", NamedTextColor.YELLOW)
        }.getItem()
    }

    override suspend fun edit(player: Player, context: EditorFieldContext, buttonType: ButtonType): FieldEditResult {
        return when (buttonType) {
            ButtonType.LEFT -> FieldEditResult.PassThrough
            ButtonType.RIGHT -> {
                val selected = PreviewTypeSelectionMenu.select(player) ?: return FieldEditResult.NoChange
                FieldEditResult.Updated(updatePreviewType(context.value, selected))
            }
            else -> FieldEditResult.NoChange
        }
    }

    private fun currentType(context: EditorFieldContext): String {
        val currentObject = context.value as? JsonObject ?: return PreviewType.AUTOMATIC.id
        return (currentObject["previewType"] as? JsonPrimitive)?.content ?: PreviewType.AUTOMATIC.id
    }

    private fun updatePreviewType(current: JsonElement, type: String): JsonElement {
        val currentObject = current as? JsonObject ?: JsonObject(emptyMap())
        return JsonObject(
            currentObject.toMutableMap().apply {
                put("previewType", JsonPrimitive(type))
            }
        )
    }

    private fun text(content: String, color: NamedTextColor): Component {
        return Component.text(content, color)
            .decoration(TextDecoration.ITALIC, false)
    }
}
