package gg.aquatic.crates.stats

import gg.aquatic.crates.stats.table.AllTimeCrateStatsTable
import gg.aquatic.crates.stats.table.AllTimeRewardStatsTable
import gg.aquatic.crates.stats.table.CrateOpeningRewardsTable
import gg.aquatic.crates.stats.table.CrateOpeningsTable
import gg.aquatic.crates.stats.table.HourlyCrateStatsTable
import gg.aquatic.crates.stats.table.HourlyRewardStatsTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere

internal suspend fun CrateStats.invalidateInternal(existingCrateIds: Set<String>): CrateStatsInvalidationResult {
    if (!ready) return CrateStatsInvalidationResult(0, 0, 0, 0, 0, 0, 0, 0)
    val expiredBefore = truncateHour(System.currentTimeMillis() - HOURLY_RETENTION_DAYS * 24L * HOUR_MILLIS)

    return dbQuery {
        val deletedOpeningRewards = deleteMissingCrateRows(CrateOpeningRewardsTable, existingCrateIds)
        val deletedOpenings = deleteMissingCrateRows(CrateOpeningsTable, existingCrateIds)
        val deletedHourlyCrateBuckets = deleteMissingCrateRows(HourlyCrateStatsTable, existingCrateIds)
        val deletedHourlyRewardBuckets = deleteMissingCrateRows(HourlyRewardStatsTable, existingCrateIds)
        val deletedAllTimeCrateRows = deleteMissingCrateRows(AllTimeCrateStatsTable, existingCrateIds)
        val deletedAllTimeRewardRows = deleteMissingCrateRows(AllTimeRewardStatsTable, existingCrateIds)

        val deletedExpiredHourlyCrateBuckets = HourlyCrateStatsTable.deleteWhere {
            HourlyCrateStatsTable.bucketHourMillis less expiredBefore
        }
        val deletedExpiredHourlyRewardBuckets = HourlyRewardStatsTable.deleteWhere {
            HourlyRewardStatsTable.bucketHourMillis less expiredBefore
        }

        statsCache.clear()

        CrateStatsInvalidationResult(
            deletedOpenings = deletedOpenings,
            deletedOpeningRewards = deletedOpeningRewards,
            deletedHourlyCrateBuckets = deletedHourlyCrateBuckets,
            deletedHourlyRewardBuckets = deletedHourlyRewardBuckets,
            deletedAllTimeCrateRows = deletedAllTimeCrateRows,
            deletedAllTimeRewardRows = deletedAllTimeRewardRows,
            deletedExpiredHourlyCrateBuckets = deletedExpiredHourlyCrateBuckets,
            deletedExpiredHourlyRewardBuckets = deletedExpiredHourlyRewardBuckets
        )
    }
}

internal fun CrateStats.getCachedValue(key: String, supplier: () -> Any): Any {
    val now = System.currentTimeMillis()
    val cached = statsCache[key]
    if (cached != null && cached.expiresAtMillis > now) {
        return cached.value
    }

    val value = supplier()
    statsCache[key] = CrateStats.CachedValue(now + CACHE_TTL_MILLIS, value)
    return value
}

internal fun JdbcTransaction.deleteMissingCrateRows(
    table: org.jetbrains.exposed.v1.core.Table,
    existingCrateIds: Set<String>,
): Int {
    @Suppress("UNCHECKED_CAST")
    val crateColumn = table.columns.first { it.name == "crate_id" } as org.jetbrains.exposed.v1.core.Column<String>
    return if (existingCrateIds.isEmpty()) {
        table.deleteWhere { Op.TRUE }
    } else {
        table.deleteWhere { crateColumn notInList existingCrateIds.toList() }
    }
}
