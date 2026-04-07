package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.data.condition.CrateOpenConditionBinder
import gg.aquatic.crates.data.hologram.RewardHologramEntry
import gg.aquatic.crates.milestone.CrateMilestoneRuntimeFactory
import gg.aquatic.crates.data.processor.RewardProcessorType
import gg.aquatic.crates.data.provider.RewardProviderType
import gg.aquatic.crates.reward.runtime.RewardRuntimeFactory
import gg.aquatic.crates.reward.processor.BasicRewardProcessor
import gg.aquatic.crates.reward.processor.ChooseRewardProcessor
import gg.aquatic.crates.reward.provider.ConditionalPoolsRewardProvider
import gg.aquatic.crates.reward.provider.SimpleRewardProvider

fun CrateData.toCrate(id: String): Crate {
    val normalized = normalized(id)
    val crateKeyStackedItem by lazy { normalized.keyItem.asStacked() }
    val crateKeyItem by lazy { crateKeyStackedItem.getItem() }
    val rewardHologramEntries by lazy { normalized.rewardHologramEntries() }

    return Crate(
        id = id,
        keyItemSupplier = { crateKeyStackedItem },
        keyMustBeHeld = normalized.keyMustBeHeld,
        crateClickMapping = normalized.crateClickMapping,
        keyClickMapping = normalized.keyClickMapping,
        displayName = normalized.displayName.toMMComponent(),
        hologramSupplier = { normalized.hologram?.toSettings(rewardHologramEntries) },
        priceGroupsSupplier = { normalized.priceGroups.map { it.toOpenPriceGroup(id, crateKeyItem) } },
        openConditionsSupplier = {
            normalized.openConditions
                .takeIf { it.isNotEmpty() }
                ?.let { conditions ->
                    gg.aquatic.crates.open.OpenConditions { player, crate, crateHandle ->
                        val binder = CrateOpenConditionBinder(player, crate, crateHandle)
                        conditions
                            .map { it.toOpenConditionHandle() }
                            .all { it.execute(binder) { _, str -> str } }
                    }
                }
                ?: gg.aquatic.crates.open.OpenConditions.DUMMY
        },
        interactables = normalized.interactables,
        disableOpenStats = normalized.disableOpenStats,
        limits = normalized.limits.map { it.toHandle() },
        milestoneManagerSupplier = {
            CrateMilestoneRuntimeFactory.createManager(
                crateId = id,
                keyItem = crateKeyItem,
                rarities = normalized.rarities,
                milestones = normalized.milestones,
                repeatableMilestones = normalized.repeatableMilestones
            )
        },
        rewardProviderSupplier = {
            when (RewardProviderType.of(normalized.rewardProviderType)) {
                RewardProviderType.CONDITIONAL_POOLS -> ConditionalPoolsRewardProvider(
                    selectionMode = gg.aquatic.crates.data.provider.PoolSelectionMode.of(normalized.conditionalPoolsProvider.poolSelectionMode),
                    fallbackPoolId = normalized.conditionalPoolsProvider.fallbackPoolId,
                    pools = normalized.conditionalPoolsProvider.pools.mapValues { (poolId, poolData) ->
                        poolData.toRewardPool(poolId, id, crateKeyItem, normalized.rarities)
                    },
                    rewardCountRanges = normalized.conditionalPoolsProvider.rewardCountRanges.map { it.toRange() }
                )

                RewardProviderType.SIMPLE -> SimpleRewardProvider(
                    buildRewards(
                        rarities = normalized.rarities,
                        rewards = normalized.simpleProvider.rewards,
                        crateId = id,
                        crateKeyItem = crateKeyItem
                    ),
                    rewardCountRanges = normalized.simpleProvider.rewardCountRanges.map { it.toRange() }
                )
            }
        },
        rewardProcessorSupplier = {
            when (RewardProcessorType.of(normalized.rewardProcessorType)) {
                RewardProcessorType.CHOOSE -> ChooseRewardProcessor(
                    chooseCountRanges = normalized.chooseProcessor.chooseCountRanges.map { it.toRange() },
                    uniqueRewards = normalized.chooseProcessor.uniqueRewards,
                    hiddenRewards = normalized.chooseProcessor.hiddenRewards,
                    onSelectActions = normalized.chooseProcessor.onSelectActions.map { it.toActionHandle() },
                    hiddenItem = normalized.chooseProcessor.hiddenItem.asStacked().getItem(),
                    menu = normalized.chooseProcessor.menu.toMenuSettings(),
                )

                RewardProcessorType.BASIC -> BasicRewardProcessor(
                    resultMenu = normalized.basicProcessor.resultMenu?.toMenuSettings()
                )
            }
        },
        previewSupplier = { normalized.preview?.toPreviewSettings() }
    )
}

private fun buildRewards(
    rarities: Map<String, RewardRarityData>,
    rewards: Map<String, RewardData>,
    crateId: String,
    crateKeyItem: org.bukkit.inventory.ItemStack,
): Collection<gg.aquatic.crates.reward.Reward> {
    val resolvedRarities = rarities.mapValues { (rarityId, rarityData) ->
        rarityData.toRewardRarity(rarityId)
    }
    val fallbackRarity = resolvedRarities.values.first()

    return rewards.entries.map { (rewardId, rewardData) ->
        val rewardRarity = resolvedRarities[rewardData.rarity] ?: fallbackRarity
        RewardRuntimeFactory.create(rewardData, rewardId, crateId, crateKeyItem, rewardRarity)
    }.toMutableList().also { builtRewards ->
        normalizeRewardChances(builtRewards, resolvedRarities)
    }
}

private fun CrateData.rewardHologramEntries(): List<RewardHologramEntry> {
    val rewards = when (RewardProviderType.of(rewardProviderType)) {
        RewardProviderType.CONDITIONAL_POOLS -> conditionalPoolsProvider.rewardEntries()
        RewardProviderType.SIMPLE -> simpleProvider.rewardEntries()
    }

    return rewards.map { (rewardId, rewardData) ->
        val item = rewardData.previewItem.asStacked().getItem()
        val displayName = rewardData.displayName?.toMMComponent()
            ?: item.itemMeta.displayName()
            ?: net.kyori.adventure.text.Component.text(rewardId)

        RewardHologramEntry(
            item = item,
            displayName = displayName
        )
    }
}
