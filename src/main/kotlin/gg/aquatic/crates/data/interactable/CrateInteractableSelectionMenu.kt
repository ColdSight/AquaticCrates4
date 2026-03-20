package gg.aquatic.crates.data.interactable

import gg.aquatic.crates.data.editor.PolymorphicSelectionMenu
import gg.aquatic.waves.serialization.editor.meta.EntryFactory
import org.bukkit.Bukkit

object CrateInteractableSelectionMenu {

    private val entrySlots = listOf(10, 12, 14, 16)

    val entryFactory: EntryFactory = PolymorphicSelectionMenu.entryFactory(
        title = "Select Interactable",
        entrySlots = entrySlots,
        definitions = CrateInteractableTypes.definitions.map { definition ->
            PolymorphicSelectionMenu.Definition(
                id = definition.id,
                displayName = definition.displayName,
                description = definition.description,
                icon = definition.icon,
                availability = { _ ->
                    when (definition.id) {
                        "meg" -> PolymorphicSelectionMenu.Availability(
                            available = Bukkit.getPluginManager().getPlugin("ModelEngine") != null,
                            lockedDescription = listOf("Requires ModelEngine on the server"),
                            deniedMessage = "ModelEngine is not installed, so this interactable type is unavailable."
                        )

                        else -> PolymorphicSelectionMenu.Availability(true)
                    }
                }
            )
        },
        elementFactory = CrateInteractableTypes::defaultElement
    )
}
