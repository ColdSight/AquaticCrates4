package gg.aquatic.crates.milestone

class CrateMilestoneManager(
    val milestones: List<CrateMilestone>,
    val repeatableMilestones: List<CrateMilestone>,
) {
    data class MilestoneHit(
        val milestone: CrateMilestone,
        val hitCount: Long,
    )

    val hasAnyMilestones: Boolean
        get() = milestones.isNotEmpty() || repeatableMilestones.isNotEmpty()

    fun milestonesReached(totalOpened: Int): List<CrateMilestone> {
        if (totalOpened < 1) return emptyList()

        return buildList {
            milestones.firstOrNull { it.milestone == totalOpened }?.let(::add)
            repeatableMilestones.forEach { milestone ->
                if (totalOpened % milestone.milestone == 0) {
                    add(milestone)
                }
            }
        }
    }

    fun milestoneHitsInRange(previousOpened: Long, currentOpened: Long): List<MilestoneHit> {
        if (currentOpened <= previousOpened) {
            return emptyList()
        }

        return buildList {
            milestones.forEach { milestone ->
                val total = milestone.milestone.toLong()
                if (total > previousOpened && total <= currentOpened) {
                    add(MilestoneHit(milestone, 1L))
                }
            }

            repeatableMilestones.forEach { milestone ->
                val step = milestone.milestone.toLong()
                if (step <= 0L) {
                    return@forEach
                }

                val hits = currentOpened.floorDiv(step) - previousOpened.floorDiv(step)
                if (hits > 0L) {
                    add(MilestoneHit(milestone, hits))
                }
            }
        }
    }
}
