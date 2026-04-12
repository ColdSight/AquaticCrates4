package gg.aquatic.crates.reward.processor

import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sqrt

internal class MassRandom(seed: Long) {
    private var state0 = mixSeed(seed)
    private var state1 = mixSeed(state0)
    private var cachedGaussian = 0.0
    private var hasCachedGaussian = false

    init {
        if (state0 == 0L && state1 == 0L) {
            state1 = GOLDEN_GAMMA
        }
    }

    fun nextInt(bound: Int): Int {
        require(bound > 0) { "Bound must be positive." }
        val boundLong = bound.toLong()
        while (true) {
            val bits = nextLong().ushr(1)
            val value = bits % boundLong
            if (bits - value + (boundLong - 1L) >= 0L) {
                return value.toInt()
            }
        }
    }

    fun nextInt(origin: Int, bound: Int): Int {
        require(bound > origin) { "Bound must be greater than origin." }
        return origin + nextInt(bound - origin)
    }

    fun nextDouble(): Double = nextLong().ushr(11) * DOUBLE_UNIT

    fun nextLong(): Long {
        val s0 = state0
        var s1 = state1
        val result = java.lang.Long.rotateLeft(s0 + s1, 17) + s0

        s1 = s1 xor s0
        state0 = java.lang.Long.rotateLeft(s0, 49) xor s1 xor (s1 shl 21)
        state1 = java.lang.Long.rotateLeft(s1, 28)

        return result
    }

    fun nextGaussian(): Double {
        if (hasCachedGaussian) {
            hasCachedGaussian = false
            return cachedGaussian
        }

        var u1 = 0.0
        while (u1 <= Double.MIN_VALUE) {
            u1 = nextDouble()
        }

        val u2 = nextDouble()
        val magnitude = sqrt(-2.0 * ln(u1))
        val angle = TWO_PI * u2
        cachedGaussian = magnitude * kotlin.math.sin(angle)
        hasCachedGaussian = true
        return magnitude * kotlin.math.cos(angle)
    }

    companion object {
        fun mixSeed(seed: Long): Long {
            var z = seed + GOLDEN_GAMMA
            z = (z xor (z ushr 30)) * -4658895280553007687L
            z = (z xor (z ushr 27)) * -7723592293110705685L
            return z xor (z ushr 31)
        }

        private const val GOLDEN_GAMMA = -7046029254386353131L
        private const val TWO_PI = PI * 2.0
        private const val DOUBLE_UNIT = 1.0 / (1L shl 53).toDouble()
    }
}
