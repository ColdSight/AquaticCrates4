package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.reward.processor.MassRewardGrant

internal data class MassOpenAggregation(
    val openedCount: Int,
    val grants: List<MassRewardGrant>,
)

internal data class MassRewardChunkAggregation(
    val winCounts: LongArray,
    val amountSums: LongArray,
)
