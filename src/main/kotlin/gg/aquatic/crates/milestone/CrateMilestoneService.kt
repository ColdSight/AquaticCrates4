package gg.aquatic.crates.milestone

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.reward.processor.RolledReward
import gg.aquatic.crates.stats.CrateStats
import gg.aquatic.crates.stats.CrateStatsTimeframe
import org.bukkit.entity.Player

object CrateMilestoneService {
    suspend fun grantReachedMilestones(player: Player, crate: Crate): List<RolledReward> {
        if (!CrateStats.ready) {
            return emptyList()
        }

        val totalOpened = CrateStats.getPlayerCrateOpens(player.uniqueId, crate.id, CrateStatsTimeframe.ALL_TIME) + 1
        val reachedMilestones = crate.milestoneManager.milestonesReached(totalOpened.toInt())
        if (reachedMilestones.isEmpty()) {
            return emptyList()
        }

        return buildList {
            for (milestone in reachedMilestones) {
                for (reward in milestone.rewards) {
                    val amount = reward.rollAmount()
                    if (reward.tryWin(player, amount)) {
                        add(RolledReward(reward, amount))
                    }
                }
            }
        }
    }
}
