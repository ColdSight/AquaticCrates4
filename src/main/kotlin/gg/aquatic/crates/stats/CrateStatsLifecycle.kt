package gg.aquatic.crates.stats

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.debug.CratesLogCategory
import gg.aquatic.crates.debug.CratesLogger
import kotlinx.coroutines.withContext
import org.bukkit.configuration.ConfigurationSection
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

internal fun CrateStats.initializeLifecycle(configuration: ConfigurationSection) {
    configuredEnabled = configuration.getBoolean("stats.enabled", false)
    if (statsDatabase != null) {
        CratesLogger.info(CratesLogCategory.STATS, "Crate stats are already initialized.")
        return
    }
    if (!configuredEnabled) {
        CratesLogger.info(CratesLogCategory.STATS, "Crate stats are disabled in config.")
        return
    }

    val url = configuration.getString("stats.database.url").orEmpty()
    val driver = configuration.getString("stats.database.driver").orEmpty()
    val user = configuration.getString("stats.database.user").orEmpty()
    val password = configuration.getString("stats.database.password").orEmpty()

    if (url.isBlank() || driver.isBlank()) {
        CratesLogger.warning(CratesLogCategory.STATS, "Crate stats were enabled, but the stats database config is incomplete.")
        return
    }

    runCatching {
        statsDatabase = CrateStatsDatabase.connect(url, driver, user, password)
        CratesLogger.info(CratesLogCategory.STATS, "Crate stats database connected successfully.")
    }.onFailure {
        CratesLogger.severe(CratesLogCategory.STATS, "Failed to initialize crate stats database: ${it.message ?: it.javaClass.simpleName}")
    }
}

internal fun CrateStats.shutdownLifecycle() {
    statsDatabase?.close()
    statsDatabase = null
    configuredEnabled = false
    statsCache.clear()
}

internal suspend fun <T> CrateStats.dbQuery(block: suspend JdbcTransaction.() -> T): T {
    val database = statsDatabase ?: error("Crate stats database is not available.")
    return withContext(VirtualsCtx) {
        suspendTransaction(db = database.database) { block() }
    }
}

internal fun <T> CrateStats.dbQuerySync(block: JdbcTransaction.() -> T): T {
    val database = statsDatabase ?: error("Crate stats database is not available.")
    return transaction(database.database) { block() }
}
