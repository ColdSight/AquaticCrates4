package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.reward.RewardAmountRange

/**
 * Weighted range sampler that collapses overlapping integer ranges into one discrete distribution.
 *
 * When every possible integer value can be materialized up front, total amounts for large batches can be sampled with
 * one multinomial pass over the discrete values instead of drawing one amount per win.
 */
internal class WeightedRangeAmountSampler(
    private val ranges: List<RewardAmountRange>,
) : CompiledAmountSampler {
    private val discreteValues: IntArray
    private val discreteProbabilities: DoubleArray
    private val discreteSampler: AliasWeightedIndexSampler

    init {
        val weightByValue = LinkedHashMap<Int, Double>()
        for (index in ranges.indices) {
            val range = ranges[index]
            val low = minOf(range.min, range.max)
            val highExclusive = maxOf(range.min, range.max) + 1
            val rangeWidth = (highExclusive - low).coerceAtLeast(1)
            val weightPerValue = range.chance / rangeWidth.toDouble()
            for (value in low until highExclusive) {
                weightByValue.merge(value, weightPerValue, Double::plus)
            }
        }

        discreteValues = IntArray(weightByValue.size)
        val rawWeights = DoubleArray(weightByValue.size)
        var offset = 0
        weightByValue.forEach { (value, weight) ->
            discreteValues[offset] = value
            rawWeights[offset] = weight
            offset++
        }

        discreteProbabilities = AliasWeightedIndexSampler.normalizeWeights(rawWeights.toList())
        discreteSampler = AliasWeightedIndexSampler.fromNormalizedWeights(discreteProbabilities)
    }

    override fun sample(random: MassRandom): Int = discreteValues[discreteSampler.sample(random)]

    override fun fixedValueOrNull(): Int? = null

    override fun maxValue(): Int = discreteValues.maxOrNull() ?: ranges.maxOf { maxOf(it.min, it.max) }

    override fun sampleTotal(trials: Long, random: MassRandom): Long {
        if (trials <= 0L) {
            return 0L
        }

        val counts = MassSampling.sampleMultinomialCounts(trials, discreteProbabilities, random)
        var total = 0L
        for (index in counts.indices) {
            val count = counts[index]
            if (count > 0L) {
                total += count * discreteValues[index].toLong()
            }
        }
        return total
    }
}
