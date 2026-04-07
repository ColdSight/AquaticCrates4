package gg.aquatic.crates.reward.processor

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChooseRewardSelectionTest {
    @Test
    fun `completedSelection keeps existing valid choices and fills missing ones`() {
        val selected = ChooseRewardSelection.completedSelection(
            rewardCount = 5,
            chooseCount = 3,
            selectedIndices = listOf(1),
            random = Random(1234)
        )

        assertEquals(3, selected.size)
        assertTrue(1 in selected)
        assertTrue(selected.all { it in 0..4 })
    }

    @Test
    fun `completedSelection ignores invalid existing indices`() {
        val selected = ChooseRewardSelection.completedSelection(
            rewardCount = 2,
            chooseCount = 2,
            selectedIndices = listOf(-1, 0, 99),
            random = Random(1234)
        )

        assertEquals(setOf(0, 1), selected)
    }

    @Test
    fun `completedSelection never exceeds available rewards`() {
        val selected = ChooseRewardSelection.completedSelection(
            rewardCount = 2,
            chooseCount = 5,
            selectedIndices = emptyList(),
            random = Random(1234)
        )

        assertEquals(setOf(0, 1), selected)
    }
}
