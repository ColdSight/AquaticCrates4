package gg.aquatic.crates.reward.provider

import gg.aquatic.crates.reward.Reward
import org.bukkit.entity.Player

interface RewardProvider {
    suspend fun resolve(player: Player): ResolvedRewardProvider

    suspend fun getRewards(player: Player): Collection<Reward> = resolve(player).rewards
}
