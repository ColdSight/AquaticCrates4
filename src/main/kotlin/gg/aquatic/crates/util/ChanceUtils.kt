package gg.aquatic.crates.util

import gg.aquatic.common.decimals
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.round

class ChanceUtils {
    companion object {
        fun <T : Weightable> getRandomChanceIndex(
            items: Collection<T>,
            random: ThreadLocalRandom = ThreadLocalRandom.current(),
        ): Int {
            if (items.isEmpty()) return -1

            var totalWeight = 0.0
            for (chance in items) {
                if (chance.chance > 0) totalWeight += chance.chance
            }

            if (totalWeight <= 0) return -1

            var random = random.nextDouble() * totalWeight
            for (i in items.indices) {
                val chance = items.elementAt(i).chance
                if (chance <= 0) continue
                random -= chance
                if (random <= 0.0) return i
            }
            return -1
        }

        fun <T : Weightable> getRandomItem(
            items: Collection<T>,
            random: ThreadLocalRandom = ThreadLocalRandom.current()
        ): T {
            require(items.isNotEmpty()) { "Cannot get random item from empty collection" }
            val size = items.size
            if (size == 1) return items.first()

            var totalWeight = 0.0
            for (item in items) {
                val c = item.chance
                if (c > 0) totalWeight += c
            }

            if (totalWeight <= 0) return items.random()

            var random = random.nextDouble() * totalWeight
            for (item in items) {
                val c = item.chance
                if (c <= 0) continue
                random -= c
                if (random <= 0.0) return item
            }
            return items.last()
        }
    }
}

fun <T : Weightable> Collection<T>.randomItem(): T = ChanceUtils.getRandomItem(this)
fun <T : Weightable> Collection<T>.randomItemIndex(): Int = ChanceUtils.getRandomChanceIndex(this)

fun Collection<Weightable>.realChance(item: Weightable): Double {
    val total = this.sumOf { it.chance }
    return if (total > 0) item.chance / total else 0.0
}

fun Collection<Weightable>.realChanceFormatted(item: Weightable): Double {
    val chance = realChance(item) * 100.0
    return round(chance * 100.0) / 100.0
}

fun <T : Weightable> Collection<T>.realChance(item: T): HashMap<T, Double> {
    var total = 0.0
    for (chance in this) {
        total += chance.chance
    }
    val realChance = HashMap<T, Double>()
    for (chance in this) {
        realChance[chance] = chance.chance / total
    }
    return realChance
}

fun <T : Weightable> Collection<T>.realChanceFormatted(item: T): HashMap<T, Double> {
    var total = 0.0
    for (chance in this) {
        total += chance.chance
    }
    val realChance = HashMap<T, Double>()
    for (chance in this) {
        realChance[chance] = (chance.chance / total * 100.0).decimals(2).toDouble()
    }
    return realChance
}