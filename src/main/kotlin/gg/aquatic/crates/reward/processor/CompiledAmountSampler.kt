package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.reward.RewardAmountRange

/**
 * Compiled sampler for "how many rewards?" or "what amount?" style ranges.
 *
 * Fixed ranges collapse to [FixedAmountSampler]. Non-trivial weighted ranges use
 * [WeightedRangeAmountSampler], which can either sample one value or aggregate many trials at once.
 */
internal sealed interface CompiledAmountSampler {
    fun sample(random: MassRandom): Int

    fun fixedValueOrNull(): Int?

    fun maxValue(): Int

    fun sampleTotal(trials: Long, random: MassRandom): Long

    companion object {
        fun fromRanges(ranges: Collection<RewardAmountRange>): CompiledAmountSampler {
            if (ranges.isEmpty()) {
                return FixedAmountSampler(1)
            }
            if (ranges.size == 1) {
                val range = ranges.first()
                if (range.min == range.max) {
                    return FixedAmountSampler(range.min)
                }
            }
            return WeightedRangeAmountSampler(ranges.toList())
        }
    }
}
