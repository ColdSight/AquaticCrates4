package gg.aquatic.crates.stats.table

import org.jetbrains.exposed.v1.core.Table

object CrateOpeningsTable : Table("acrates_openings") {
    val id = long("id").autoIncrement()
    val playerUuid = varchar("player_uuid", 36)
    val crateId = varchar("crate_id", 64)
    val openedAtMillis = long("opened_at_millis")
    val openCount = long("open_count").default(1L)

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, crateId, openedAtMillis)
        index(false, playerUuid, openedAtMillis)
        index(false, openedAtMillis)
    }
}
