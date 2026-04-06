package gg.aquatic.crates.data

import gg.aquatic.crates.data.processor.RewardProcessorType
import gg.aquatic.crates.data.provider.RewardProviderType

fun CrateData.normalized(crateId: String? = null, existingCrateIds: Set<String> = emptySet()): CrateData {
    val normalizedProviderType = RewardProviderType.of(rewardProviderType).id
    val normalizedRarities = rarities
        .mapNotNull { (rarityId, data) ->
            rarityId.trim()
                .takeIf { it.isNotEmpty() }
                ?.let { it to data }
        }
        .toMap()
        .ifEmpty { mapOf(CrateData.DEFAULT_RARITY_ID to RewardRarityData(displayName = "<gray>Default")) }
    val fallbackRarityId = normalizedRarities.keys.first()
    val availableRarityIds = normalizedRarities.keys

    return copy(
        rarities = normalizedRarities,
        rewardProviderType = normalizedProviderType,
        simpleProvider = simpleProvider.normalized(availableRarityIds, fallbackRarityId, crateId, existingCrateIds),
        conditionalPoolsProvider = conditionalPoolsProvider.normalized(availableRarityIds, fallbackRarityId, crateId, existingCrateIds),
        rewardProcessorType = RewardProcessorType.of(rewardProcessorType).id,
        chooseProcessor = chooseProcessor.normalized(),
        limits = limits.map { it.normalized() }.distinctBy { it.timeframe },
        priceGroups = priceGroups.map { it.normalized(crateId, existingCrateIds) },
    )
}
