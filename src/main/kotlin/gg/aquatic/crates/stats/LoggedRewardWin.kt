package gg.aquatic.crates.stats

import gg.aquatic.crates.reward.processor.RolledReward

data class LoggedRewardWin(
    val rewardId: String,
    val rarityId: String?,
    val amount: Int,
) {
    companion object {
        fun from(rolledReward: RolledReward): LoggedRewardWin {
            return LoggedRewardWin(
                rewardId = rolledReward.reward.id,
                rarityId = rolledReward.reward.rarity.id,
                amount = rolledReward.amount
            )
        }
    }
}