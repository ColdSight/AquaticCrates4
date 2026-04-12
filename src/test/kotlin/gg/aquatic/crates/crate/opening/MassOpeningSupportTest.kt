package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.reward.processor.MassRandom
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class MassOpeningSupportTest {
    @Test
    fun `sample capped reward counts never exceeds capacities`() {
        val capacities = longArrayOf(5L, 7L, 11L)
        val sampled = MassOpeningSupport.sampleCappedRewardCounts(
            totalDraws = 1_000L,
            probabilities = doubleArrayOf(0.7, 0.2, 0.1),
            capacities = capacities,
            random = MassRandom(12345L)
        )

        assertTrue(sampled.indices.all { sampled[it] <= capacities[it] })
        assertTrue(sampled.sum() <= capacities.sum())
    }

    @Test
    fun `enforce reward cap clamps overflowed counts`() {
        val clamped = MassOpeningSupport.enforceRewardCap(
            sampledCounts = longArrayOf(10L, 2L, 15L),
            capacities = longArrayOf(3L, 2L, 5L)
        )

        assertContentEquals(longArrayOf(3L, 2L, 5L), clamped)
    }
}
