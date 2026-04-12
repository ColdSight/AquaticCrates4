package gg.aquatic.crates.limit

import gg.aquatic.crates.stats.CrateStats
import org.bukkit.entity.Player

object LimitService {
    suspend fun canOpenCrate(player: Player, crateId: String, limits: Collection<LimitHandle>): Boolean {
        if (limits.isEmpty() || !CrateStats.ready) {
            return true
        }
        return LimitEvaluation.canPass(limits) { limit ->
            CrateStats.getPlayerCrateOpens(player.uniqueId, crateId, limit.timeframe)
        }
    }

    suspend fun canWinReward(
        player: Player,
        crateId: String,
        rewardId: String,
        limits: Collection<LimitHandle>,
    ): Boolean {
        if (limits.isEmpty() || !CrateStats.ready) {
            return true
        }
        return LimitEvaluation.canPass(limits) { limit ->
            CrateStats.getPlayerRewardWins(player.uniqueId, crateId, rewardId, limit.timeframe)
        }
    }
}
