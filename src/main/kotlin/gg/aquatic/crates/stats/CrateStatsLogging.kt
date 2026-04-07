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
import org.jetbrains.exposed.v1.jdbc.upsert

internal suspend fun CrateStats.logOpeningInternal(opening: LoggedOpening) {
    if (!ready) return
    updateHotCaches(opening)
    pendingOpenings += opening
    val queuedCount = pendingOpeningsCount.incrementAndGet()
    statsCache.clear()

    if (queuedCount >= WRITE_BATCH_SIZE) {
        requestFlush()
    }

    CratesDebug.log(CratesLogCategory.STATS, 1, "Queued opening for crate '${opening.crateId}' with ${opening.rewards.size} rewards.")
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

internal fun CrateStats.requestFlush() {
    writeSignalChannel?.trySend(Unit)
}

internal fun CrateStats.flushPendingOpeningsSync() {
    if (!ready) return

    val batch = ArrayList<LoggedOpening>(WRITE_BATCH_SIZE)
    while (batch.size < WRITE_BATCH_SIZE) {
        val opening = pendingOpenings.poll() ?: break
        batch += opening
    }
    if (batch.isEmpty()) return
    pendingOpeningsCount.addAndGet(-batch.size)

    runCatching {
        dbQuerySync {
            flushOpeningBatch(batch)
        }
        CratesDebug.log(CratesLogCategory.STATS, 1, "Flushed ${batch.size} queued crate openings.")
    }.onFailure {
        batch.asReversed().forEach { pendingOpenings.add(it) }
        pendingOpeningsCount.addAndGet(batch.size)
        CratesLogger.severe(CratesLogCategory.STATS, "Failed to flush queued crate openings: ${it.message ?: it.javaClass.simpleName}")
    }

    if (pendingOpeningsCount.get() > 0) {
        requestFlush()
    }
}

internal fun JdbcTransaction.flushOpeningBatch(openings: List<LoggedOpening>) {
    val insertedOpenings = CrateOpeningsTable.batchInsert(openings, shouldReturnGeneratedValues = true) { opening ->
        this[CrateOpeningsTable.playerUuid] = opening.playerUuid.toString()
        this[CrateOpeningsTable.crateId] = opening.crateId
        this[CrateOpeningsTable.openedAtMillis] = opening.openedAtMillis
    }

    if (insertedOpenings.size != openings.size) {
        error("Expected ${openings.size} inserted openings, got ${insertedOpenings.size}.")
    }

    val rewardRows = buildList {
        openings.forEachIndexed { index, opening ->
            val openingId = insertedOpenings[index][CrateOpeningsTable.id]
            opening.rewards.forEach { reward ->
                add(QueuedRewardRow(openingId, opening, reward))
            }
        }
    }

    if (rewardRows.isNotEmpty()) {
        CrateOpeningRewardsTable.batchInsert(rewardRows) { row ->
            this[CrateOpeningRewardsTable.openingId] = row.openingId
            this[CrateOpeningRewardsTable.playerUuid] = row.opening.playerUuid.toString()
            this[CrateOpeningRewardsTable.crateId] = row.opening.crateId
            this[CrateOpeningRewardsTable.rewardId] = row.reward.rewardId
            this[CrateOpeningRewardsTable.rarityId] = row.reward.rarityId
            this[CrateOpeningRewardsTable.amount] = row.reward.amount
            this[CrateOpeningRewardsTable.wonAtMillis] = row.opening.openedAtMillis
        }
    }

    val aggregation = CrateStatsBatchAggregator.aggregate(openings)

    aggregation.hourlyCrateOpens.forEach { (key, opens) ->
        incrementHourlyCrateStats(bucketHour = key.first, crateId = key.second, opens = opens)
    }
    aggregation.allTimeCrateOpens.forEach { (crateId, opens) ->
        incrementAllTimeCrateStats(crateId, opens)
    }
    aggregation.hourlyRewardStats.forEach { (key, aggregate) ->
        incrementHourlyRewardStats(
            bucketHour = key.first,
            crateId = key.second,
            rewardId = key.third,
            wins = aggregate.wins,
            amountSum = aggregate.amountSum
        )
    }
    aggregation.allTimeRewardStats.forEach { (key, aggregate) ->
        incrementAllTimeRewardStats(
            crateId = key.first,
            rewardId = key.second,
            wins = aggregate.wins,
            amountSum = aggregate.amountSum
        )
    }
}

internal fun CrateStats.updateHotCaches(opening: LoggedOpening) {
    playerAllTimeOpenCache.merge(playerCrateKey(opening.playerUuid, opening.crateId), 1L, Long::plus)
    opening.rewards.forEach { reward ->
        playerAllTimeRewardWinCache.merge(playerRewardKey(opening.playerUuid, opening.crateId, reward.rewardId), 1L, Long::plus)
    }
}

private data class QueuedRewardRow(
    val openingId: Long,
    val opening: LoggedOpening,
    val reward: LoggedRewardWin,
)
