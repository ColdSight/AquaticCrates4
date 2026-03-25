package gg.aquatic.crates.data

import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.RewardRarity

fun normalizeRewardChances(
    rewards: MutableList<Reward>,
    rarities: Map<String, RewardRarity>
) {
    if (rewards.isEmpty() || rarities.isEmpty()) return

    val groupedRewards = rewards.groupBy { it.rarity.id }
    val activeRarities = rarities.filterKeys { groupedRewards.containsKey(it) }
    if (activeRarities.isEmpty()) return

    val totalRarityWeight = activeRarities.values.sumOf { it.chance.coerceAtLeast(0.0) }

    activeRarities.forEach { (rarityId, rarity) ->
        val rarityRewards = groupedRewards[rarityId].orEmpty()
        if (rarityRewards.isEmpty()) return@forEach

        val rarityFactor = when {
            totalRarityWeight > 0.0 -> rarity.chance.coerceAtLeast(0.0) / totalRarityWeight
            else -> 1.0 / activeRarities.size
        }

        val totalRewardWeight = rarityRewards.sumOf { it.chance.coerceAtLeast(0.0) }
        if (totalRewardWeight > 0.0) {
            rarityRewards.forEach { reward ->
                reward.chance = rarityFactor * (reward.chance.coerceAtLeast(0.0) / totalRewardWeight)
            }
        } else {
            val evenChance = rarityFactor / rarityRewards.size
            rarityRewards.forEach { reward ->
                reward.chance = evenChance
            }
        }
    }
}
