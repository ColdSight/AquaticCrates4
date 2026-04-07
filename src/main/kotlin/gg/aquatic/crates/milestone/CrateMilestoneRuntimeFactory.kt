package gg.aquatic.crates.milestone

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.data.CrateData
import gg.aquatic.crates.data.MilestoneData
import gg.aquatic.crates.data.RewardData
import gg.aquatic.crates.data.RewardRarityData
import gg.aquatic.crates.data.normalizeRewardChances
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.runtime.RewardRuntimeFactory

object CrateMilestoneRuntimeFactory {
    fun createManager(
        crateId: String,
        keyItem: org.bukkit.inventory.ItemStack,
        rarities: Map<String, RewardRarityData>,
        milestones: List<MilestoneData>,
        repeatableMilestones: List<MilestoneData>,
    ): CrateMilestoneManager {
        return CrateMilestoneManager(
            milestones = milestones.map { createMilestone(crateId, keyItem, rarities, it, repeatable = false) },
            repeatableMilestones = repeatableMilestones.map { createMilestone(crateId, keyItem, rarities, it, repeatable = true) }
        )
    }

    private fun createMilestone(
        crateId: String,
        keyItem: org.bukkit.inventory.ItemStack,
        rarities: Map<String, RewardRarityData>,
        data: MilestoneData,
        repeatable: Boolean,
    ): CrateMilestone {
        return CrateMilestone(
            milestone = data.milestone,
            displayName = data.displayName?.toMMComponent(),
            rewards = buildRewards(
                crateId = crateId,
                keyItem = keyItem,
                rarities = rarities,
                rewards = data.rewards,
                rewardIdPrefix = if (repeatable) "repeatable_milestone_${data.milestone}_" else "milestone_${data.milestone}_"
            )
        )
    }

    private fun buildRewards(
        crateId: String,
        keyItem: org.bukkit.inventory.ItemStack,
        rarities: Map<String, RewardRarityData>,
        rewards: Map<String, RewardData>,
        rewardIdPrefix: String,
    ): Collection<Reward> {
        val resolvedRarities = rarities.mapValues { (rarityId, rarityData) ->
            rarityData.toRewardRarity(rarityId)
        }
        val fallbackRarity = resolvedRarities.values.first()

        return rewards.entries.map { (rewardId, rewardData) ->
            val rewardRarity = resolvedRarities[rewardData.rarity] ?: fallbackRarity
            RewardRuntimeFactory.create(rewardData, "$rewardIdPrefix$rewardId", crateId, keyItem, rewardRarity)
        }.toMutableList().also { builtRewards ->
            normalizeRewardChances(builtRewards, resolvedRarities)
        }
    }
}
