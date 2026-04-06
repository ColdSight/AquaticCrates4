package gg.aquatic.crates.reward.provider

import gg.aquatic.crates.data.provider.PoolSelectionMode
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.RewardAmountRange
import org.bukkit.entity.Player

class ConditionalPoolsRewardProvider(
    private val selectionMode: PoolSelectionMode,
    private val fallbackPoolId: String?,
    private val pools: Map<String, RewardPool>,
    private val rewardCountRanges: Collection<RewardAmountRange>,
) : RewardProvider {
    override fun allRewards(): Collection<Reward> {
        return pools.values.flatMap { it.rewards }
    }

    override suspend fun resolve(player: Player): ResolvedRewardProvider {
        val matchingPools = pools.values.filter { it.matches(player) }
        val selectedPools = when {
            matchingPools.isNotEmpty() && selectionMode == PoolSelectionMode.FIRST_MATCH ->
                listOf(matchingPools.first())
            matchingPools.isNotEmpty() ->
                matchingPools
            else ->
                listOfNotNull(fallbackPoolId?.let(pools::get))
        }

        return ResolvedRewardProvider(
            rewards = selectedPools.flatMap { it.rewards },
            rewardCountRanges = rewardCountRanges
        )
    }
}
