package gg.aquatic.crates.stats

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CrateStatsBatchAggregatorTest {
    @Test
    fun `aggregate groups crate opens and reward totals by hour and all time`() {
        val player = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
        val firstHour = 1_700_000_000_000L
        val sameHour = firstHour + 20_000L
        val nextHour = firstHour + CrateStats.HOUR_MILLIS

        val aggregation = CrateStatsBatchAggregator.aggregate(
            listOf(
                LoggedOpening(
                    playerUuid = player,
                    crateId = "alpha",
                    openedAtMillis = firstHour,
                    rewards = listOf(
                        LoggedRewardWin("common", "c", 2),
                        LoggedRewardWin("rare", "r", 1)
                    )
                ),
                LoggedOpening(
                    playerUuid = player,
                    crateId = "alpha",
                    openedAtMillis = sameHour,
                    rewards = listOf(
                        LoggedRewardWin("common", "c", 3)
                    )
                ),
                LoggedOpening(
                    playerUuid = player,
                    crateId = "beta",
                    openedAtMillis = nextHour,
                    rewards = listOf(
                        LoggedRewardWin("common", "c", 4)
                    )
                )
            )
        )

        assertEquals(
            mapOf(
                (CrateStats.truncateHour(firstHour) to "alpha") to 2L,
                (CrateStats.truncateHour(nextHour) to "beta") to 1L
            ),
            aggregation.hourlyCrateOpens
        )
        assertEquals(
            mapOf(
                "alpha" to 2L,
                "beta" to 1L
            ),
            aggregation.allTimeCrateOpens
        )
        assertEquals(
            RewardTotals(wins = 2, amountSum = 5),
            aggregation.hourlyRewardStats[Triple(CrateStats.truncateHour(firstHour), "alpha", "common")]
        )
        assertEquals(
            RewardTotals(wins = 1, amountSum = 1),
            aggregation.hourlyRewardStats[Triple(CrateStats.truncateHour(firstHour), "alpha", "rare")]
        )
        assertEquals(
            RewardTotals(wins = 1, amountSum = 4),
            aggregation.hourlyRewardStats[Triple(CrateStats.truncateHour(nextHour), "beta", "common")]
        )
        assertEquals(
            RewardTotals(wins = 2, amountSum = 5),
            aggregation.allTimeRewardStats["alpha" to "common"]
        )
        assertEquals(
            RewardTotals(wins = 1, amountSum = 1),
            aggregation.allTimeRewardStats["alpha" to "rare"]
        )
        assertEquals(
            RewardTotals(wins = 1, amountSum = 4),
            aggregation.allTimeRewardStats["beta" to "common"]
        )
    }

    @Test
    fun `aggregate handles empty openings list`() {
        val aggregation = CrateStatsBatchAggregator.aggregate(emptyList())

        assertEquals(emptyMap(), aggregation.hourlyCrateOpens)
        assertEquals(emptyMap(), aggregation.hourlyRewardStats)
        assertEquals(emptyMap(), aggregation.allTimeCrateOpens)
        assertEquals(emptyMap(), aggregation.allTimeRewardStats)
    }

    @Test
    fun `aggregate respects compressed open and reward counts`() {
        val player = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
        val openedAt = 1_700_000_000_000L

        val aggregation = CrateStatsBatchAggregator.aggregate(
            listOf(
                LoggedOpening(
                    playerUuid = player,
                    crateId = "alpha",
                    openedAtMillis = openedAt,
                    rewards = listOf(
                        LoggedRewardWin("common", "c", amount = 12, winCount = 4),
                        LoggedRewardWin("rare", "r", amount = 9, winCount = 3)
                    ),
                    openCount = 7
                )
            )
        )

        assertEquals(7L, aggregation.hourlyCrateOpens[CrateStats.truncateHour(openedAt) to "alpha"])
        assertEquals(7L, aggregation.allTimeCrateOpens["alpha"])
        assertEquals(
            RewardTotals(wins = 4, amountSum = 12),
            aggregation.hourlyRewardStats[Triple(CrateStats.truncateHour(openedAt), "alpha", "common")]
        )
        assertEquals(
            RewardTotals(wins = 3, amountSum = 9),
            aggregation.allTimeRewardStats["alpha" to "rare"]
        )
    }
}
