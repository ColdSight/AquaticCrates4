package gg.aquatic.crates.limit

import gg.aquatic.crates.stats.CrateStats
import org.bukkit.entity.Player

object LimitService {
    suspend fun canOpenCrate(player: Player, crateId: String, limits: Collection<LimitHandle>): Boolean {
        if (limits.isEmpty()) return true

        for (limit in limits) {
            val opens = CrateStats.getPlayerCrateOpens(player.uniqueId, crateId, limit.timeframe)
            if (opens >= limit.limit) {
                return false
            }
        }

        return true
    }

    suspend fun canWinReward(
        player: Player,
        crateId: String,
        rewardId: String,
        limits: Collection<LimitHandle>,
    ): Boolean {
        if (limits.isEmpty()) return true

        for (limit in limits) {
            val wins = CrateStats.getPlayerRewardWins(player.uniqueId, crateId, rewardId, limit.timeframe)
            if (wins >= limit.limit) {
                return false
            }
        }

        return true
    }
}
