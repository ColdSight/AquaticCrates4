package gg.aquatic.crates.reward

import gg.aquatic.crates.util.Weightable
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class RewardAmountRange(
    val min: Int,
    val max: Int,
    override val chance: Double,
) : Weightable {
    fun roll(): Int {
        if (min == max) {
            return min
        }

        return Random.nextInt(min(min, max), max(min, max) + 1)
    }
}
