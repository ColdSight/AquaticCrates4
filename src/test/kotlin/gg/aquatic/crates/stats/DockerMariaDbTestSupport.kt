package gg.aquatic.crates.stats

import java.net.ServerSocket
import java.sql.DriverManager
import java.util.UUID

internal class DockerMariaDbTestSupport(
    private val image: String = "mariadb:11.8",
    private val database: String = "aqcrates_test",
    private val username: String = "test",
    private val password: String = "test",
) {
    private val hostPort = findFreePort()
    private val containerName = "aqcrates-test-${UUID.randomUUID().toString().substring(0, 8)}"

    val jdbcUrl: String
        get() = "jdbc:mariadb://127.0.0.1:$hostPort/$database"

    fun start() {
        runDocker(
            "run", "-d", "--rm",
            "--name", containerName,
            "-e", "MARIADB_DATABASE=$database",
            "-e", "MARIADB_USER=$username",
            "-e", "MARIADB_PASSWORD=$password",
            "-e", "MARIADB_ROOT_PASSWORD=root",
            "-p", "$hostPort:3306",
            image
        )
        waitUntilReady()
    }

    fun stop() {
        runCatching {
            runDocker("rm", "-f", containerName)
        }
    }

    private fun waitUntilReady(timeoutMillis: Long = 60_000L) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        var lastFailure: Throwable? = null

        while (System.currentTimeMillis() < deadline) {
            runCatching {
                DriverManager.getConnection(jdbcUrl, username, password).use { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute("SELECT 1")
                    }
                }
            }.onSuccess {
                return
            }.onFailure {
                lastFailure = it
                Thread.sleep(500L)
            }
        }

        error("MariaDB docker container did not become ready in time. Last failure: ${lastFailure?.message}")
    }

    private fun runDocker(vararg args: String): String {
        val process = ProcessBuilder(listOf("docker", *args))
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            error("Docker command failed (${args.joinToString(" ")}): $output")
        }
        return output.trim()
    }

    private fun findFreePort(): Int {
        return ServerSocket(0).use { it.localPort }
    }
}
