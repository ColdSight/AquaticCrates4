package gg.aquatic.crates.stats

internal data class CrateStatsBatchAggregation(
    val hourlyCrateOpens: Map<Pair<Long, String>, Long>,
    val hourlyRewardStats: Map<Triple<Long, String, String>, RewardTotals>,
    val allTimeCrateOpens: Map<String, Long>,
    val allTimeRewardStats: Map<Pair<String, String>, RewardTotals>,
)

internal data class RewardTotals(
    val wins: Long,
    val amountSum: Long,
)

internal object CrateStatsBatchAggregator {
    fun aggregate(openings: List<LoggedOpening>): CrateStatsBatchAggregation {
        val hourlyCrateOpens = HashMap<Pair<Long, String>, Long>()
        val hourlyRewardStats = HashMap<Triple<Long, String, String>, MutableRewardTotals>()
        val allTimeCrateOpens = HashMap<String, Long>()
        val allTimeRewardStats = HashMap<Pair<String, String>, MutableRewardTotals>()

        openings.forEach { opening ->
            val bucketHour = CrateStats.truncateHour(opening.openedAtMillis)
            hourlyCrateOpens.merge(bucketHour to opening.crateId, 1L, Long::plus)
            allTimeCrateOpens.merge(opening.crateId, 1L, Long::plus)

            opening.rewards.forEach { reward ->
                hourlyRewardStats.compute(Triple(bucketHour, opening.crateId, reward.rewardId)) { _, current ->
                    val aggregate = current ?: MutableRewardTotals()
                    aggregate.wins += 1L
                    aggregate.amountSum += reward.amount.toLong()
                    aggregate
                }
                allTimeRewardStats.compute(opening.crateId to reward.rewardId) { _, current ->
                    val aggregate = current ?: MutableRewardTotals()
                    aggregate.wins += 1L
                    aggregate.amountSum += reward.amount.toLong()
                    aggregate
                }
            }
        }

        return CrateStatsBatchAggregation(
            hourlyCrateOpens = hourlyCrateOpens,
            hourlyRewardStats = hourlyRewardStats.mapValues { it.value.toImmutable() },
            allTimeCrateOpens = allTimeCrateOpens,
            allTimeRewardStats = allTimeRewardStats.mapValues { it.value.toImmutable() }
        )
    }
}

private class MutableRewardTotals(
    var wins: Long = 0L,
    var amountSum: Long = 0L,
) {
    fun toImmutable(): RewardTotals = RewardTotals(wins = wins, amountSum = amountSum)
}
