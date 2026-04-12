package gg.aquatic.crates.reward.processor

import kotlin.math.ln
import kotlin.math.pow

internal object MassSampling {
    private const val SMALL_BINOMIAL_THRESHOLD = 10_000L
    private const val WAITING_TIME_MEAN_THRESHOLD = 64.0
    private const val BINOMIAL_BETA_SPLIT_THRESHOLD = 4096L

    fun sampleMultinomialCounts(totalDraws: Long, probabilities: DoubleArray, random: MassRandom): LongArray {
        val counts = LongArray(probabilities.size)
        var remainingDraws = totalDraws
        var remainingProbability = 1.0

        // A multinomial draw can be decomposed into a chain of conditional binomials:
        // sample category 0, then category 1 from the remaining mass, and so on.
        // Reference: https://en.wikipedia.org/wiki/Multinomial_distribution
        for (index in 0 until probabilities.lastIndex) {
            if (remainingDraws <= 0L) {
                break
            }

            val currentProbability = probabilities[index]
            if (currentProbability <= 0.0) {
                continue
            }

            val conditionalProbability = if (remainingProbability <= 0.0) 0.0 else currentProbability / remainingProbability
            val sampled = sampleBinomial(remainingDraws, conditionalProbability.coerceIn(0.0, 1.0), random)
            counts[index] = sampled
            remainingDraws -= sampled
            remainingProbability -= currentProbability
        }

        if (counts.isNotEmpty()) {
            counts[counts.lastIndex] = remainingDraws.coerceAtLeast(0L)
        }
        return counts
    }

    private fun sampleBinomial(trials: Long, probability: Double, random: MassRandom): Long {
        if (trials <= 0L || probability <= 0.0) {
            return 0L
        }
        if (probability >= 1.0) {
            return trials
        }
        if (probability > 0.5) {
            return trials - sampleBinomial(trials, 1.0 - probability, random)
        }

        // Small-n path: the direct Bernoulli loop is cheap and avoids the overhead of the more advanced samplers.
        if (trials <= SMALL_BINOMIAL_THRESHOLD) {
            var successes = 0L
            repeat(trials.toInt()) {
                if (random.nextDouble() < probability) {
                    successes++
                }
            }
            return successes
        }

        // Rare-event path: when np is small, sampling geometric waiting times between successes is cheaper than
        // flipping every Bernoulli trial.
        if (trials * probability <= WAITING_TIME_MEAN_THRESHOLD) {
            return sampleBinomialWaitingTime(trials, probability, random)
        }

        var remainingTrials = trials
        var remainingProbability = probability
        var successes = 0L

        while (remainingTrials > BINOMIAL_BETA_SPLIT_THRESHOLD) {
            if (remainingProbability <= 0.0) {
                return successes
            }
            if (remainingProbability >= 1.0) {
                return successes + remainingTrials
            }
            if (remainingTrials * remainingProbability <= WAITING_TIME_MEAN_THRESHOLD) {
                return successes + sampleBinomialWaitingTime(remainingTrials, remainingProbability, random)
            }

            // Large-n path: recursively cut the remaining binomial with a beta pivot instead of iterating over all
            // trials. This is the standard beta/binomial relationship used for fast binomial generation.
            val splitPoint = (remainingTrials / 2L) + 1L
            val leftShape = splitPoint.toDouble()
            val rightShape = (remainingTrials - splitPoint + 1L).toDouble()
            val pivot = sampleBeta(leftShape, rightShape, random)

            if (pivot >= remainingProbability) {
                remainingTrials = splitPoint - 1L
                remainingProbability /= pivot
            } else {
                successes += splitPoint
                remainingTrials -= splitPoint
                remainingProbability = (remainingProbability - pivot) / (1.0 - pivot)
            }
        }

        return successes + sampleBinomialDirect(remainingTrials, remainingProbability, random)
    }

    private fun sampleBinomialDirect(trials: Long, probability: Double, random: MassRandom): Long {
        var successes = 0L
        repeat(trials.toInt()) {
            if (random.nextDouble() < probability) {
                successes++
            }
        }
        return successes
    }

    private fun sampleBinomialWaitingTime(trials: Long, probability: Double, random: MassRandom): Long {
        if (trials <= 0L || probability <= 0.0) {
            return 0L
        }

        val logFailure = ln(1.0 - probability)
        var successes = 0L
        var position = 0L
        while (position < trials) {
            val skip = (ln(random.nextDouble()) / logFailure).toLong()
            position += skip + 1L
            if (position <= trials) {
                successes++
            }
        }
        return successes
    }

    private fun sampleBeta(alpha: Double, beta: Double, random: MassRandom): Double {
        val left = sampleGamma(alpha, random)
        val right = sampleGamma(beta, random)
        val total = left + right
        return if (total <= 0.0) 0.5 else left / total
    }

    private fun sampleGamma(shape: Double, random: MassRandom): Double {
        if (shape <= 0.0) {
            return 0.0
        }
        if (shape < 1.0) {
            return sampleGamma(shape + 1.0, random) * random.nextDouble().pow(1.0 / shape)
        }

        // Marsaglia-Tsang gamma sampler for shape >= 1. The shape<1 case above uses the standard boosting identity.
        // Reference: George Marsaglia, Wai Wan Tsang, "A Simple Method for Generating Gamma Variables"
        // https://doi.org/10.1145/358407.358414
        val d = shape - (1.0 / 3.0)
        val c = 1.0 / kotlin.math.sqrt(9.0 * d)
        while (true) {
            val x = random.nextGaussian()
            val vBase = 1.0 + (c * x)
            if (vBase <= 0.0) {
                continue
            }

            val v = vBase * vBase * vBase
            val u = random.nextDouble()
            val xSquared = x * x
            if (u < 1.0 - (0.0331 * xSquared * xSquared)) {
                return d * v
            }
            if (ln(u) < (0.5 * xSquared) + d * (1.0 - v + ln(v))) {
                return d * v
            }
        }
    }
}
