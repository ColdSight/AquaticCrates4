package gg.aquatic.crates.reward.provider

import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.RewardAmountRange

data class ResolvedRewardProvider(
    val rewards: Collection<Reward>,
    val rewardCountRanges: Collection<RewardAmountRange>,
)
