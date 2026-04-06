package gg.aquatic.crates.stats

data class CrateStatsInvalidationResult(
    val deletedOpenings: Int,
    val deletedOpeningRewards: Int,
    val deletedHourlyCrateBuckets: Int,
    val deletedHourlyRewardBuckets: Int,
    val deletedAllTimeCrateRows: Int,
    val deletedAllTimeRewardRows: Int,
    val deletedExpiredHourlyCrateBuckets: Int,
    val deletedExpiredHourlyRewardBuckets: Int,
) {
    val totalDeletedRows: Int
        get() = deletedOpenings +
            deletedOpeningRewards +
            deletedHourlyCrateBuckets +
            deletedHourlyRewardBuckets +
            deletedAllTimeCrateRows +
            deletedAllTimeRewardRows +
            deletedExpiredHourlyCrateBuckets +
            deletedExpiredHourlyRewardBuckets
}
