package gg.aquatic.crates.stats.table

import org.jetbrains.exposed.v1.core.Table

object HourlyRewardStatsTable : Table("acrates_hourly_reward_stats") {
    val bucketHourMillis = long("bucket_hour_millis")
    val crateId = varchar("crate_id", 64)
    val rewardId = varchar("reward_id", 64)
    val wins = long("wins").default(0L)
    val amountSum = long("amount_sum").default(0L)

    override val primaryKey = PrimaryKey(bucketHourMillis, crateId, rewardId)

    init {
        index(false, crateId, rewardId, bucketHourMillis)
        index(false, rewardId, bucketHourMillis)
    }
}