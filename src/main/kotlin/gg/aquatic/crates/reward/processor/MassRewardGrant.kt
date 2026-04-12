package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.reward.Reward

data class MassRewardGrant(
    val reward: Reward,
    val winCount: Long,
    val totalAmount: Long,
)
