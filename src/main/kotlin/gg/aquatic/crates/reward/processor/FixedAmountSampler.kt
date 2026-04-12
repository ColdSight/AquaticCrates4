package gg.aquatic.crates.reward.processor

/**
 * Trivial amount sampler for fixed values.
 *
 * This lets the mass-opening pipeline turn repeated amount draws into plain multiplication.
 */
internal class FixedAmountSampler(
    private val value: Int,
) : CompiledAmountSampler {
    override fun sample(random: MassRandom): Int = value

    override fun fixedValueOrNull(): Int = value

    override fun maxValue(): Int = value

    override fun sampleTotal(trials: Long, random: MassRandom): Long = trials * value.toLong()
}
