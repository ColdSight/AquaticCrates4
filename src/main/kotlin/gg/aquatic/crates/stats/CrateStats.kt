package gg.aquatic.crates.stats

import org.bukkit.configuration.ConfigurationSection
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel

object CrateStats {
    internal const val HOUR_MILLIS = 60L * 60L * 1000L
    internal const val CACHE_TTL_MILLIS = 10_000L
    internal const val HOURLY_RETENTION_DAYS = 31L
    internal const val WRITE_BATCH_SIZE = 250
    internal const val WRITE_FLUSH_INTERVAL_MILLIS = 1_000L

    internal var statsDatabase: CrateStatsDatabase? = null
    internal val statsCache = ConcurrentHashMap<String, CachedValue>()
    internal val pendingOpenings = ConcurrentLinkedQueue<LoggedOpening>()
    internal val pendingOpeningsCount = AtomicInteger(0)
    internal val playerAllTimeOpenCache = ConcurrentHashMap<String, Long>()
    internal val playerAllTimeRewardWinCache = ConcurrentHashMap<String, Long>()
    internal var configuredEnabled = false
    internal var writeScope: CoroutineScope? = null
    internal var writeLoopJob: Job? = null
    internal var writeSignalChannel: Channel<Unit>? = null

    val ready: Boolean
        get() = statsDatabase != null
    val configured: Boolean
        get() = configuredEnabled

    fun initialize(configuration: ConfigurationSection) = initializeLifecycle(configuration)

    fun shutdown() = shutdownLifecycle()

    suspend fun logOpening(opening: LoggedOpening) = logOpeningInternal(opening)

    suspend fun getCrateOpens(crateId: String, timeframe: CrateStatsTimeframe): Long =
        getCrateOpensInternal(crateId, timeframe)

    suspend fun getRewardStats(crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): RewardStatsSnapshot =
        getRewardStatsInternal(crateId, rewardId, timeframe)

    suspend fun getPlayerCrateOpens(playerUuid: UUID, crateId: String, timeframe: CrateStatsTimeframe): Long =
        getPlayerCrateOpensInternal(playerUuid, crateId, timeframe)

    suspend fun getPlayerRewardWins(playerUuid: UUID, crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): Long =
        getPlayerRewardWinsInternal(playerUuid, crateId, rewardId, timeframe)

    suspend fun invalidate(existingCrateIds: Set<String>): CrateStatsInvalidationResult =
        invalidateInternal(existingCrateIds)

    fun getCrateOpensCached(crateId: String, timeframe: CrateStatsTimeframe): Long =
        getCrateOpensCachedInternal(crateId, timeframe)

    fun getRewardStatsCached(crateId: String, rewardId: String, timeframe: CrateStatsTimeframe): RewardStatsSnapshot =
        getRewardStatsCachedInternal(crateId, rewardId, timeframe)

    fun getLatestCrateRewardsCached(crateId: String, limit: Int = 10): List<LatestRewardSnapshot> =
        getLatestCrateRewardsCachedInternal(crateId, limit)

    fun getLatestPlayerRewardsCached(playerUuid: UUID, limit: Int = 10): List<LatestRewardSnapshot> =
        getLatestPlayerRewardsCachedInternal(playerUuid, limit)

    internal fun truncateHour(timestampMillis: Long): Long {
        return timestampMillis - (timestampMillis % HOUR_MILLIS)
    }

    internal data class CachedValue(
        val expiresAtMillis: Long,
        val value: Any,
    )

    internal fun playerCrateKey(playerUuid: UUID, crateId: String): String = "$playerUuid:$crateId"
    internal fun playerRewardKey(playerUuid: UUID, crateId: String, rewardId: String): String = "$playerUuid:$crateId:$rewardId"
}
