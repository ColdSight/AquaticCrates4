package gg.aquatic.crates.data.condition

import gg.aquatic.crates.data.editor.PolymorphicSelectionMenu
import gg.aquatic.waves.serialization.editor.meta.EntryFactory

object PlayerConditionSelectionMenu {

    private val entrySlots = listOf(13)

    private fun selectionDefinitions(definitions: List<PlayerConditionTypes.Definition>) = definitions.map { definition ->
        PolymorphicSelectionMenu.Definition(
            id = definition.id,
            displayName = definition.displayName,
            description = definition.description,
            icon = definition.icon
        )
    }

    private val definitions = selectionDefinitions(PlayerConditionTypes.definitions.filterNot {
        it.id == "world-blacklist" || it.id == "available-rewards"
    })

    val entryFactory: EntryFactory = PolymorphicSelectionMenu.entryFactory(
        title = "Select Condition",
        entrySlots = entrySlots,
        definitions = definitions,
        elementFactory = PlayerConditionTypes::defaultElement
    )

    suspend fun select(player: org.bukkit.entity.Player): String? {
        return PolymorphicSelectionMenu.selectType(
            player = player,
            title = "Select Condition",
            inventoryType = gg.aquatic.kmenu.inventory.InventoryType.GENERIC9X3,
            entrySlots = entrySlots,
            cancelSlot = 22,
            definitions = definitions
        )
    }
}

object OpenPlayerConditionSelectionMenu {

    private val entrySlots = listOf(13)
    private val definitions = PlayerConditionTypes.definitions.map { definition ->
        PolymorphicSelectionMenu.Definition(
            id = definition.id,
            displayName = definition.displayName,
            description = definition.description,
            icon = definition.icon
        )
    }

    val entryFactory: EntryFactory = PolymorphicSelectionMenu.entryFactory(
        title = "Select Condition",
        entrySlots = entrySlots,
        definitions = definitions,
        elementFactory = PlayerConditionTypes::defaultElement
    )

    suspend fun select(player: org.bukkit.entity.Player): String? {
        return PolymorphicSelectionMenu.selectType(
            player = player,
            title = "Select Condition",
            inventoryType = gg.aquatic.kmenu.inventory.InventoryType.GENERIC9X3,
            entrySlots = entrySlots,
            cancelSlot = 22,
            definitions = definitions
        )
    }
}
