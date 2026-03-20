package gg.aquatic.crates.data.condition

import gg.aquatic.crates.data.editor.PolymorphicSelectionMenu
import gg.aquatic.waves.serialization.editor.meta.EntryFactory

object PlayerConditionSelectionMenu {

    private val entrySlots = listOf(13)

    val entryFactory: EntryFactory = PolymorphicSelectionMenu.entryFactory(
        title = "Select Condition",
        entrySlots = entrySlots,
        definitions = PlayerConditionTypes.definitions.map { definition ->
            PolymorphicSelectionMenu.Definition(
                id = definition.id,
                displayName = definition.displayName,
                description = definition.description,
                icon = definition.icon
            )
        },
        elementFactory = PlayerConditionTypes::defaultElement
    )
}
