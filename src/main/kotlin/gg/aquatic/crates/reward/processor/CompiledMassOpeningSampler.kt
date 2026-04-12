package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.RewardAmountRange
import gg.aquatic.crates.reward.provider.ResolvedRewardProvider

/**
 * Precompiled reward sampler used by the mass-opening pipeline.
 *
 * The goal of this layer is to move all reward-count, reward-index and reward-amount preprocessing out of the hot
 * path so the opening service can switch between deterministic, multinomial, vectorized and exact strategies without
 * rebuilding weighted tables for every batch.
 */
internal class CompiledMassOpeningSampler private constructor(
    val rewards: List<Reward>,
    val normalizedRewardProbabilities: DoubleArray,
    private val rewardIndexSampler: AliasWeightedIndexSampler,
    private val rewardCountSampler: CompiledAmountSampler,
    private val rewardAmountSamplers: List<CompiledAmountSampler>,
) {
    val fixedRewardCount: Int? = rewardCountSampler.fixedValueOrNull()
    val maxRewardCount: Int = rewardCountSampler.maxValue()
    val hasRandomRewardCount: Boolean = fixedRewardCount == null
    val hasRandomRewardAmounts: Boolean = rewardAmountSamplers.any { it.fixedValueOrNull() == null }
    private val fixedRewardAmounts: IntArray? = buildFixedRewardAmounts()

    fun sampleRewardIndex(random: MassRandom): Int = rewardIndexSampler.sample(random)

    fun sampleRewardCount(random: MassRandom): Int = rewardCountSampler.sample(random)

    fun sampleTotalRewardDraws(openCount: Long, random: MassRandom): Long = rewardCountSampler.sampleTotal(openCount, random)

    fun sampleRewardAmount(rewardIndex: Int, random: MassRandom): Int {
        return rewardAmountSamplers[rewardIndex].sample(random)
    }

    fun sampleRewardAmountTotal(rewardIndex: Int, winCount: Long, random: MassRandom): Long {
        return rewardAmountSamplers[rewardIndex].sampleTotal(winCount, random)
    }

    fun fixedRewardAmount(rewardIndex: Int): Int? = rewardAmountSamplers[rewardIndex].fixedValueOrNull()

    fun allFixedRewardAmounts(): IntArray? {
        return fixedRewardAmounts
    }

    private fun buildFixedRewardAmounts(): IntArray? {
        val fixed = IntArray(rewardAmountSamplers.size)
        for (index in rewardAmountSamplers.indices) {
            val value = rewardAmountSamplers[index].fixedValueOrNull() ?: return null
            fixed[index] = value
        }
        return fixed
    }

    companion object {
        fun compile(provider: ResolvedRewardProvider, rewards: List<Reward>): CompiledMassOpeningSampler {
            return compile(provider.rewardCountRanges, rewards)
        }

        fun compile(rewardCountRanges: Collection<RewardAmountRange>, rewards: List<Reward>): CompiledMassOpeningSampler {
            require(rewards.isNotEmpty()) { "Cannot compile mass opening sampler without rewards." }
            val normalized = AliasWeightedIndexSampler.normalizeWeights(rewards.map { it.chance })

            return CompiledMassOpeningSampler(
                rewards = rewards,
                normalizedRewardProbabilities = normalized,
                rewardIndexSampler = AliasWeightedIndexSampler.fromNormalizedWeights(normalized),
                rewardCountSampler = CompiledAmountSampler.fromRanges(rewardCountRanges),
                rewardAmountSamplers = rewards.map { CompiledAmountSampler.fromRanges(it.amountRanges) }
            )
        }
    }
}
