package gg.aquatic.crates.crate.opening

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.processor.MassRandom
import gg.aquatic.crates.reward.processor.MassRewardGrant
import gg.aquatic.crates.reward.processor.MassSampling
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

internal object MassOpeningSupport {
    const val MULTINOMIAL_OPEN_THRESHOLD = 1_000_000
    const val VECTORIZE_OPEN_THRESHOLD = 100_000
    const val WORKER_DRAW_GRANULARITY = 2_000_000L

    private val workerDispatchers = ConcurrentHashMap<Int, kotlinx.coroutines.CoroutineDispatcher>()

    fun mergeMassRewardGrants(grants: List<MassRewardGrant>): List<MassRewardGrant> {
        if (grants.isEmpty()) {
            return emptyList()
        }

        val merged = LinkedHashMap<Reward, MutableMassRewardGrant>()
        grants.forEach { grant ->
            val aggregate = merged.getOrPut(grant.reward) { MutableMassRewardGrant(grant.reward) }
            aggregate.winCount += grant.winCount
            aggregate.totalAmount += grant.totalAmount
        }
        return merged.values.map { it.toImmutable() }
    }

    fun sampleCappedRewardCounts(
        totalDraws: Long,
        probabilities: DoubleArray,
        capacities: LongArray,
        random: MassRandom,
    ): LongArray {
        val counts = LongArray(probabilities.size)
        var remainingDraws = totalDraws
        var active = probabilities.indices.filterTo(ArrayList(probabilities.size)) { capacities[it] > 0L }

        // Re-sample only the overflow mass against rewards that still have free capacity.
        // This approximates "sample without breaking limits" while staying fully aggregated.
        // In other words: draw as if there were no limits, keep the accepted mass, then redistribute only the
        // overflow over the still-available rewards until either all draws are assigned or no capacity remains.
        while (remainingDraws > 0L && active.isNotEmpty()) {
            val normalized = normalizeActiveProbabilities(probabilities, active)
            val sampled = MassSampling.sampleMultinomialCounts(remainingDraws, normalized, random)
            var overflow = 0L
            val nextActive = ArrayList<Int>(active.size)

            for (activeIndex in active.indices) {
                val rewardIndex = active[activeIndex]
                val cap = capacities[rewardIndex]
                val available = cap - counts[rewardIndex]
                if (available <= 0L) {
                    continue
                }

                val requested = sampled[activeIndex]
                val granted = minOf(requested, available)
                counts[rewardIndex] += granted
                overflow += requested - granted
                if (counts[rewardIndex] < cap) {
                    nextActive += rewardIndex
                }
            }

            if (overflow <= 0L) {
                break
            }
            remainingDraws = overflow
            active = nextActive
        }

        return counts
    }

    fun enforceRewardCap(
        sampledCounts: LongArray,
        capacities: LongArray,
    ): LongArray {
        if (sampledCounts.isEmpty()) {
            return sampledCounts
        }

        val clamped = sampledCounts.copyOf()
        for (index in clamped.indices) {
            val capacity = capacities.getOrElse(index) { Long.MAX_VALUE }
            if (capacity < 0L) {
                clamped[index] = 0L
                continue
            }
            if (clamped[index] > capacity) {
                clamped[index] = capacity
            }
        }
        return clamped
    }

    fun selectWorkerCount(expectedDraws: Long): Int {
        val processors = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val desired = (expectedDraws / WORKER_DRAW_GRANULARITY).coerceAtLeast(1L)
        return minOf(processors, desired.coerceAtMost(processors.toLong()).toInt())
    }

    fun openChunkSize(amount: Int, workerCount: Int): Int {
        // Split by open count, not by reward count, so each worker can evaluate the same sampler state
        // independently and we only merge aggregated win/amount totals at the end.
        return ceil(amount.toDouble() / workerCount.toDouble()).toInt().coerceAtLeast(1)
    }

    fun workerDispatcher(workerCount: Int): kotlinx.coroutines.CoroutineDispatcher {
        return workerDispatchers.computeIfAbsent(workerCount) {
            VirtualsCtx.limitedParallelism(it.coerceAtLeast(1))
        }
    }

    private fun normalizeActiveProbabilities(
        probabilities: DoubleArray,
        active: List<Int>,
    ): DoubleArray {
        val normalized = DoubleArray(active.size)
        var total = 0.0
        for (index in active.indices) {
            val probability = probabilities[active[index]].coerceAtLeast(0.0)
            normalized[index] = probability
            total += probability
        }

        if (total <= 0.0) {
            val fallback = 1.0 / active.size.toDouble()
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
