package gg.aquatic.crates.stats

import org.bukkit.configuration.ConfigurationSection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CrateStats {
    internal const val HOUR_MILLIS = 60L * 60L * 1000L
    internal const val CACHE_TTL_MILLIS = 10_000L
    internal const val HOURLY_RETENTION_DAYS = 31L

    internal var statsDatabase: CrateStatsDatabase? = null
    internal val statsCache = ConcurrentHashMap<String, CachedValue>()
    internal var configuredEnabled = false

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
}
