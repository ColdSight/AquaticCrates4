package gg.aquatic.crates.stats

import java.util.UUID

data class LatestRewardSnapshot(
    val playerUuid: UUID,
    val crateId: String,
    val rewardId: String,
    val rarityId: String?,
    val amount: Int,
    val wonAtMillis: Long,
)