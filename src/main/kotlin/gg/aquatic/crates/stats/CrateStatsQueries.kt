package gg.aquatic.crates.stats

import gg.aquatic.crates.stats.table.AllTimeCrateStatsTable
import gg.aquatic.crates.stats.table.AllTimeRewardStatsTable
import gg.aquatic.crates.stats.table.CrateOpeningRewardsTable
import gg.aquatic.crates.stats.table.CrateOpeningsTable
import gg.aquatic.crates.stats.table.HourlyCrateStatsTable
import gg.aquatic.crates.stats.table.HourlyRewardStatsTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import java.util.UUID

internal suspend fun CrateStats.getCrateOpensInternal(crateId: String, timeframe: CrateStatsTimeframe): Long {
    if (!ready) return 0L
    return dbQuery { queryCrateOpens(crateId, timeframe) }
}

internal suspend fun CrateStats.getRewardStatsInternal(crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): RewardStatsSnapshot {
    if (!ready) return RewardStatsSnapshot(0L, 0L)
    return dbQuery { queryRewardStats(crateId, rewardId, timeframe) }
}

internal suspend fun CrateStats.getPlayerCrateOpensInternal(playerUuid: UUID, crateId: String, timeframe: CrateStatsTimeframe): Long {
    if (!ready) return 0L
    return dbQuery { queryPlayerCrateOpens(playerUuid, crateId, timeframe) }
}

internal suspend fun CrateStats.getPlayerRewardWinsInternal(playerUuid: UUID, crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): Long {
    if (!ready) return 0L
    return dbQuery { queryPlayerRewardWins(playerUuid, crateId, rewardId, timeframe) }
}

internal fun CrateStats.getCrateOpensCachedInternal(crateId: String, timeframe: CrateStatsTimeframe): Long {
    if (!ready) return 0L
    val key = "crate:$crateId:${timeframe.name}"
    return getCachedValue(key) { dbQuerySync { queryCrateOpens(crateId, timeframe) } } as Long
}

internal fun CrateStats.getRewardStatsCachedInternal(crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): RewardStatsSnapshot {
    if (!ready) return RewardStatsSnapshot(0L, 0L)
    val key = "reward:$crateId:$rewardId:${timeframe.name}"
    return getCachedValue(key) { dbQuerySync { queryRewardStats(crateId, rewardId, timeframe) } } as RewardStatsSnapshot
}

internal fun CrateStats.getLatestCrateRewardsCachedInternal(crateId: String, limit: Int): List<LatestRewardSnapshot> {
    if (!ready) return emptyList()
    val safeLimit = limit.coerceIn(1, 100)
    val key = "latest:crate:$crateId:$safeLimit"
    @Suppress("UNCHECKED_CAST")
    return getCachedValue(key) { dbQuerySync { queryLatestRewards(crateId = crateId, playerUuid = null, limit = safeLimit) } } as List<LatestRewardSnapshot>
}

internal fun CrateStats.getLatestPlayerRewardsCachedInternal(playerUuid: UUID, limit: Int): List<LatestRewardSnapshot> {
    if (!ready) return emptyList()
    val safeLimit = limit.coerceIn(1, 100)
    val key = "latest:player:$playerUuid:$safeLimit"
    @Suppress("UNCHECKED_CAST")
    return getCachedValue(key) { dbQuerySync { queryLatestRewards(crateId = null, playerUuid = playerUuid, limit = safeLimit) } } as List<LatestRewardSnapshot>
}

internal fun JdbcTransaction.queryCrateOpens(crateId: String, timeframe: CrateStatsTimeframe): Long {
    return when (timeframe) {
        CrateStatsTimeframe.ALL_TIME -> {
            AllTimeCrateStatsTable
                .select(AllTimeCrateStatsTable.opens)
                .where { AllTimeCrateStatsTable.crateId eq crateId }
                .singleOrNull()
                ?.get(AllTimeCrateStatsTable.opens)
                ?: 0L
        }

        else -> exactRollingOpenCount(crateId, System.currentTimeMillis() - (timeframe.windowMillis ?: 0L))
    }
}

internal fun JdbcTransaction.queryRewardStats(crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): RewardStatsSnapshot {
    return when (timeframe) {
        CrateStatsTimeframe.ALL_TIME -> {
            AllTimeRewardStatsTable
                .select(AllTimeRewardStatsTable.wins, AllTimeRewardStatsTable.amountSum)
                .where {
                    (AllTimeRewardStatsTable.crateId eq crateId) and
                        (AllTimeRewardStatsTable.rewardId eq rewardId)
                }
                .singleOrNull()
                ?.let {
                    RewardStatsSnapshot(
                        wins = it[AllTimeRewardStatsTable.wins],
                        amountSum = it[AllTimeRewardStatsTable.amountSum]
                    )
                }
                ?: RewardStatsSnapshot(0L, 0L)
        }

        else -> exactRollingRewardStats(crateId, rewardId, System.currentTimeMillis() - (timeframe.windowMillis ?: 0L))
    }
}

internal fun JdbcTransaction.queryPlayerCrateOpens(playerUuid: UUID, crateId: String, timeframe: CrateStatsTimeframe): Long {
    val countExpr = CrateOpeningsTable.id.count()
    return CrateOpeningsTable
        .select(countExpr)
        .where {
            (CrateOpeningsTable.playerUuid eq playerUuid.toString()) and
                (CrateOpeningsTable.crateId eq crateId) and
                timeframeCondition(CrateOpeningsTable.openedAtMillis, timeframe)
        }
        .singleOrNull()
        ?.get(countExpr)
        ?: 0L
}

internal fun JdbcTransaction.queryPlayerRewardWins(playerUuid: UUID, crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): Long {
    val countExpr = CrateOpeningRewardsTable.id.count()
    return CrateOpeningRewardsTable
        .select(countExpr)
        .where {
            (CrateOpeningRewardsTable.playerUuid eq playerUuid.toString()) and
                (CrateOpeningRewardsTable.crateId eq crateId) and
                (CrateOpeningRewardsTable.rewardId eq rewardId) and
                timeframeCondition(CrateOpeningRewardsTable.wonAtMillis, timeframe)
        }
        .singleOrNull()
        ?.get(countExpr)
        ?: 0L
}

internal fun JdbcTransaction.exactRollingOpenCount(crateId: String, startMillis: Long): Long {
    val currentHour = CrateStats.truncateHour(System.currentTimeMillis())
    val firstFullHour = CrateStats.truncateHour(startMillis) + CrateStats.HOUR_MILLIS
    val rawCountExpr = CrateOpeningsTable.id.count()

    val rawCount = CrateOpeningsTable
        .select(rawCountExpr)
        .where {
            (CrateOpeningsTable.crateId eq crateId) and
                (CrateOpeningsTable.openedAtMillis greaterEq startMillis) and
                (CrateOpeningsTable.openedAtMillis less firstFullHour)
        }
        .singleOrNull()
        ?.get(rawCountExpr)
        ?: 0L

    val hourlyCount = if (firstFullHour <= currentHour) {
        val sumExpr = HourlyCrateStatsTable.opens.sum()
        HourlyCrateStatsTable
            .select(sumExpr)
            .where {
                (HourlyCrateStatsTable.crateId eq crateId) and
                    (HourlyCrateStatsTable.bucketHourMillis greaterEq firstFullHour) and
                    (HourlyCrateStatsTable.bucketHourMillis less (currentHour + CrateStats.HOUR_MILLIS))
            }
            .singleOrNull()
            ?.get(sumExpr)
            ?: 0L
    } else {
        0L
    }

    return rawCount + hourlyCount
}

internal fun JdbcTransaction.exactRollingRewardStats(crateId: String, rewardId: String, startMillis: Long): RewardStatsSnapshot {
    val currentHour = CrateStats.truncateHour(System.currentTimeMillis())
    val firstFullHour = CrateStats.truncateHour(startMillis) + CrateStats.HOUR_MILLIS

    val rawWinsExpr = CrateOpeningRewardsTable.id.count()
    val rawAmountExpr = CrateOpeningRewardsTable.amount.sum()
    val rawRow = CrateOpeningRewardsTable
        .select(rawWinsExpr, rawAmountExpr)
        .where {
            (CrateOpeningRewardsTable.crateId eq crateId) and
                (CrateOpeningRewardsTable.rewardId eq rewardId) and
                (CrateOpeningRewardsTable.wonAtMillis greaterEq startMillis) and
                (CrateOpeningRewardsTable.wonAtMillis less firstFullHour)
        }
        .singleOrNull()

    val rawStats = RewardStatsSnapshot(
        wins = rawRow?.get(rawWinsExpr) ?: 0L,
        amountSum = rawRow?.get(rawAmountExpr)?.toLong() ?: 0L
    )

    val hourlyStats = if (firstFullHour <= currentHour) {
        val winsExpr = HourlyRewardStatsTable.wins.sum()
        val amountExpr = HourlyRewardStatsTable.amountSum.sum()
        val row = HourlyRewardStatsTable
            .select(winsExpr, amountExpr)
            .where {
                (HourlyRewardStatsTable.crateId eq crateId) and
                    (HourlyRewardStatsTable.rewardId eq rewardId) and
                    (HourlyRewardStatsTable.bucketHourMillis greaterEq firstFullHour) and
                    (HourlyRewardStatsTable.bucketHourMillis less (currentHour + CrateStats.HOUR_MILLIS))
            }
            .singleOrNull()

        RewardStatsSnapshot(
            wins = row?.get(winsExpr) ?: 0L,
            amountSum = row?.get(amountExpr) ?: 0L
        )
    } else {
        RewardStatsSnapshot(0L, 0L)
    }

    return RewardStatsSnapshot(
        wins = rawStats.wins + hourlyStats.wins,
        amountSum = rawStats.amountSum + hourlyStats.amountSum
    )
}

internal fun JdbcTransaction.queryLatestRewards(crateId: String?, playerUuid: UUID?, limit: Int): List<LatestRewardSnapshot> {
    require(crateId != null || playerUuid != null) { "crateId or playerUuid must be provided." }

    val conditions = buildList<Op<Boolean>> {
        if (crateId != null) add(CrateOpeningRewardsTable.crateId eq crateId)
        if (playerUuid != null) add(CrateOpeningRewardsTable.playerUuid eq playerUuid.toString())
    }.reduce { acc, op -> acc and op }

    return CrateOpeningRewardsTable
        .select(
            CrateOpeningRewardsTable.playerUuid,
            CrateOpeningRewardsTable.crateId,
            CrateOpeningRewardsTable.rewardId,
            CrateOpeningRewardsTable.rarityId,
            CrateOpeningRewardsTable.amount,
            CrateOpeningRewardsTable.wonAtMillis
        )
        .where { conditions }
        .orderBy(
            CrateOpeningRewardsTable.wonAtMillis to SortOrder.DESC,
            CrateOpeningRewardsTable.id to SortOrder.DESC
        )
        .limit(limit)
        .map {
            LatestRewardSnapshot(
                playerUuid = UUID.fromString(it[CrateOpeningRewardsTable.playerUuid]),
                crateId = it[CrateOpeningRewardsTable.crateId],
                rewardId = it[CrateOpeningRewardsTable.rewardId],
                rarityId = it[CrateOpeningRewardsTable.rarityId],
                amount = it[CrateOpeningRewardsTable.amount],
                wonAtMillis = it[CrateOpeningRewardsTable.wonAtMillis]
            )
        }
}

internal fun timeframeCondition(column: org.jetbrains.exposed.v1.core.Column<Long>, timeframe: CrateStatsTimeframe): Op<Boolean> {
    val windowMillis = timeframe.windowMillis ?: return Op.TRUE
    return column greaterEq (System.currentTimeMillis() - windowMillis)
}
