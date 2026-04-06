package gg.aquatic.crates.message.condition

import gg.aquatic.crates.data.editor.PolymorphicEntryFieldAdapter

object MessageConditionEntryFieldAdapter : PolymorphicEntryFieldAdapter(
    sectionName = "Visibility Condition",
    iconResolver = { MessageConditionTypes.definition(it ?: "")?.icon },
    nameResolver = { MessageConditionTypes.definition(it ?: "")?.displayName },
    selectType = MessageConditionSelectionMenu::select,
    createElement = MessageConditionTypes::defaultElement,
    currentTypeResolver = { it.findMessageConditionSubtypeId() }
)
