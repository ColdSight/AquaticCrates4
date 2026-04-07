package gg.aquatic.crates.stats

import java.nio.file.Files
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CrateStatsSqliteIntegrationTest : CrateStatsIntegrationContract() {
    private val tempDbFile = Files.createTempFile("aqcrates-stats-", ".db").toFile().apply {
        deleteOnExit()
    }

    @AfterAll
    fun cleanupSqliteFile() {
        CrateStats.shutdown()
        tempDbFile.delete()
    }

    override fun createDatabase(): CrateStatsDatabase {
        return CrateStatsDatabase.connect(
            url = "jdbc:sqlite:${tempDbFile.absolutePath.replace('\\', '/')}",
            driver = "org.sqlite.JDBC",
            user = "",
            password = ""
        )
    }
}
