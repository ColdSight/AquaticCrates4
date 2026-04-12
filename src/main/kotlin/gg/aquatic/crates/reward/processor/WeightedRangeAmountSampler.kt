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
    private val sampler = AliasWeightedIndexSampler.fromWeights(ranges.map { it.chance })
    private val minBounds = IntArray(ranges.size) { index -> minOf(ranges[index].min, ranges[index].max) }
    private val maxBoundsExclusive = IntArray(ranges.size) { index -> maxOf(ranges[index].min, ranges[index].max) + 1 }
    private val discreteValues: IntArray
    private val discreteProbabilities: DoubleArray
    private val discreteSampler: AliasWeightedIndexSampler

    init {
        val weightByValue = LinkedHashMap<Int, Double>()
        for (index in ranges.indices) {
            val low = minBounds[index]
            val highExclusive = maxBoundsExclusive[index]
            val rangeWidth = (highExclusive - low).coerceAtLeast(1)
            val weightPerValue = ranges[index].chance / rangeWidth.toDouble()
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

    override fun sample(random: MassRandom): Int {
        if (discreteValues.isNotEmpty()) {
            return discreteValues[discreteSampler.sample(random)]
        }

        val rangeIndex = sampler.sample(random)
        val low = minBounds[rangeIndex]
        val highExclusive = maxBoundsExclusive[rangeIndex]
        return if (low + 1 >= highExclusive) low else random.nextInt(low, highExclusive)
    }

    override fun fixedValueOrNull(): Int? = null

    override fun maxValue(): Int = discreteValues.maxOrNull() ?: ranges.maxOf { maxOf(it.min, it.max) }

    override fun sampleTotal(trials: Long, random: MassRandom): Long {
        if (trials <= 0L) {
            return 0L
        }

        if (discreteValues.isNotEmpty()) {
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

        var total = 0L
        var remaining = trials
        while (remaining > 0L) {
            total += sample(random).toLong()
            remaining--
        }
        return total
    }
}
