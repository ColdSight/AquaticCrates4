package gg.aquatic.crates.milestone

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrateMilestoneManagerTest {
    @Test
    fun `hasAnyMilestones is false when both lists are empty`() {
        val manager = CrateMilestoneManager(
            milestones = emptyList(),
            repeatableMilestones = emptyList()
        )

        assertFalse(manager.hasAnyMilestones)
    }

    @Test
    fun `hasAnyMilestones is true when any list contains milestones`() {
        val manager = CrateMilestoneManager(
            milestones = listOf(milestone(3)),
            repeatableMilestones = emptyList()
        )

        assertTrue(manager.hasAnyMilestones)
    }

    @Test
    fun `milestonesReached returns exact one-time milestone and matching repeatables`() {
        val normal = milestone(3)
        val repeatableFive = milestone(5)
        val repeatableTen = milestone(10)
        val manager = CrateMilestoneManager(
            milestones = listOf(normal),
            repeatableMilestones = listOf(repeatableFive, repeatableTen)
        )

        val reached = manager.milestonesReached(10)

        assertEquals(listOf(repeatableFive, repeatableTen), reached)
    }

    @Test
    fun `milestonesReached includes one-time milestone when exact total is reached`() {
        val normal = milestone(3)
        val repeatable = milestone(6)
        val manager = CrateMilestoneManager(
            milestones = listOf(normal),
            repeatableMilestones = listOf(repeatable)
        )

        val reached = manager.milestonesReached(3)

        assertEquals(listOf(normal), reached)
    }

    @Test
    fun `milestonesReached returns empty for zero or negative totals`() {
        val manager = CrateMilestoneManager(
            milestones = listOf(milestone(1)),
            repeatableMilestones = listOf(milestone(5))
        )

        assertTrue(manager.milestonesReached(0).isEmpty())
        assertTrue(manager.milestonesReached(-5).isEmpty())
    }

    @Test
    fun `milestoneHitsInRange preserves repeatable hits across chunk boundaries`() {
        val repeatable = milestone(5)
        val manager = CrateMilestoneManager(
            milestones = emptyList(),
            repeatableMilestones = listOf(repeatable)
        )

        val hits = manager.milestoneHitsInRange(
            previousOpened = 0L,
            currentOpened = 5_000_000_000L
        )

        assertEquals(1, hits.size)
        assertEquals(repeatable, hits.single().milestone)
        assertEquals(1_000_000_000L, hits.single().hitCount)
    }

    private fun milestone(at: Int): CrateMilestone {
        return CrateMilestone(
            milestone = at,
            displayName = null,
            rewards = emptyList()
        )
    }
}
