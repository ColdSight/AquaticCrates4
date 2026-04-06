package gg.aquatic.crates.stats

enum class CrateStatsTimeframe(val windowMillis: Long?) {
    DAY(24L * 60L * 60L * 1000L),
    WEEK(7L * 24L * 60L * 60L * 1000L),
    MONTH(30L * 24L * 60L * 60L * 1000L),
    ALL_TIME(null),
}
