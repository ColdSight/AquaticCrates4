package gg.aquatic.crates.stats

import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.HikariConfig
import gg.aquatic.crates.stats.table.AllTimeCrateStatsTable
import gg.aquatic.crates.stats.table.AllTimeRewardStatsTable
import gg.aquatic.crates.stats.table.CrateOpeningRewardsTable
import gg.aquatic.crates.stats.table.CrateOpeningsTable
import gg.aquatic.crates.stats.table.HourlyCrateStatsTable
import gg.aquatic.crates.stats.table.HourlyRewardStatsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CrateStatsDatabase private constructor(
    val database: Database,
    val dataSource: HikariDataSource,
) {
    fun close() {
        dataSource.close()
    }

    companion object {
        fun connect(url: String, driver: String, user: String, password: String): CrateStatsDatabase {
            val dataSource = HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = url
                    driverClassName = driver
                    username = user
                    this.password = password

                    maximumPoolSize = 10
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_REPEATABLE_READ"

                    connectionTimeout = 3000
                    idleTimeout = 600000
                    maxLifetime = 1800000

                    addDataSourceProperty("cachePrepStmts", "true")
                    addDataSourceProperty("prepStmtCacheSize", "250")
                    addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                }
            )
            val database = Database.connect(dataSource)

            transaction(database) {
                SchemaUtils.create(
                    CrateOpeningsTable,
                    CrateOpeningRewardsTable,
                    HourlyCrateStatsTable,
                    HourlyRewardStatsTable,
                    AllTimeCrateStatsTable,
                    AllTimeRewardStatsTable,
                )
            }

            return CrateStatsDatabase(database, dataSource)
        }
    }
}
