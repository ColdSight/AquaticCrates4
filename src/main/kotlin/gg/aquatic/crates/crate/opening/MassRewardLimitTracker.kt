package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.stats.CrateStats
import gg.aquatic.crates.stats.CrateStatsTimeframe
import org.bukkit.entity.Player

internal class MassRewardLimitTracker(
    private val counts: Map<Pair<String, CrateStatsTimeframe>, Long>,
    val hasNoLimits: Boolean,
) {
    private val pendingWins = HashMap<String, Long>()

    fun canGrant(reward: Reward): Boolean {
        if (reward.limits.isEmpty()) {
            return true
        }

        val pending = pendingWins[reward.id] ?: 0L
        return reward.limits.all { limit ->
            val current = counts[reward.id to limit.timeframe] ?: 0L
            current + pending < limit.limit.toLong()
        }
    }

    fun recordGrant(reward: Reward) {
        if (reward.limits.isNotEmpty()) {
            pendingWins.merge(reward.id, 1L, Long::plus)
        }
    }

    fun maxGrantableWins(reward: Reward): Long {
        if (hasNoLimits || reward.limits.isEmpty()) {
            return Long.MAX_VALUE
        }

        var maxWins = Long.MAX_VALUE
        for (limit in reward.limits) {
            val current = counts[reward.id to limit.timeframe] ?: 0L
            val remaining = limit.limit.toLong() - current
            maxWins = minOf(maxWins, remaining.coerceAtLeast(0L))
        }
        return maxWins
    }

    companion object {
        suspend fun create(player: Player, crateId: String, rewards: Collection<Reward>): MassRewardLimitTracker {
            if (!CrateStats.ready) {
                return MassRewardLimitTracker(emptyMap(), hasNoLimits = true)
            }

            val rewardLimits = rewards.filter { it.limits.isNotEmpty() }
            if (rewardLimits.isEmpty()) {
                return MassRewardLimitTracker(emptyMap(), hasNoLimits = true)
            }

            val counts = HashMap<Pair<String, CrateStatsTimeframe>, Long>()
            rewardLimits.forEach { reward ->
                reward.limits.forEach { limit ->
                    counts[reward.id to limit.timeframe] = CrateStats.getPlayerRewardWins(
                        player.uniqueId,
                        crateId,
                        reward.id,
                        limit.timeframe
                    )
                }
            }

            return MassRewardLimitTracker(counts, hasNoLimits = false)
        }
    }
}
