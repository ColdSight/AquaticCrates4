package gg.aquatic.crates.reward.processor

import kotlin.random.Random

internal object ChooseRewardSelection {
    fun completedSelection(
        rewardCount: Int,
        chooseCount: Int,
        selectedIndices: Collection<Int>,
        random: Random = Random.Default,
    ): Set<Int> {
        val normalizedSelected = linkedSetOf<Int>()
        normalizedSelected += selectedIndices.filter { it in 0 until rewardCount }
        val missingChoices = (chooseCount - normalizedSelected.size).coerceAtLeast(0)
        if (missingChoices == 0) {
            return normalizedSelected
        }

        (0 until rewardCount)
            .filterNot { it in normalizedSelected }
            .shuffled(random)
            .take(missingChoices)
            .forEach { normalizedSelected += it }

        return normalizedSelected
    }
}
