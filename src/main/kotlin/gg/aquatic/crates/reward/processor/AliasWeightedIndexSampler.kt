package gg.aquatic.crates.reward.processor

/**
 * O(1) discrete sampler built with an alias table.
 *
 * We pay the preprocessing cost once when the reward provider is compiled, then each reward draw is only:
 * 1. pick one column uniformly,
 * 2. accept the primary value or jump to its alias.
 *
 * This is the classic alias-method family described in:
 * https://www.keithschwarz.com/darts-dice-coins/
 */
internal class AliasWeightedIndexSampler private constructor(
    private val probability: DoubleArray,
    private val alias: IntArray,
) {
    fun sample(random: MassRandom): Int {
        val column = random.nextInt(probability.size)
        return if (random.nextDouble() < probability[column]) column else alias[column]
    }

    companion object {
        fun fromWeights(weights: List<Double>): AliasWeightedIndexSampler {
            return fromNormalizedWeights(normalizeWeights(weights))
        }

        fun fromNormalizedWeights(normalized: DoubleArray): AliasWeightedIndexSampler {
            require(normalized.isNotEmpty()) { "Cannot build weighted sampler from empty weights." }
            if (normalized.size == 1) {
                return AliasWeightedIndexSampler(doubleArrayOf(1.0), intArrayOf(0))
            }

            val size = normalized.size
            val scaled = DoubleArray(size) { normalized[it] * size }
            val probability = DoubleArray(size)
            val alias = IntArray(size)
            val small = IntArray(size)
            val large = IntArray(size)
            var smallSize = 0
            var largeSize = 0

            for (index in 0 until size) {
                if (scaled[index] < 1.0) {
                    small[smallSize++] = index
                } else {
                    large[largeSize++] = index
                }
            }

            while (smallSize > 0 && largeSize > 0) {
                val less = small[--smallSize]
                val more = large[--largeSize]
                probability[less] = scaled[less]
                alias[less] = more
                scaled[more] = (scaled[more] + scaled[less]) - 1.0
                if (scaled[more] < 1.0) {
                    small[smallSize++] = more
                } else {
                    large[largeSize++] = more
                }
            }

            while (largeSize > 0) {
                probability[large[--largeSize]] = 1.0
            }
            while (smallSize > 0) {
                probability[small[--smallSize]] = 1.0
            }

            return AliasWeightedIndexSampler(probability, alias)
        }

        fun normalizeWeights(weights: List<Double>): DoubleArray {
            val normalized = DoubleArray(weights.size)
            var total = 0.0
            for (index in weights.indices) {
                val weight = weights[index].coerceAtLeast(0.0)
                normalized[index] = weight
                total += weight
            }

            if (total <= 0.0) {
                val fallback = 1.0 / weights.size
                for (index in normalized.indices) {
                    normalized[index] = fallback
                }
                return normalized
            }

            for (index in normalized.indices) {
                normalized[index] /= total
            }
            return normalized
        }
    }
}
