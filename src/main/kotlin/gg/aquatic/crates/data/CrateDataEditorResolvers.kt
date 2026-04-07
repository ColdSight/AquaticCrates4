package gg.aquatic.crates.data

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import gg.aquatic.crates.data.action.RewardActionTypes
import gg.aquatic.crates.data.condition.PlayerConditionTypes
import gg.aquatic.crates.data.condition.findConditionSubtypeId
import gg.aquatic.crates.data.editor.listValue
import gg.aquatic.crates.data.editor.mapValue
import gg.aquatic.crates.data.editor.stringContentOrNull
import gg.aquatic.crates.data.hologram.CrateHologramLineTypes
import gg.aquatic.crates.data.hologram.findHologramLineSubtypeId
import gg.aquatic.crates.data.interaction.CrateClickActionTypes
import gg.aquatic.crates.data.interaction.findCrateClickActionSubtypeId
import gg.aquatic.crates.data.interactable.CrateInteractableTypes
import gg.aquatic.crates.data.interactable.findInteractableSubtypeId
import gg.aquatic.crates.data.price.OpenPriceTypes
import gg.aquatic.crates.data.price.findOpenPriceSubtypeId
import gg.aquatic.crates.data.processor.RewardProcessorType
import gg.aquatic.crates.data.provider.RewardProviderType
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext

internal fun resolveCrateDataDescriptor(context: EditorFieldContext) = when {
    context.pathSegments.contains("hologram") ->
        context.findHologramLineSubtypeId()?.let(CrateHologramLineTypes::descriptor)
    context.pathSegments.contains("interactables") ->
        context.findInteractableSubtypeId()?.let(CrateInteractableTypes::descriptor)
    (context.pathSegments.contains("priceGroups") || context.pathSegments.contains("cost")) && context.pathSegments.contains("prices") ->
        context.findOpenPriceSubtypeId()?.let(OpenPriceTypes::descriptor)
    context.pathSegments.contains("winActions") || context.pathSegments.contains("clickActions") ->
        context.findRewardActionType()?.let(RewardActionTypes::descriptor)
    context.pathSegments.any { it == "crateClickMapping" || it == "keyClickMapping" } ->
        context.findCrateClickActionSubtypeId()?.let(CrateClickActionTypes::descriptor)
    context.pathSegments.contains("openConditions") || context.pathSegments.contains("conditions") ->
        context.findConditionSubtypeId()?.let(PlayerConditionTypes::descriptor)
    else -> null
}

internal fun EditorFieldContext.isRewardProviderType(type: RewardProviderType): Boolean {
    val current = (value as? YamlMap)
        ?.get<YamlNode>("rewardProviderType")
        ?.stringContentOrNull
    if (current != null) {
        return current.equals(type.id, true)
    }

    val rootType = (root as? YamlMap)
        ?.get<YamlNode>("rewardProviderType")
        ?.stringContentOrNull

    return (rootType ?: RewardProviderType.SIMPLE.id).equals(type.id, true)
}

internal fun EditorFieldContext.isRewardProcessorType(type: RewardProcessorType): Boolean {
    val current = (value as? YamlMap)
        ?.get<YamlNode>("rewardProcessorType")
        ?.stringContentOrNull
    if (current != null) {
        return current.equals(type.id, true)
    }

    val rootType = (root as? YamlMap)
        ?.get<YamlNode>("rewardProcessorType")
        ?.stringContentOrNull

    return (rootType ?: RewardProcessorType.BASIC.id).equals(type.id, true)
}

private fun EditorFieldContext.findRewardActionType(): String? {
    if (!pathSegments.contains("winActions") && !pathSegments.contains("clickActions")) {
        return null
    }

    val direct = (value as? YamlMap)
        ?.get<YamlNode>("type")
        ?.stringContentOrNull
    if (direct != null) {
        return direct
    }

    val actionIndex = pathSegments.indexOfLast { it.toIntOrNull() != null }
    if (actionIndex == -1) {
        return null
    }

    var current: YamlNode = root
    for (segment in pathSegments.take(actionIndex + 1)) {
        current = when (val numericIndex = segment.toIntOrNull()) {
            null -> current.mapValue(segment) ?: return null
            else -> current.listValue(numericIndex) ?: return null
        }
    }

    return (current as? YamlMap)
        ?.get<YamlNode>("type")
        ?.stringContentOrNull
}
