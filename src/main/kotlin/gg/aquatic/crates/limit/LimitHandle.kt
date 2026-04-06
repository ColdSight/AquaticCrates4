package gg.aquatic.crates.limit

import gg.aquatic.crates.stats.CrateStatsTimeframe

data class LimitHandle(
    val timeframe: CrateStatsTimeframe,
    val limit: Int,
)
