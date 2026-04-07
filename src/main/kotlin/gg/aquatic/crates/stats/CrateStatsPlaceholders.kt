package gg.aquatic.crates.stats

import gg.aquatic.replace.dslPlaceholder
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.debug.CratesLogCategory
import gg.aquatic.crates.debug.CratesDebug
import gg.aquatic.crates.debug.CratesLogger
import gg.aquatic.treepapi.papiPlaceholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CrateStatsPlaceholders {
    private const val DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss"

    @Volatile
    private var timestampFormatter: DateTimeFormatter = createFormatter(DEFAULT_TIMESTAMP_FORMAT)

    @Volatile
    private var registered = false

    fun configure(configuration: ConfigurationSection) {
        val pattern = configuration.getString("stats.timestamp-format", DEFAULT_TIMESTAMP_FORMAT).orEmpty()
        timestampFormatter = runCatching { createFormatter(pattern) }
            .getOrElse {
                CratesLogger.warning(
                    CratesLogCategory.STATS,
                    "Invalid stats.timestamp-format '$pattern'. Falling back to '$DEFAULT_TIMESTAMP_FORMAT'."
                )
                createFormatter(DEFAULT_TIMESTAMP_FORMAT)
            }
    }

    fun register() {
        if (registered) {
            return
        }

        registerReplacePlaceholders()
        registerPapiPlaceholders()
        registered = true
    }

    private fun registerReplacePlaceholders() {
        dslPlaceholder<Player>("acrates", isConst = false) {
            configureStatsTree(
                crateOpensHandler = { crateId, timeframe ->
                    resolveCrateOpens(crateId, timeframe)
                },
                rewardMetricHandler = { crateId, rewardId, timeframe, metric ->
                    resolveRewardMetric(crateId, rewardId, timeframe, metric)
                },
                latestCrateHandler = { crateId, index, field ->
                    resolveLatestCrate(crateId, index, field)
                },
                latestPlayerHandler = { player, index, field ->
                    resolveLatestPlayer(player, index, field)
                },
                latestPlayerNamedHandler = { playerName, index, field ->
                    resolveLatestPlayerByName(playerName, index, field)
                }
            )
        }
    }

    private fun registerPapiPlaceholders() {
        papiPlaceholder("Aquatic", "acrates") {
            configureStatsTree(
                crateOpensHandler = { crateId, timeframe ->
                    resolveCrateOpens(crateId, timeframe)
                },
                rewardMetricHandler = { crateId, rewardId, timeframe, metric ->
                    resolveRewardMetric(crateId, rewardId, timeframe, metric)
                },
                latestCrateHandler = { crateId, index, field ->
                    resolveLatestCrate(crateId, index, field)
                },
                latestPlayerHandler = { player, index, field ->
                    resolveLatestPlayer(player, index, field)
                },
                latestPlayerNamedHandler = { playerName, index, field ->
                    resolveLatestPlayerByName(playerName, index, field)
                }
            )
        }
    }

    private fun resolveCrateOpens(crateId: String, timeframeRaw: String): String {
        val timeframe = parseTimeframe(timeframeRaw) ?: return "0"
        if (!CrateStats.ready) {
            CratesDebug.log(CratesLogCategory.STATS, 1, "Stats placeholder opens crate='$crateId' timeframe='${timeframe.name}' -> stats unavailable")
            return "0"
        }
        val result = CrateStats.getCrateOpensCached(crateId, timeframe).toString()
        CratesDebug.log(CratesLogCategory.STATS, 1, "Stats placeholder opens crate='$crateId' timeframe='${timeframe.name}' -> $result")
        return result
    }

    private fun resolveRewardMetric(
        crateId: String,
        rewardId: String,
        timeframeRaw: String,
        metricRaw: String,
    ): String {
        val timeframe = parseTimeframe(timeframeRaw) ?: return "0"
        val metric = parseRewardMetric(metricRaw) ?: return "0"
        if (!CrateStats.ready) {
            CratesDebug.log(
                CratesLogCategory.STATS,
                1,
                "Stats placeholder reward crate='$crateId' reward='$rewardId' timeframe='${timeframe.name}' metric='${metric.name}' -> stats unavailable"
            )
            return "0"
        }
        val snapshot = CrateStats.getRewardStatsCached(crateId, rewardId, timeframe)

        val result = when (metric) {
            RewardMetric.WINS -> snapshot.wins.toString()
            RewardMetric.AMOUNT -> snapshot.amountSum.toString()
        }
        CratesDebug.log(
            CratesLogCategory.STATS,
            1,
            "Stats placeholder reward crate='$crateId' reward='$rewardId' timeframe='${timeframe.name}' metric='${metric.name}' -> $result"
        )
        return result
    }

    private fun resolveLatestCrate(crateId: String, indexRaw: Int, fieldRaw: String): String {
        if (!CrateStats.ready) {
            CratesDebug.log(CratesLogCategory.STATS, 1, "Stats placeholder latest crate='$crateId' index='$indexRaw' field='$fieldRaw' -> stats unavailable")
            return ""
        }
        val latestReward = CrateStats.getLatestCrateRewardsCached(crateId).getOrNull(indexRaw - 1) ?: return ""
        val result = resolveLatestField(latestReward, fieldRaw)
        CratesDebug.log(CratesLogCategory.STATS, 1, "Stats placeholder latest crate='$crateId' index='$indexRaw' field='$fieldRaw' -> $result")
        return result
    }

    private fun resolveLatestPlayer(player: OfflinePlayer, indexRaw: Int, fieldRaw: String): String {
        if (!CrateStats.ready) {
            CratesDebug.log(CratesLogCategory.STATS, 1, "Stats placeholder latest player='${player.name ?: player.uniqueId}' index='$indexRaw' field='$fieldRaw' -> stats unavailable")
            return ""
        }
        val latestReward = CrateStats.getLatestPlayerRewardsCached(player.uniqueId).getOrNull(indexRaw - 1) ?: return ""
        val result = resolveLatestField(latestReward, fieldRaw)
        CratesDebug.log(CratesLogCategory.STATS, 1, "Stats placeholder latest player='${player.name ?: player.uniqueId}' index='$indexRaw' field='$fieldRaw' -> $result")
        return result
    }

    private fun resolveLatestPlayerByName(playerName: String, indexRaw: Int, fieldRaw: String): String {
        if (!CrateStats.ready) {
            CratesDebug.log(CratesLogCategory.STATS, 1, "Stats placeholder latest playerName='$playerName' index='$indexRaw' field='$fieldRaw' -> stats unavailable")
            return ""
        }
        val offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(playerName)
        val latestReward = CrateStats.getLatestPlayerRewardsCached(offlinePlayer.uniqueId).getOrNull(indexRaw - 1) ?: return ""
        val result = resolveLatestField(latestReward, fieldRaw)
        CratesDebug.log(CratesLogCategory.STATS, 1, "Stats placeholder latest playerName='$playerName' index='$indexRaw' field='$fieldRaw' -> $result")
        return result
    }

    private fun resolveLatestField(snapshot: LatestRewardSnapshot, fieldRaw: String): String {
        val crate = CrateHandler.crates[snapshot.crateId]
        val reward = crate?.rewardProvider?.allRewards()?.firstOrNull { it.id == snapshot.rewardId }

        return when (fieldRaw.lowercase()) {
            "crate", "crateid", "crate_id" -> snapshot.crateId
            "cratename", "crate_name" -> crate?.displayName?.let(PlainTextComponentSerializer.plainText()::serialize) ?: snapshot.crateId
            "reward", "rewardid", "reward_id" -> snapshot.rewardId
            "rewardname", "reward_name" -> reward?.displayName?.let(PlainTextComponentSerializer.plainText()::serialize) ?: snapshot.rewardId
            "rarity", "rarityid", "rarity_id" -> snapshot.rarityId.orEmpty()
            "rarityname", "rarity_name" -> reward?.rarity?.displayName?.let(PlainTextComponentSerializer.plainText()::serialize)
                ?: snapshot.rarityId.orEmpty()
            "amount" -> snapshot.amount.toString()
            "timestamp", "time" -> snapshot.wonAtMillis.toString()
            "formattedtime", "formatted_time", "date" -> timestampFormatter.format(Instant.ofEpochMilli(snapshot.wonAtMillis))
            "player", "playeruuid", "player_uuid" -> snapshot.playerUuid.toString()
            else -> ""
        }
    }

    private fun parseTimeframe(raw: String): CrateStatsTimeframe? {
        return when (raw.lowercase()) {
            "day", "daily", "24h" -> CrateStatsTimeframe.DAY
            "week", "weekly", "7d" -> CrateStatsTimeframe.WEEK
            "month", "monthly", "30d" -> CrateStatsTimeframe.MONTH
            "alltime", "all-time", "all", "total" -> CrateStatsTimeframe.ALL_TIME
            else -> null
        }
    }

    private fun parseRewardMetric(raw: String): RewardMetric? {
        return when (raw.lowercase()) {
            "wins", "count" -> RewardMetric.WINS
            "amount", "amountsum", "amount-sum", "sum" -> RewardMetric.AMOUNT
            else -> null
        }
    }

    private enum class RewardMetric {
        WINS,
        AMOUNT,
    }

    private fun createFormatter(pattern: String): DateTimeFormatter {
        return DateTimeFormatter.ofPattern(pattern.ifBlank { DEFAULT_TIMESTAMP_FORMAT })
            .withZone(ZoneId.systemDefault())
    }
}

private fun gg.aquatic.replace.PlaceholderDSLNode<Player>.configureStatsTree(
    crateOpensHandler: (String, String) -> String,
    rewardMetricHandler: (String, String, String, String) -> String,
    latestCrateHandler: (String, Int, String) -> String,
    latestPlayerHandler: (Player, Int, String) -> String,
    latestPlayerNamedHandler: (String, Int, String) -> String,
) {
    "stats" {
        "opens" {
            stringArgument("crateId") {
                stringArgument("timeframe") {
                    handle {
                        crateOpensHandler(
                            string("crateId").orEmpty(),
                            string("timeframe").orEmpty()
                        )
                    }
                }
            }
        }

        "reward" {
            stringArgument("crateId") {
                stringArgument("rewardId") {
                    stringArgument("timeframe") {
                        stringArgument("metric") {
                            handle {
                                rewardMetricHandler(
                                    string("crateId").orEmpty(),
                                    string("rewardId").orEmpty(),
                                    string("timeframe").orEmpty(),
                                    string("metric").orEmpty()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    "opens" {
        stringArgument("crateId") {
            stringArgument("timeframe") {
                handle {
                    crateOpensHandler(
                        string("crateId").orEmpty(),
                        string("timeframe").orEmpty()
                    )
                }
            }
        }
    }

    "reward" {
        stringArgument("crateId") {
            stringArgument("rewardId") {
                stringArgument("timeframe") {
                    stringArgument("metric") {
                        handle {
                            rewardMetricHandler(
                                string("crateId").orEmpty(),
                                string("rewardId").orEmpty(),
                                string("timeframe").orEmpty(),
                                string("metric").orEmpty()
                            )
                        }
                    }
                }
            }
        }
    }

    "latest" {
        "crate" {
            stringArgument("crateId") {
                intArgument("index") {
                    stringArgument("field") {
                        handle {
                            latestCrateHandler(
                                string("crateId").orEmpty(),
                                arg<Int>("index") ?: 0,
                                string("field").orEmpty()
                            )
                        }
                    }
                }
            }
        }

        "player" {
            intArgument("index") {
                stringArgument("field") {
                    handle {
                        latestPlayerHandler(
                            binder,
                            arg<Int>("index") ?: 0,
                            string("field").orEmpty()
                        )
                    }
                }
            }

            stringArgument("playerName") {
                intArgument("index") {
                    stringArgument("field") {
                        handle {
                            latestPlayerNamedHandler(
                                string("playerName").orEmpty(),
                                arg<Int>("index") ?: 0,
                                string("field").orEmpty()
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun gg.aquatic.treepapi.PlaceholderNode.configureStatsTree(
    crateOpensHandler: (String, String) -> String,
    rewardMetricHandler: (String, String, String, String) -> String,
    latestCrateHandler: (String, Int, String) -> String,
    latestPlayerHandler: (OfflinePlayer, Int, String) -> String,
    latestPlayerNamedHandler: (String, Int, String) -> String,
) {
    "stats" {
        "opens" {
            stringArgument("crateId") {
                stringArgument("timeframe") {
                    handle {
                        crateOpensHandler(
                            string("crateId").orEmpty(),
                            string("timeframe").orEmpty()
                        )
                    }
                }
            }
        }

        "reward" {
            stringArgument("crateId") {
                stringArgument("rewardId") {
                    stringArgument("timeframe") {
                        stringArgument("metric") {
                            handle {
                                rewardMetricHandler(
                                    string("crateId").orEmpty(),
                                    string("rewardId").orEmpty(),
                                    string("timeframe").orEmpty(),
                                    string("metric").orEmpty()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    "opens" {
        stringArgument("crateId") {
            stringArgument("timeframe") {
                handle {
                    crateOpensHandler(
                        string("crateId").orEmpty(),
                        string("timeframe").orEmpty()
                    )
                }
            }
        }
    }

    "reward" {
        stringArgument("crateId") {
            stringArgument("rewardId") {
                stringArgument("timeframe") {
                    stringArgument("metric") {
                        handle {
                            rewardMetricHandler(
                                string("crateId").orEmpty(),
                                string("rewardId").orEmpty(),
                                string("timeframe").orEmpty(),
                                string("metric").orEmpty()
                            )
                        }
                    }
                }
            }
        }
    }

    "latest" {
        "crate" {
            stringArgument("crateId") {
                intArgument("index") {
                    stringArgument("field") {
                        handle {
                            latestCrateHandler(
                                string("crateId").orEmpty(),
                                getOrNull<Int>("index") ?: 0,
                                string("field").orEmpty()
                            )
                        }
                    }
                }
            }
        }

        "player" {
            intArgument("index") {
                stringArgument("field") {
                    handle {
                        latestPlayerHandler(
                            binder,
                            getOrNull<Int>("index") ?: 0,
                            string("field").orEmpty()
                        )
                    }
                }
            }

            stringArgument("playerName") {
                intArgument("index") {
                    stringArgument("field") {
                        handle {
                            latestPlayerNamedHandler(
                                string("playerName").orEmpty(),
                                getOrNull<Int>("index") ?: 0,
                                string("field").orEmpty()
                            )
                        }
                    }
                }
            }
        }
    }
}
