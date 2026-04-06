package gg.aquatic.crates.stats.table

import org.jetbrains.exposed.v1.core.Table

object HourlyCrateStatsTable : Table("acrates_hourly_crate_stats") {
    val bucketHourMillis = long("bucket_hour_millis")
    val crateId = varchar("crate_id", 64)
    val opens = long("opens").default(0L)

    override val primaryKey = PrimaryKey(bucketHourMillis, crateId)

    init {
        index(false, crateId, bucketHourMillis)
    }
}