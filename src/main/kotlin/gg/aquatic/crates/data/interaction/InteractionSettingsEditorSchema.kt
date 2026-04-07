package gg.aquatic.crates.data.interaction

import gg.aquatic.crates.data.LimitData
import gg.aquatic.crates.data.condition.OpenPlayerConditionEntryFieldAdapter
import gg.aquatic.crates.data.condition.OpenPlayerConditionSelectionMenu
import gg.aquatic.crates.data.condition.definePlayerConditionEditor
import gg.aquatic.crates.data.interactable.BlockCrateInteractableData
import gg.aquatic.crates.data.interactable.CrateInteractableEntryFieldAdapter
import gg.aquatic.crates.data.interactable.CrateInteractableSelectionMenu
import gg.aquatic.crates.data.interactable.EntityCrateInteractableData
import gg.aquatic.crates.data.interactable.MEGCrateInteractableData
import gg.aquatic.crates.data.interactable.MultiBlockCrateInteractableData
import gg.aquatic.crates.data.price.OpenPriceGroupData
import gg.aquatic.waves.serialization.editor.meta.EditableModel
import gg.aquatic.waves.serialization.editor.meta.TypedEditorSchemaBuilder
import org.bukkit.Material

object InteractionSettingsEditorSchema : EditableModel<InteractionSettingsData>(InteractionSettingsData.serializer()) {
    override fun TypedEditorSchemaBuilder<InteractionSettingsData>.define() {
        list(
            InteractionSettingsData::interactables,
            displayName = "Interactables",
            iconMaterial = Material.ARMOR_STAND,
            description = listOf("Clientside objects players can click to open this crate."),
            newValueFactory = CrateInteractableSelectionMenu.entryFactory
        ) {
            fieldPattern(
                displayName = "Interactable",
                adapter = CrateInteractableEntryFieldAdapter,
                description = listOf(
                    "Left click to edit this interactable.",
                    "Right click to change its interactable type."
                )
            )
            include { with(BlockCrateInteractableData) { defineEditor() } }
            include { with(EntityCrateInteractableData) { defineEditor() } }
            include { with(MEGCrateInteractableData) { defineEditor() } }
            include { with(MultiBlockCrateInteractableData) { defineEditor() } }
        }
        list(
            InteractionSettingsData::openConditions,
            displayName = "Open Conditions",
            iconMaterial = Material.TRIPWIRE_HOOK,
            description = listOf("Conditions that must pass before the crate can be opened."),
            newValueFactory = OpenPlayerConditionSelectionMenu.entryFactory
        ) {
            definePlayerConditionEditor(
                includeOpenOnlyConditions = true,
                adapter = OpenPlayerConditionEntryFieldAdapter
            )
        }
        field(
            InteractionSettingsData::disableOpenStats,
            displayName = "Disable Open Stats",
            prompt = "Enter true or false:",
            iconMaterial = Material.LECTERN,
            description = listOf("If enabled, openings and won rewards from this crate will not be written into the stats database.")
        )
        list(
            InteractionSettingsData::limits,
            displayName = "Limits",
            iconMaterial = Material.CLOCK,
            description = listOf("Per-player rolling limits for how often this crate can be opened.")
        ) {
            with(LimitData) { defineEditor() }
        }
        list(
            InteractionSettingsData::priceGroups,
            displayName = "Price Groups",
            iconMaterial = Material.GOLD_INGOT,
            description = listOf(
                "Alternative price groups used to open this crate.",
                "If one group can be paid in full, the crate opens."
            ),
            newValueFactory = OpenPriceGroupData.defaultEntryFactory
        ) {
            with(OpenPriceGroupData) { defineEditor() }
        }
        group(InteractionSettingsData::crateClickMapping) {
            with(CrateClickMappingData) { defineEditor("Crate Click") }
        }
    }
}
