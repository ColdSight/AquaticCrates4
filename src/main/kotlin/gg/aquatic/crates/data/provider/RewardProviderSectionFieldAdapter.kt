package gg.aquatic.crates.data.provider

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

object RewardProviderSectionFieldAdapter : EditorFieldAdapter {
    override fun createItem(context: EditorFieldContext, defaultItem: () -> ItemStack): ItemStack {
        val currentType = currentType(context)
        return stackedItem(Material.CHEST_MINECART) {
            displayName = text("Rewards", NamedTextColor.AQUA)
            if (context.description.isNotEmpty()) {
                lore += text("Description", NamedTextColor.DARK_AQUA)
                lore += context.description.map { text(it, NamedTextColor.GRAY) }
            }
            lore += text("Current Type: $currentType", NamedTextColor.WHITE)
            lore += text("Left Click: Edit reward provider settings", NamedTextColor.GREEN)
            lore += text("Right Click: Change reward provider type", NamedTextColor.YELLOW)
        }.getItem()
    }

    override suspend fun edit(
        player: Player,
        context: EditorFieldContext,
        buttonType: ButtonType
    ): FieldEditResult {
        return when (buttonType) {
            ButtonType.LEFT -> FieldEditResult.PassThrough
            ButtonType.RIGHT -> {
                val selected = RewardProviderTypeSelectionMenu.select(player) ?: return FieldEditResult.NoChange
                FieldEditResult.UpdatedRoot(updateRewardProviderType(context.root, selected))
            }
            else -> FieldEditResult.NoChange
        }
    }

    private fun currentType(context: EditorFieldContext): String {
        val root = context.root as? JsonObject ?: return RewardProviderType.SIMPLE.id
        return (root["rewardProviderType"] as? JsonPrimitive)?.content ?: RewardProviderType.SIMPLE.id
    }

    private fun updateRewardProviderType(root: kotlinx.serialization.json.JsonElement, type: String): kotlinx.serialization.json.JsonElement {
        val objectRoot = root as? JsonObject ?: return root
        return JsonObject(
            objectRoot.toMutableMap().apply {
                put("rewardProviderType", JsonPrimitive(type))
            }
        )
    }

    private fun text(content: String, color: NamedTextColor): Component {
        return Component.text(content, color)
            .decoration(TextDecoration.ITALIC, false)
    }
}
