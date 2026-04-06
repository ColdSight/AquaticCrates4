package gg.aquatic.crates.stats.table

import org.jetbrains.exposed.v1.core.Table

object AllTimeCrateStatsTable : Table("acrates_alltime_crate_stats") {
    val crateId = varchar("crate_id", 64)
    val opens = long("opens").default(0L)

    override val primaryKey = PrimaryKey(crateId)
}