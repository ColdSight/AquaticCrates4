package gg.aquatic.crates.crate.opening

import java.math.BigInteger

internal data class MassOpenExecutionResult(
    val success: Boolean,
    val openedCount: Int,
    val strategy: MassAggregationStrategy?,
)

internal data class MassOpenChunkResult(
    val success: Boolean,
    val aggregation: MassOpenAggregation,
    val strategy: MassAggregationStrategy?,
)

data class OpeningExecutionResult(
    val success: Boolean,
    val openedCount: BigInteger,
)
