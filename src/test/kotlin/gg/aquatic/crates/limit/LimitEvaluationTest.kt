package gg.aquatic.crates.limit

import gg.aquatic.crates.stats.CrateStatsTimeframe
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LimitEvaluationTest {
    @Test
    fun `canPass returns true for empty limits`() {
        runBlocking {
            assertTrue(LimitEvaluation.canPass(emptyList()) { 999L })
        }
    }

    @Test
    fun `canPass allows values below all limits`() {
        val limits = listOf(
            LimitHandle(CrateStatsTimeframe.DAY, 2),
            LimitHandle(CrateStatsTimeframe.WEEK, 5)
        )

        val result = runBlocking {
            LimitEvaluation.canPass(limits) { limit ->
                when (limit.timeframe) {
                    CrateStatsTimeframe.DAY -> 1L
                    CrateStatsTimeframe.WEEK -> 4L
                    else -> 0L
                }
            }
        }

        assertTrue(result)
    }

    @Test
    fun `canPass blocks when any timeframe limit is reached`() {
        val limits = listOf(
            LimitHandle(CrateStatsTimeframe.DAY, 1),
            LimitHandle(CrateStatsTimeframe.MONTH, 10)
        )

        val result = runBlocking {
            LimitEvaluation.canPass(limits) { limit ->
                when (limit.timeframe) {
                    CrateStatsTimeframe.DAY -> 1L
                    CrateStatsTimeframe.MONTH -> 2L
                    else -> 0L
                }
            }
        }

        assertFalse(result)
    }
}
