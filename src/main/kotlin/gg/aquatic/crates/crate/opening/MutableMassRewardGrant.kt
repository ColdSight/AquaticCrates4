package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.processor.MassRewardGrant

internal class MutableMassRewardGrant(
    val reward: Reward,
    var winCount: Long = 0L,
    var totalAmount: Long = 0L,
) {
    fun toImmutable(): MassRewardGrant = MassRewardGrant(reward = reward, winCount = winCount, totalAmount = totalAmount)
}
