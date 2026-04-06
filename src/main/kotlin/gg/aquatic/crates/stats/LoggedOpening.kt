package gg.aquatic.crates.stats

import java.util.UUID

data class LoggedOpening(
    val playerUuid: UUID,
    val crateId: String,
    val openedAtMillis: Long,
    val rewards: List<LoggedRewardWin>,
)
