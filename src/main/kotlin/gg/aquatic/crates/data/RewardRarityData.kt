package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.reward.RewardRarity
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.Serializable

@Serializable
data class RewardRarityData(
    val id: String = "default",
    val displayName: String? = null,
    val chance: Double = 1.0,
) {

    fun toRewardRarity(): RewardRarity {
        return RewardRarity(
            id = id,
            displayName = (displayName ?: id).toMMComponent(),
            chance = chance
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<RewardRarityData>.defineEditor() {
            field(
                RewardRarityData::id,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter rarity id:"),
                displayName = "Rarity Id",
                description = listOf("Internal id of the rarity group.")
            )
            field(
                RewardRarityData::displayName,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter rarity display name:", showFormattedPreview = true),
                displayName = "Rarity Name",
                description = listOf("Display name shown for this rarity in reward previews.")
            )
            field(
                RewardRarityData::chance,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter rarity chance:", min = 0.0),
                displayName = "Rarity Chance",
                description = listOf("Relative weight of this rarity when rewards are grouped.")
            )
        }
    }
}
