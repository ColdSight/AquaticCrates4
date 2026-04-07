package gg.aquatic.crates.data.interaction

import gg.aquatic.crates.data.CrateData
import gg.aquatic.crates.data.LimitData
import gg.aquatic.crates.data.condition.PlayerConditionData
import gg.aquatic.crates.data.interactable.BlockCrateInteractableData
import gg.aquatic.crates.data.interactable.CrateInteractableData
import gg.aquatic.crates.data.price.OpenPriceGroupData
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
data class InteractionSettingsData(
    val interactables: List<@Polymorphic CrateInteractableData> = listOf(BlockCrateInteractableData()),
    val openConditions: List<@Polymorphic PlayerConditionData> = emptyList(),
    val disableOpenStats: Boolean = false,
    val limits: List<LimitData> = emptyList(),
    val priceGroups: List<OpenPriceGroupData> = listOf(OpenPriceGroupData()),
    val crateClickMapping: CrateClickMappingData = CrateClickMappingData(),
) {
    companion object {
        fun from(crateData: CrateData): InteractionSettingsData {
            return InteractionSettingsData(
                interactables = crateData.interactables,
                openConditions = crateData.openConditions,
                disableOpenStats = crateData.disableOpenStats,
                limits = crateData.limits,
                priceGroups = crateData.priceGroups,
                crateClickMapping = crateData.crateClickMapping
            )
        }
    }
}
