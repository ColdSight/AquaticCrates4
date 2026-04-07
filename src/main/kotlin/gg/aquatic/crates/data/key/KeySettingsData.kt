package gg.aquatic.crates.data.key

import gg.aquatic.crates.data.CrateData
import gg.aquatic.crates.data.interaction.CrateClickMappingData
import gg.aquatic.crates.data.item.StackedItemData
import kotlinx.serialization.Serializable

@Serializable
data class KeySettingsData(
    val keyItem: StackedItemData = StackedItemData(),
    val keyMustBeHeld: Boolean = false,
    val keyClickMapping: CrateClickMappingData = CrateClickMappingData(),
) {
    companion object {
        fun from(crateData: CrateData): KeySettingsData {
            return KeySettingsData(
                keyItem = crateData.keyItem,
                keyMustBeHeld = crateData.keyMustBeHeld,
                keyClickMapping = crateData.keyClickMapping
            )
        }
    }
}
