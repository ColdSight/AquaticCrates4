package gg.aquatic.crates.data.editor

import gg.aquatic.crates.data.PreviewType
import gg.aquatic.kmenu.inventory.InventoryType
import org.bukkit.Material

object PreviewTypeSelectionMenu {
    private val entrySlots = listOf(11, 15)

    suspend fun select(player: org.bukkit.entity.Player): String? {
        return PolymorphicSelectionMenu.selectType(
            player = player,
            title = "Select Preview Type",
            inventoryType = InventoryType.GENERIC9X3,
            entrySlots = entrySlots,
            cancelSlot = 22,
            definitions = listOf(
                PolymorphicSelectionMenu.Definition(
                    id = PreviewType.AUTOMATIC.id,
                    displayName = "Automatic",
                    description = listOf(
                        "Uses one shared layout.",
                        "Rewards are paginated automatically.",
                        "Best for standard crate previews."
                    ),
                    icon = Material.CRAFTER
                ),
                PolymorphicSelectionMenu.Definition(
                    id = PreviewType.CUSTOM_PAGES.id,
                    displayName = "Custom Pages",
                    description = listOf(
                        "Lets you define each page manually.",
                        "Each page can have its own layout and buttons.",
                        "Best for fully custom preview flows."
                    ),
                    icon = Material.BOOK
                )
            )
        )
    }
}
