package gg.aquatic.crates.crate.opening

internal data class MassPriceSelection(
    val group: gg.aquatic.crates.open.OpenPriceGroup?,
    val maxAffordable: Long,
)

/**
 * Strategy used to aggregate a large number of opens without replaying every open one by one.
 *
 * The strategies are ordered from the cheapest exact closed-form path to the most expensive
 * per-open fallback:
 * - [DETERMINISTIC]: one reward, fixed reward-count and fixed amount, so the result is simple multiplication.
 * - [LIMIT_AWARE]: one draw per open with reward caps, so we sample total draws in aggregate and clamp/redistribute
 *   overflow instead of replaying each open.
 * - [MULTINOMIAL]: fixed number of draws per open and no reward limits, so the whole batch can be sampled as one
 *   multinomial experiment.
 * - [VECTORIZED]: still aggregated, but requires raw sampling work; the batch is split into independent worker chunks
 *   and their totals are merged at the end.
 * - [EXACT]: replay every open because the semantics depend on per-open state (for example unique choose menus or
 *   multi-draw reward limits).
 *
 * References:
 * - Multinomial distribution: https://en.wikipedia.org/wiki/Multinomial_distribution
 * - Alias method overview: https://www.keithschwarz.com/darts-dice-coins/
 */
internal enum class MassAggregationStrategy {
    DETERMINISTIC,
    LIMIT_AWARE,
    MULTINOMIAL,
    VECTORIZED,
    EXACT,
}
