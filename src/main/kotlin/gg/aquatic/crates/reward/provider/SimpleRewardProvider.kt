package gg.aquatic.crates.reward.provider

import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.RewardAmountRange
import org.bukkit.entity.Player

class SimpleRewardProvider(
    private val rewards: Collection<Reward>,
    private val rewardCountRanges: Collection<RewardAmountRange>,
) : RewardProvider {
    override fun allRewards(): Collection<Reward> = rewards

    override suspend fun resolve(player: Player): ResolvedRewardProvider {
        return ResolvedRewardProvider(
            rewards = rewards,
            rewardCountRanges = rewardCountRanges
        )
    }
}
