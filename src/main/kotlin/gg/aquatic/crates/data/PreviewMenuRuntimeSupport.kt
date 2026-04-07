package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.preview.PreviewMenuSettings
import gg.aquatic.kmenu.menu.settings.PrivateMenuSettings

fun PreviewMenuData.toPreviewSettings(): PreviewMenuSettings {
    val type = normalizePreviewType(previewType)
    if (type == PREVIEW_TYPE_CUSTOM_PAGES && pages.isNotEmpty()) {
        return PreviewMenuSettings.CustomPages(pages.map { it.toBasicSettings() })
    }

    return toBasicSettings()
}

fun PreviewMenuData.toBasicSettings(): PreviewMenuSettings.Basic {
    val inventorySettings = inventory.toRuntimeSettings()
    val components = hashMapOf<String, gg.aquatic.kmenu.menu.settings.IButtonSettings>()

    customButtons.forEach { (id, button) ->
        components[id] = button.toButtonSettings(id)
    }

    return PreviewMenuSettings.Basic(
        rewardSlots = rewardSlots.distinct(),
        randomRewardSlots = randomRewardSlots.distinct(),
        randomRewardSwitchTicks = randomRewardSwitchTicks.coerceAtLeast(1),
        randomRewardUnique = randomRewardUnique,
        rewardLore = rewardLore,
        invSettings = PrivateMenuSettings(
            inventorySettings.inventoryType,
            title.toMMComponent(),
            components
        ),
        anvilSettings = inventorySettings.anvil
    )
}

fun PreviewPageData.toBasicSettings(): PreviewMenuSettings.Basic {
    return PreviewMenuData(
        inventory = inventory,
        title = title,
        rewardSlots = rewardSlots,
        randomRewardSlots = randomRewardSlots,
        randomRewardSwitchTicks = randomRewardSwitchTicks,
        randomRewardUnique = randomRewardUnique,
        rewardLore = rewardLore,
        customButtons = customButtons
    ).toBasicSettings()
}

internal fun normalizePreviewType(raw: String): String {
    return when {
        raw.equals(PREVIEW_TYPE_CUSTOM_PAGES, ignoreCase = true) -> PREVIEW_TYPE_CUSTOM_PAGES
        else -> PREVIEW_TYPE_AUTOMATIC
    }
}
