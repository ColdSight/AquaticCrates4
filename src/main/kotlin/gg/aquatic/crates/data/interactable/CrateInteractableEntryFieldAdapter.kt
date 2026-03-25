package gg.aquatic.crates.data.interactable

import gg.aquatic.crates.data.editor.PolymorphicEntryFieldAdapter

object CrateInteractableEntryFieldAdapter : PolymorphicEntryFieldAdapter(
    sectionName = "Interactable",
    iconResolver = { CrateInteractableTypes.definition(it ?: "")?.icon },
    nameResolver = { CrateInteractableTypes.definition(it ?: "")?.displayName },
    selectType = CrateInteractableSelectionMenu::select,
    createElement = CrateInteractableTypes::defaultElement,
    currentTypeResolver = { it.findInteractableSubtypeId() }
)
