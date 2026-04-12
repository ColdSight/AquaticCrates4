package gg.aquatic.crates.stats.table

import org.jetbrains.exposed.v1.core.Table

object CrateOpeningRewardsTable : Table("acrates_opening_rewards") {
    val id = long("id").autoIncrement()
    val openingId = long("opening_id")
    val playerUuid = varchar("player_uuid", 36)
    val crateId = varchar("crate_id", 64)
    val rewardId = varchar("reward_id", 64)
    val rarityId = varchar("rarity_id", 64).nullable()
    val amount = integer("amount")
    val winCount = long("win_count").default(1L)
    val wonAtMillis = long("won_at_millis")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, openingId)
        index(false, crateId, rewardId, wonAtMillis)
        index(false, rewardId, wonAtMillis)
        index(false, playerUuid, rewardId, wonAtMillis)
        index(false, wonAtMillis)
    }
}
