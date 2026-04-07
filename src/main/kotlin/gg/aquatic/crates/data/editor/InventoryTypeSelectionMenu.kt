package gg.aquatic.crates.data.editor

import gg.aquatic.crates.data.editor.menu.PagedSelectionMenu
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.stacked.stackedItem
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player

object InventoryTypeSelectionMenu {

    private val options = listOf(
        "GENERIC9X1",
        "GENERIC9X2",
        "GENERIC9X3",
        "GENERIC9X4",
        "GENERIC9X5",
        "GENERIC9X6",
        "GENERIC3X3",
        "CRAFTER3X3",
        "BEACON",
        "BLAST_FURNACE",
        "BREWING_STAND",
        "CRAFTING_TABLE",
        "ENCHANTMENT_TABLE",
        "FURNACE",
        "GRINDSTONE",
        "HOPPER",
        "LECTERN",
        "LOOM",
        "VILLAGER",
        "SHULKER_BOX",
        "SMITHING_TABLE",
        "SMOKER",
        "CARTOGRAPHY_TABLE",
        "STONECUTTER",
        "ANVIL"
    )

    sealed interface SelectionResult {
        data object Cancelled : SelectionResult
        data class Selected(val inventoryType: String) : SelectionResult
        data object PreviousPage : SelectionResult
        data object NextPage : SelectionResult
    }

    suspend fun select(player: Player, currentValue: String?): SelectionResult {
        return when (val result = PagedSelectionMenu.select(
            player = player,
            title = "Select Inventory Type",
            options = options,
            currentValue = currentValue,
            inventoryType = InventoryType.GENERIC9X6,
            buildEntry = ::buildEntry,
            cancelDescription = "Keep the current UI type",
            navigationDescription = "Open another page of UI types"
        )) {
            PagedSelectionMenu.Result.Cancelled -> SelectionResult.Cancelled
            is PagedSelectionMenu.Result.Selected -> SelectionResult.Selected(result.value)
            PagedSelectionMenu.Result.PreviousPage -> SelectionResult.PreviousPage
            PagedSelectionMenu.Result.NextPage -> SelectionResult.NextPage
        }
    }

    private fun buildEntry(inventoryTypeName: String, currentValue: String?) = stackedItem(Material.CHEST) {
        displayName = text(inventoryTypeName, NamedTextColor.AQUA)
        lore += text("Use this inventory layout", NamedTextColor.GRAY)
        lore += text("for the preview menu UI.", NamedTextColor.GRAY)
        if (inventoryTypeName == "ANVIL") {
            lore += text(" ", NamedTextColor.DARK_GRAY)
            lore += text("Supports confirm actions", NamedTextColor.YELLOW)
            lore += text("and %anvil_input% placeholder.", NamedTextColor.GRAY)
        }
        if (currentValue == inventoryTypeName) {
            lore += text(" ", NamedTextColor.DARK_GRAY)
            lore += text("Currently selected", NamedTextColor.GREEN)
        }
    }.getItem()

    private fun text(content: String, color: NamedTextColor): Component {
        return Component.text(content, color)
            .decoration(TextDecoration.ITALIC, false)
    }
}
