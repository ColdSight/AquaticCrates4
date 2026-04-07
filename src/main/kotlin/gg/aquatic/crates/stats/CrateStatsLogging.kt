package gg.aquatic.crates.stats

import gg.aquatic.crates.debug.CratesDebug
import gg.aquatic.crates.debug.CratesLogCategory
import gg.aquatic.crates.debug.CratesLogger
import gg.aquatic.crates.stats.table.AllTimeCrateStatsTable
import gg.aquatic.crates.stats.table.AllTimeRewardStatsTable
import gg.aquatic.crates.stats.table.CrateOpeningRewardsTable
import gg.aquatic.crates.stats.table.CrateOpeningsTable
import gg.aquatic.crates.stats.table.HourlyCrateStatsTable
import gg.aquatic.crates.stats.table.HourlyRewardStatsTable
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.upsert

internal suspend fun CrateStats.logOpeningInternal(opening: LoggedOpening) {
    if (!ready) return
    runCatching {
        dbQuery {
            val openingId = CrateOpeningsTable.insert {
                it[playerUuid] = opening.playerUuid.toString()
                it[crateId] = opening.crateId
                it[openedAtMillis] = opening.openedAtMillis
            }[CrateOpeningsTable.id]

            if (opening.rewards.isNotEmpty()) {
                CrateOpeningRewardsTable.batchInsert(opening.rewards) { reward ->
                    this[CrateOpeningRewardsTable.openingId] = openingId
                    this[CrateOpeningRewardsTable.playerUuid] = opening.playerUuid.toString()
                    this[CrateOpeningRewardsTable.crateId] = opening.crateId
                    this[CrateOpeningRewardsTable.rewardId] = reward.rewardId
                    this[CrateOpeningRewardsTable.rarityId] = reward.rarityId
                    this[CrateOpeningRewardsTable.amount] = reward.amount
                    this[CrateOpeningRewardsTable.wonAtMillis] = opening.openedAtMillis
                }
            }

            val bucketHour = truncateHour(opening.openedAtMillis)
            incrementHourlyCrateStats(bucketHour, opening.crateId, 1L)
            incrementAllTimeCrateStats(opening.crateId, 1L)

            opening.rewards.forEach { reward ->
                incrementHourlyRewardStats(
                    bucketHour = bucketHour,
                    crateId = opening.crateId,
                    rewardId = reward.rewardId,
                    wins = 1L,
                    amountSum = reward.amount.toLong()
                )
                incrementAllTimeRewardStats(
                    crateId = opening.crateId,
                    rewardId = reward.rewardId,
                    wins = 1L,
                    amountSum = reward.amount.toLong()
                )
            }
        }

        CratesDebug.log(CratesLogCategory.STATS, 1, "Logged opening for crate '${opening.crateId}' with ${opening.rewards.size} rewards.")
    }.onFailure {
        CratesLogger.severe(CratesLogCategory.STATS, "Failed to log crate opening for '${opening.crateId}': ${it.message ?: it.javaClass.simpleName}")
    }

    statsCache.clear()
}

internal fun JdbcTransaction.incrementHourlyCrateStats(bucketHour: Long, crateId: String, opens: Long) {
    HourlyCrateStatsTable.upsert(onUpdate = {
        it[HourlyCrateStatsTable.opens] = HourlyCrateStatsTable.opens + opens
    }) {
        it[HourlyCrateStatsTable.bucketHourMillis] = bucketHour
        it[HourlyCrateStatsTable.crateId] = crateId
        it[HourlyCrateStatsTable.opens] = opens
    }
}

internal fun JdbcTransaction.incrementAllTimeCrateStats(crateId: String, opens: Long) {
    AllTimeCrateStatsTable.upsert(onUpdate = {
        it[AllTimeCrateStatsTable.opens] = AllTimeCrateStatsTable.opens + opens
    }) {
        it[AllTimeCrateStatsTable.crateId] = crateId
        it[AllTimeCrateStatsTable.opens] = opens
    }
}

internal fun JdbcTransaction.incrementHourlyRewardStats(
    bucketHour: Long,
    crateId: String,
    rewardId: String,
    wins: Long,
    amountSum: Long,
) {
    HourlyRewardStatsTable.upsert(onUpdate = {
        it[HourlyRewardStatsTable.wins] = HourlyRewardStatsTable.wins + wins
        it[HourlyRewardStatsTable.amountSum] = HourlyRewardStatsTable.amountSum + amountSum
    }) {
        it[HourlyRewardStatsTable.bucketHourMillis] = bucketHour
        it[HourlyRewardStatsTable.crateId] = crateId
        it[HourlyRewardStatsTable.rewardId] = rewardId
        it[HourlyRewardStatsTable.wins] = wins
        it[HourlyRewardStatsTable.amountSum] = amountSum
    }
}

internal fun JdbcTransaction.incrementAllTimeRewardStats(crateId: String, rewardId: String, wins: Long, amountSum: Long) {
    AllTimeRewardStatsTable.upsert(onUpdate = {
        it[AllTimeRewardStatsTable.wins] = AllTimeRewardStatsTable.wins + wins
        it[AllTimeRewardStatsTable.amountSum] = AllTimeRewardStatsTable.amountSum + amountSum
    }) {
        it[AllTimeRewardStatsTable.crateId] = crateId
        it[AllTimeRewardStatsTable.rewardId] = rewardId
        it[AllTimeRewardStatsTable.wins] = wins
        it[AllTimeRewardStatsTable.amountSum] = amountSum
    }
}
