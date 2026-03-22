package gg.aquatic.crates.data.price

import gg.aquatic.crates.data.editor.PolymorphicSelectionMenu
import gg.aquatic.waves.serialization.editor.meta.EntryFactory

object OpenPriceSelectionMenu {
    val entryFactory: EntryFactory = PolymorphicSelectionMenu.entryFactory(
        title = "Select Open Price",
        entrySlots = listOf(13),
        definitions = OpenPriceTypes.definitions.map { definition ->
            PolymorphicSelectionMenu.Definition(
                id = definition.id,
                displayName = definition.displayName,
                description = definition.description,
                icon = definition.icon
            )
        },
        elementFactory = OpenPriceTypes::defaultElement
    )
}
