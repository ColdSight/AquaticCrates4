package gg.aquatic.crates.data

import gg.aquatic.crates.data.menu.MenuInventoryData
import kotlinx.serialization.Serializable

@Serializable
data class PreviewMenuData(
    val previewType: String = PREVIEW_TYPE_AUTOMATIC,
    val inventory: MenuInventoryData = MenuInventoryData(),
    val title: String = "<yellow>Preview Menu",
    val rewardSlots: List<Int> = listOf(10, 11, 12, 13, 14, 15, 16),
    val randomRewardSlots: List<Int> = emptyList(),
    val randomRewardSwitchTicks: Int = 20,
    val randomRewardUnique: Boolean = false,
    val rewardLore: List<String> = emptyList(),
    val customButtons: Map<String, PreviewButtonData> = emptyMap(),
    val pages: List<PreviewPageData> = emptyList(),
) {
    companion object
}

@Serializable
data class PreviewPageData(
    val inventory: MenuInventoryData = MenuInventoryData(),
    val title: String = "<yellow>Preview Page",
    val rewardSlots: List<Int> = listOf(10, 11, 12, 13, 14, 15, 16),
    val randomRewardSlots: List<Int> = emptyList(),
    val randomRewardSwitchTicks: Int = 20,
    val randomRewardUnique: Boolean = false,
    val rewardLore: List<String> = emptyList(),
    val customButtons: Map<String, PreviewButtonData> = emptyMap(),
) {
    companion object
}

const val PREVIEW_TYPE_AUTOMATIC = "automatic"
const val PREVIEW_TYPE_CUSTOM_PAGES = "custom-pages"
