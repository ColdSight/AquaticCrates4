package gg.aquatic.crates.stats.table

import org.jetbrains.exposed.v1.core.Table

object AllTimeRewardStatsTable : Table("acrates_alltime_reward_stats") {
    val crateId = varchar("crate_id", 64)
    val rewardId = varchar("reward_id", 64)
    val wins = long("wins").default(0L)
    val amountSum = long("amount_sum").default(0L)

    override val primaryKey = PrimaryKey(crateId, rewardId)

    init {
        index(false, rewardId)
    }
}