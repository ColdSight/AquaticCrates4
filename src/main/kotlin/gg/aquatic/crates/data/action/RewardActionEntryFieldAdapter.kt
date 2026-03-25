package gg.aquatic.crates.data.action

import gg.aquatic.crates.data.editor.PolymorphicEntryFieldAdapter

object RewardActionEntryFieldAdapter : PolymorphicEntryFieldAdapter(
    sectionName = "Action",
    iconResolver = { RewardActionTypes.definition(it ?: "")?.icon },
    nameResolver = { RewardActionTypes.definition(it ?: "")?.displayName },
    selectType = RewardActionSelectionMenu::select,
    createElement = RewardActionTypes::defaultElement,
    currentTypeResolver = { it.findRewardActionSubtypeId() }
)
