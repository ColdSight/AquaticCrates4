package gg.aquatic.crates.data.processor

import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class BasicRewardProcessorData(
    val resultMenu: RewardDisplayMenuData? = null,
) {
    companion object {
        fun TypedNestedSchemaBuilder<BasicRewardProcessorData>.defineEditor() {
            field(
                BasicRewardProcessorData::resultMenu,
                displayName = "Result Menu",
                iconMaterial = Material.CHEST,
                description = listOf(
                    "Optional showcase menu opened after the rewards are already given.",
                    "Use it to show the player which rewards were won.",
                    "Click to create it. Press Q to clear it back to null."
                )
            )
            optionalGroup(BasicRewardProcessorData::resultMenu) {
                with(RewardDisplayMenuData) {
                    defineEditor()
                }
            }
        }
    }
}
