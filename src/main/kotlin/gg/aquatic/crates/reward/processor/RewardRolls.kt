package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.provider.ResolvedRewardProvider
import gg.aquatic.crates.util.randomItem
import org.bukkit.entity.Player

internal suspend fun ResolvedRewardProvider.rollRewards(
    player: Player,
    countOverride: Int? = null,
    unique: Boolean = false,
): List<RolledReward> {
    val availableRewards = rewards.filter { it.canWin(player) }
    if (availableRewards.isEmpty()) {
        return emptyList()
    }

    val targetCount = (countOverride ?: if (rewardCountRanges.isEmpty()) 1 else rewardCountRanges.randomItem().roll())
        .coerceAtLeast(1)
    val rolledRewards = ArrayList<RolledReward>(targetCount)

    if (unique) {
        val pool = availableRewards.toMutableList()
        repeat(targetCount.coerceAtMost(pool.size)) {
            if (pool.isEmpty()) return@repeat
            val picked = pool.randomItem()
            pool.remove(picked)
            rolledRewards += RolledReward(picked, picked.rollAmount())
        }
        return rolledRewards
    }

    repeat(targetCount) {
        val picked = availableRewards.randomItem()
        rolledRewards += RolledReward(picked, picked.rollAmount())
    }

    return rolledRewards
}
