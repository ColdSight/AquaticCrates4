package gg.aquatic.crates.stats

import gg.aquatic.crates.stats.table.CrateOpeningRewardsTable
import java.sql.Connection
import java.sql.ResultSet

internal object StatsMigrations {
    private const val TABLE = "acrates_schema_migrations"

    private val migrations = listOf(
        StatsMigration(
            version = 1,
            description = "Add win_count to opening rewards",
        ) { connection ->
            ensureColumn(
                connection = connection,
                table = CrateOpeningRewardsTable.tableName,
                column = "win_count",
                definition = "BIGINT NOT NULL DEFAULT 1",
                backfillSql = "UPDATE ${CrateOpeningRewardsTable.tableName} SET win_count = 1 WHERE win_count IS NULL"
            )
        },
        StatsMigration(
            version = 2,
            description = "Add open_count to openings",
        ) { connection ->
            ensureColumn(
                connection = connection,
                table = "acrates_openings",
                column = "open_count",
                definition = "BIGINT NOT NULL DEFAULT 1",
                backfillSql = "UPDATE acrates_openings SET open_count = 1 WHERE open_count IS NULL"
            )
        }
    )

    fun runAll(connection: Connection) {
        ensureMigrationTable(connection)
        val appliedVersions = loadAppliedVersions(connection)

        for (migration in migrations.sortedBy { it.version }) {
            if (migration.version in appliedVersions) {
                continue
            }

            try {
                migration.apply(connection)
                recordAppliedMigration(connection, migration)
                connection.commit()
            } catch (throwable: Throwable) {
                runCatching { connection.rollback() }
                throw throwable
            }
        }
    }

    private fun ensureMigrationTable(connection: Connection) {
        executeUpdate(
            connection,
            """
            CREATE TABLE IF NOT EXISTS $TABLE (
                version INTEGER PRIMARY KEY,
                description VARCHAR(255) NOT NULL,
                applied_at_millis BIGINT NOT NULL
            )
            """.trimIndent()
        )
        connection.commit()
    }

    private fun loadAppliedVersions(connection: Connection): Set<Int> {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT version FROM $TABLE").use { resultSet ->
                return buildSet {
                    while (resultSet.next()) {
                        add(resultSet.getInt("version"))
                    }
                }
            }
        }
    }

    private fun recordAppliedMigration(connection: Connection, migration: StatsMigration) {
        connection.prepareStatement(
            "INSERT INTO $TABLE (version, description, applied_at_millis) VALUES (?, ?, ?)"
        ).use { statement ->
            statement.setInt(1, migration.version)
            statement.setString(2, migration.description)
            statement.setLong(3, System.currentTimeMillis())
            statement.executeUpdate()
        }
    }
}

internal data class StatsMigration(
    val version: Int,
    val description: String,
    val apply: (Connection) -> Unit,
)

internal fun ensureColumn(
    connection: Connection,
    table: String,
    column: String,
    definition: String,
    backfillSql: String? = null,
) {
    if (columnExists(connection, table, column)) {
        backfillSql?.let { sql -> executeUpdate(connection, sql) }
        return
    }

    executeUpdate(connection, "ALTER TABLE $table ADD COLUMN $column $definition")
    backfillSql?.let { sql -> executeUpdate(connection, sql) }
}

internal fun columnExists(connection: Connection, table: String, column: String): Boolean {
    val metadata = connection.metaData
    return sequenceOf(
        metadata.getColumns(null, null, table, column),
        metadata.getColumns(null, null, table.uppercase(), column.uppercase())
    ).any { resultSet ->
        resultSet.use(ResultSet::next)
    }
}

internal fun executeUpdate(connection: Connection, sql: String) {
    connection.createStatement().use { statement ->
        statement.executeUpdate(sql)
    }
}
