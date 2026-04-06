package gg.aquatic.crates.data.interaction

import gg.aquatic.crates.data.editor.PolymorphicEntryFieldAdapter

class CrateClickActionEntryFieldAdapter(
    allowDestroy: Boolean,
) : PolymorphicEntryFieldAdapter(
    sectionName = "Action",
    iconResolver = { CrateClickActionTypes.definition(it ?: "")?.icon },
    nameResolver = { CrateClickActionTypes.definition(it ?: "")?.displayName },
    selectType = { player -> CrateClickActionSelectionMenu.select(player, allowDestroy) },
    createElement = CrateClickActionTypes::defaultElement,
    currentTypeResolver = { it.findCrateClickActionSubtypeId() }
)
