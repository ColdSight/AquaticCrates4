package gg.aquatic.crates.stats

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CrateStatsMariaDbIntegrationTest : CrateStatsIntegrationContract() {
    private val mariadb = DockerMariaDbTestSupport()

    @BeforeAll
    fun startContainer() {
        mariadb.start()
    }

    @AfterAll
    fun stopContainer() {
        CrateStats.shutdown()
        mariadb.stop()
    }

    override fun createDatabase(): CrateStatsDatabase {
        return CrateStatsDatabase.connect(
            url = mariadb.jdbcUrl,
            driver = "org.mariadb.jdbc.Driver",
            user = "test",
            password = "test"
        )
    }
}
