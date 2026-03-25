package gg.aquatic.crates.data.processor

import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.serialization.editor.meta.EditorFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.FieldEditResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object RewardProcessorSectionFieldAdapter : EditorFieldAdapter {
    override fun createItem(context: EditorFieldContext, defaultItem: () -> ItemStack): ItemStack {
        val currentType = currentType(context)
        return stackedItem(Material.HOPPER_MINECART) {
            displayName = text("Reward Processor", NamedTextColor.AQUA)
            if (context.description.isNotEmpty()) {
                lore += text("Description", NamedTextColor.DARK_AQUA)
                lore += context.description.map { text(it, NamedTextColor.GRAY) }
            }
            lore += text("Current Type: $currentType", NamedTextColor.WHITE)
            lore += text("Left Click: Edit processor settings", NamedTextColor.GREEN)
            lore += text("Right Click: Change processor type", NamedTextColor.YELLOW)
        }.getItem()
    }

    override suspend fun edit(player: Player, context: EditorFieldContext, buttonType: ButtonType): FieldEditResult {
        return when (buttonType) {
            ButtonType.LEFT -> FieldEditResult.PassThrough
            ButtonType.RIGHT -> {
                val selected = RewardProcessorTypeSelectionMenu.select(player) ?: return FieldEditResult.NoChange
                FieldEditResult.UpdatedRoot(updateRewardProcessorType(context.root, selected))
            }
            else -> FieldEditResult.NoChange
        }
    }

    private fun currentType(context: EditorFieldContext): String {
        val root = context.root as? JsonObject ?: return RewardProcessorType.BASIC.id
        return (root["rewardProcessorType"] as? JsonPrimitive)?.content ?: RewardProcessorType.BASIC.id
    }

    private fun updateRewardProcessorType(root: kotlinx.serialization.json.JsonElement, type: String): kotlinx.serialization.json.JsonElement {
        val objectRoot = root as? JsonObject ?: return root
        return JsonObject(
            objectRoot.toMutableMap().apply {
                put("rewardProcessorType", JsonPrimitive(type))
            }
        )
    }

    private fun text(content: String, color: NamedTextColor): Component {
        return Component.text(content, color)
            .decoration(TextDecoration.ITALIC, false)
    }
}
