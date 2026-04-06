package gg.aquatic.crates.stats

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.debug.CratesDebug
import gg.aquatic.crates.debug.CratesLogger
import gg.aquatic.crates.stats.table.AllTimeCrateStatsTable
import gg.aquatic.crates.stats.table.AllTimeRewardStatsTable
import gg.aquatic.crates.stats.table.CrateOpeningRewardsTable
import gg.aquatic.crates.stats.table.CrateOpeningsTable
import gg.aquatic.crates.stats.table.HourlyCrateStatsTable
import gg.aquatic.crates.stats.table.HourlyRewardStatsTable
import kotlinx.coroutines.withContext
import org.bukkit.configuration.ConfigurationSection
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.notInList
import org.jetbrains.exposed.v1.core.plus
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.upsert
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CrateStats {
    private const val HOUR_MILLIS = 60L * 60L * 1000L
    private const val CACHE_TTL_MILLIS = 10_000L
    private const val HOURLY_RETENTION_DAYS = 31L

    private var statsDatabase: CrateStatsDatabase? = null
    private val statsCache = ConcurrentHashMap<String, CachedValue>()

    val enabled: Boolean
        get() = statsDatabase != null

    fun initialize(configuration: ConfigurationSection) {
        if (statsDatabase != null) {
            CratesLogger.info("Crate stats are already initialized.")
            return
        }
        if (!configuration.getBoolean("stats.enabled", false)) {
            CratesLogger.info("Crate stats are disabled in config.")
            return
        }

        val url = configuration.getString("stats.database.url").orEmpty()
        val driver = configuration.getString("stats.database.driver").orEmpty()
        val user = configuration.getString("stats.database.user").orEmpty()
        val password = configuration.getString("stats.database.password").orEmpty()

        if (url.isBlank() || driver.isBlank()) {
            CratesLogger.warning("Crate stats were enabled, but the stats database config is incomplete.")
            return
        }

        runCatching {
            statsDatabase = CrateStatsDatabase.connect(url, driver, user, password)
            CratesLogger.info("Crate stats database connected successfully.")
        }.onFailure {
            CratesLogger.severe("Failed to initialize crate stats database: ${it.message ?: it.javaClass.simpleName}")
        }
    }

    fun shutdown() {
        statsDatabase?.close()
        statsDatabase = null
        statsCache.clear()
    }

    suspend fun logOpening(opening: LoggedOpening) {
        val database = statsDatabase ?: return
        runCatching {
            dbQuery(database) {
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

            CratesDebug.log(1, "Logged opening for crate '${opening.crateId}' with ${opening.rewards.size} rewards.")
        }.onFailure {
            CratesLogger.severe("Failed to log crate opening for '${opening.crateId}': ${it.message ?: it.javaClass.simpleName}")
        }

        statsCache.clear()
    }

    suspend fun getCrateOpens(crateId: String, timeframe: CrateStatsTimeframe): Long {
        val database = statsDatabase ?: return 0L
        return dbQuery(database) { queryCrateOpens(crateId, timeframe) }
    }

    suspend fun getRewardStats(crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): RewardStatsSnapshot {
        val database = statsDatabase ?: return RewardStatsSnapshot(0L, 0L)
        return dbQuery(database) { queryRewardStats(crateId, rewardId, timeframe) }
    }

    suspend fun getPlayerCrateOpens(playerUuid: UUID, crateId: String, timeframe: CrateStatsTimeframe): Long {
        val database = statsDatabase ?: return 0L
        return dbQuery(database) { queryPlayerCrateOpens(playerUuid, crateId, timeframe) }
    }

    suspend fun getPlayerRewardWins(playerUuid: UUID, crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): Long {
        val database = statsDatabase ?: return 0L
        return dbQuery(database) { queryPlayerRewardWins(playerUuid, crateId, rewardId, timeframe) }
    }

    suspend fun invalidate(existingCrateIds: Set<String>): CrateStatsInvalidationResult {
        val database = statsDatabase ?: return CrateStatsInvalidationResult(0, 0, 0, 0, 0, 0, 0, 0)
        val expiredBefore = truncateHour(System.currentTimeMillis() - HOURLY_RETENTION_DAYS * 24L * HOUR_MILLIS)

        return dbQuery(database) {
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

    fun getCrateOpensCached(crateId: String, timeframe: CrateStatsTimeframe): Long {
        val database = statsDatabase ?: return 0L
        val key = "crate:$crateId:${timeframe.name}"
        return getCached(key) {
            dbQuerySync(database) { queryCrateOpens(crateId, timeframe) }
        } as Long
    }

    fun getRewardStatsCached(crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): RewardStatsSnapshot {
        val database = statsDatabase ?: return RewardStatsSnapshot(0L, 0L)
        val key = "reward:$crateId:$rewardId:${timeframe.name}"
        return getCached(key) {
            dbQuerySync(database) { queryRewardStats(crateId, rewardId, timeframe) }
        } as RewardStatsSnapshot
    }

    fun getLatestCrateRewardsCached(crateId: String, limit: Int = 10): List<LatestRewardSnapshot> {
        val database = statsDatabase ?: return emptyList()
        val safeLimit = limit.coerceIn(1, 100)
        val key = "latest:crate:$crateId:$safeLimit"
        @Suppress("UNCHECKED_CAST")
        return getCached(key) {
            dbQuerySync(database) { queryLatestRewards(crateId = crateId, playerUuid = null, limit = safeLimit) }
        } as List<LatestRewardSnapshot>
    }

    fun getLatestPlayerRewardsCached(playerUuid: UUID, limit: Int = 10): List<LatestRewardSnapshot> {
        val database = statsDatabase ?: return emptyList()
        val safeLimit = limit.coerceIn(1, 100)
        val key = "latest:player:$playerUuid:$safeLimit"
        @Suppress("UNCHECKED_CAST")
        return getCached(key) {
            dbQuerySync(database) { queryLatestRewards(crateId = null, playerUuid = playerUuid, limit = safeLimit) }
        } as List<LatestRewardSnapshot>
    }

    private fun JdbcTransaction.queryCrateOpens(
        crateId: String,
        timeframe: CrateStatsTimeframe,
    ): Long {
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

    private fun JdbcTransaction.queryRewardStats(
        crateId: String,
        rewardId: String,
        timeframe: CrateStatsTimeframe,
    ): RewardStatsSnapshot {
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

    private fun queryPlayerCrateOpens(
        playerUuid: UUID,
        crateId: String,
        timeframe: CrateStatsTimeframe,
    ): Long {
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

    private fun org.jetbrains.exposed.v1.jdbc.JdbcTransaction.queryPlayerRewardWins(
        playerUuid: UUID,
        crateId: String,
        rewardId: String,
        timeframe: CrateStatsTimeframe,
    ): Long {
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

    private fun org.jetbrains.exposed.v1.jdbc.JdbcTransaction.exactRollingOpenCount(crateId: String, startMillis: Long): Long {
        val currentHour = truncateHour(System.currentTimeMillis())
        val firstFullHour = truncateHour(startMillis) + HOUR_MILLIS
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
                            (HourlyCrateStatsTable.bucketHourMillis less (currentHour + HOUR_MILLIS))
                }
                .singleOrNull()
                ?.get(sumExpr)
                ?: 0L
        } else {
            0L
        }

        return rawCount + hourlyCount
    }

    private fun org.jetbrains.exposed.v1.jdbc.JdbcTransaction.exactRollingRewardStats(
        crateId: String,
        rewardId: String,
        startMillis: Long,
    ): RewardStatsSnapshot {
        val currentHour = truncateHour(System.currentTimeMillis())
        val firstFullHour = truncateHour(startMillis) + HOUR_MILLIS

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
                            (HourlyRewardStatsTable.bucketHourMillis less (currentHour + HOUR_MILLIS))
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

    private fun incrementHourlyCrateStats(
        bucketHour: Long,
        crateId: String,
        opens: Long,
    ) {
        HourlyCrateStatsTable.upsert(onUpdate = {
            it[HourlyCrateStatsTable.opens] = HourlyCrateStatsTable.opens + opens
        }) {
            it[HourlyCrateStatsTable.bucketHourMillis] = bucketHour
            it[HourlyCrateStatsTable.crateId] = crateId
            it[HourlyCrateStatsTable.opens] = opens
        }
    }

    private fun incrementAllTimeCrateStats(crateId: String, opens: Long) {
        AllTimeCrateStatsTable.upsert(onUpdate = {
            it[AllTimeCrateStatsTable.opens] = AllTimeCrateStatsTable.opens + opens
        }) {
            it[AllTimeCrateStatsTable.crateId] = crateId
            it[AllTimeCrateStatsTable.opens] = opens
        }
    }

    private fun incrementHourlyRewardStats(
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

    private fun org.jetbrains.exposed.v1.jdbc.JdbcTransaction.incrementAllTimeRewardStats(
        crateId: String,
        rewardId: String,
        wins: Long,
        amountSum: Long,
    ) {
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

    private fun org.jetbrains.exposed.v1.jdbc.JdbcTransaction.queryLatestRewards(
        crateId: String?,
        playerUuid: UUID?,
        limit: Int,
    ): List<LatestRewardSnapshot> {
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

    private fun deleteMissingCrateRows(
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

    private suspend fun <T> dbQuery(database: CrateStatsDatabase, block: suspend JdbcTransaction.() -> T): T {
        return withContext(VirtualsCtx) {
            suspendTransaction(db = database.database) { block() }
        }
    }

    private fun <T> dbQuerySync(database: CrateStatsDatabase, block: JdbcTransaction.() -> T): T {
        return transaction(database.database) { block() }
    }

    private fun getCached(key: String, supplier: () -> Any): Any {
        val now = System.currentTimeMillis()
        val cached = statsCache[key]
        if (cached != null && cached.expiresAtMillis > now) {
            return cached.value
        }

        val value = supplier()
        statsCache[key] = CachedValue(
            expiresAtMillis = now + CACHE_TTL_MILLIS,
            value = value
        )
        return value
    }

    private fun truncateHour(timestampMillis: Long): Long {
        return timestampMillis - (timestampMillis % HOUR_MILLIS)
    }

    private fun timeframeCondition(
        column: org.jetbrains.exposed.v1.core.Column<Long>,
        timeframe: CrateStatsTimeframe,
    ): Op<Boolean> {
        val windowMillis = timeframe.windowMillis ?: return Op.TRUE
        return column greaterEq (System.currentTimeMillis() - windowMillis)
    }

    private data class CachedValue(
        val expiresAtMillis: Long,
        val value: Any,
    )
}
