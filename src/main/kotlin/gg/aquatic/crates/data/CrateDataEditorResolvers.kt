package gg.aquatic.crates.data

import gg.aquatic.crates.data.action.RewardActionTypes
import gg.aquatic.crates.data.condition.PlayerConditionTypes
import gg.aquatic.crates.data.editor.mapValue
import gg.aquatic.crates.data.editor.stringContentOrNull
import gg.aquatic.crates.data.hologram.CrateHologramLineTypes
import gg.aquatic.crates.data.interaction.CrateClickActionTypes
import gg.aquatic.crates.data.interactable.CrateInteractableTypes
import gg.aquatic.crates.data.price.OpenPriceTypes
import gg.aquatic.crates.data.processor.RewardProcessorType
import gg.aquatic.crates.data.provider.RewardProviderType
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext

internal fun resolveCrateDataDescriptor(context: EditorFieldContext) = when {
    context.pathSegments.contains("hologram") ->
        context.currentSubtypeId()?.let(CrateHologramLineTypes::descriptor)
    context.pathSegments.contains("interactables") ->
        context.currentSubtypeId()?.let(CrateInteractableTypes::descriptor)
    (context.pathSegments.contains("priceGroups") || context.pathSegments.contains("cost")) && context.pathSegments.contains("prices") ->
        context.currentSubtypeId()?.let(OpenPriceTypes::descriptor)
    context.pathSegments.contains("winActions") || context.pathSegments.contains("clickActions") ->
        context.currentSubtypeId()?.let(RewardActionTypes::descriptor)
    context.pathSegments.any { it == "crateClickMapping" || it == "keyClickMapping" } ->
        context.currentSubtypeId()?.let(CrateClickActionTypes::descriptor)
    context.pathSegments.contains("openConditions") || context.pathSegments.contains("conditions") ->
        context.currentSubtypeId()?.let(PlayerConditionTypes::descriptor)
    else -> null
}

private fun EditorFieldContext.currentSubtypeId(): String? {
    return value.mapValue("type")?.stringContentOrNull
}

internal fun EditorFieldContext.isRewardProviderType(type: RewardProviderType): Boolean {
    val current = value
        .mapValue("rewardProviderType")
        ?.stringContentOrNull
    if (current != null) {
        return current.equals(type.id, true)
    }

    val rootType = root
        .mapValue("rewardProviderType")
        ?.stringContentOrNull

    return (rootType ?: RewardProviderType.SIMPLE.id).equals(type.id, true)
}

internal fun EditorFieldContext.isRewardProcessorType(type: RewardProcessorType): Boolean {
    val current = value
        .mapValue("rewardProcessorType")
        ?.stringContentOrNull
    if (current != null) {
        return current.equals(type.id, true)
    }

    val rootType = root
        .mapValue("rewardProcessorType")
        ?.stringContentOrNull

    return (rootType ?: RewardProcessorType.BASIC.id).equals(type.id, true)
}
