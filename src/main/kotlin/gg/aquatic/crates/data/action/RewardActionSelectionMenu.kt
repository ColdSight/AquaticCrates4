package gg.aquatic.crates.data.action

import gg.aquatic.crates.data.editor.PolymorphicSelectionMenu
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.waves.serialization.editor.meta.EntryFactory

object RewardActionSelectionMenu {

    private val entrySlots = listOf(10, 11, 12, 13, 14, 15, 16, 19)

    val entryFactory: EntryFactory = PolymorphicSelectionMenu.entryFactory(
        title = "Select Win Action",
        inventoryType = InventoryType.GENERIC9X4,
        entrySlots = entrySlots,
        cancelSlot = 31,
        definitions = RewardActionTypes.definitions.map { definition ->
            PolymorphicSelectionMenu.Definition(
                id = definition.id,
                displayName = definition.displayName,
                description = definition.description,
                icon = definition.icon
            )
        },
        elementFactory = RewardActionTypes::defaultElement
    )
}
