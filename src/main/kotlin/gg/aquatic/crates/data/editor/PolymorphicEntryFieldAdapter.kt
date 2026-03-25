package gg.aquatic.crates.data.editor

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.serialization.editor.meta.EditorFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.FieldEditResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

open class PolymorphicEntryFieldAdapter(
    private val sectionName: String,
    private val iconResolver: (String?) -> Material?,
    private val nameResolver: (String?) -> String?,
    private val selectType: suspend (Player) -> String?,
    private val createElement: (String) -> JsonElement?,
    private val currentTypeResolver: (EditorFieldContext) -> String?,
) : EditorFieldAdapter {

    override fun createItem(context: EditorFieldContext, defaultItem: () -> ItemStack): ItemStack {
        val currentType = currentTypeResolver(context)
        val displayType = nameResolver(currentType) ?: currentType ?: "Unknown"
        val icon = iconResolver(currentType) ?: Material.BOOK

        return stackedItem(icon) {
            displayName = text(context.label, NamedTextColor.AQUA)
            if (context.description.isNotEmpty()) {
                lore += text("Description", NamedTextColor.DARK_AQUA)
                lore += context.description.map { text(it, NamedTextColor.GRAY) }
            }
            lore += text("Current Type: $displayType", NamedTextColor.WHITE)
            lore += text("Left Click: Edit ${sectionName.lowercase()} settings", NamedTextColor.GREEN)
            lore += text("Right Click: Change ${sectionName.lowercase()} type", NamedTextColor.YELLOW)
        }.getItem()
    }

    override suspend fun edit(player: Player, context: EditorFieldContext, buttonType: ButtonType): FieldEditResult {
        return when (buttonType) {
            ButtonType.LEFT -> FieldEditResult.PassThrough
            ButtonType.RIGHT -> {
                val selected = selectType(player) ?: return FieldEditResult.NoChange
                val element = createElement(selected) ?: return FieldEditResult.NoChange
                FieldEditResult.Updated(mergeType(element, context.value))
            }
            else -> FieldEditResult.NoChange
        }
    }

    private fun mergeType(newElement: JsonElement, previousElement: JsonElement): JsonElement {
        val previousObject = previousElement as? JsonObject ?: return newElement
        val newObject = newElement as? JsonObject ?: return newElement
        val newType = (newObject["type"] as? JsonPrimitive)?.jsonPrimitive?.content
        val previousType = (previousObject["type"] as? JsonPrimitive)?.jsonPrimitive?.content

        if (newType != null && previousType != null && newType.lowercase() == previousType.lowercase()) {
            return previousElement
        }

        return newElement
    }

    private fun text(content: String, color: NamedTextColor): Component {
        return Component.text(content, color)
            .decoration(TextDecoration.ITALIC, false)
    }
}
