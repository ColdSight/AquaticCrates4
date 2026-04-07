package gg.aquatic.crates.data.milestone

import gg.aquatic.crates.data.MilestoneData
import gg.aquatic.crates.data.resolveCrateDataDescriptor
import gg.aquatic.waves.serialization.editor.meta.EditableModel
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.TypedEditorSchemaBuilder
import org.bukkit.Material

object MilestoneSettingsEditorSchema : EditableModel<MilestoneSettingsData>(MilestoneSettingsData.serializer()) {
    override fun resolveDescriptor(context: EditorFieldContext) = resolveCrateDataDescriptor(context)

    override fun TypedEditorSchemaBuilder<MilestoneSettingsData>.define() {
        list(
            MilestoneSettingsData::milestones,
            displayName = "Milestones",
            iconMaterial = Material.DIAMOND,
            description = listOf("Rewards granted once when the player's alltime opens reach the exact target.")
        ) {
            with(MilestoneData) { defineEditor() }
        }
        list(
            MilestoneSettingsData::repeatableMilestones,
            displayName = "Repeatable Milestones",
            iconMaterial = Material.CLOCK,
            description = listOf("Rewards granted every time the player's alltime opens are divisible by the target.")
        ) {
            with(MilestoneData) { defineEditor() }
        }
    }
}
