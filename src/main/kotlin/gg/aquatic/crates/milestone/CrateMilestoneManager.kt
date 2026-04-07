package gg.aquatic.crates.milestone

class CrateMilestoneManager(
    val milestones: List<CrateMilestone>,
    val repeatableMilestones: List<CrateMilestone>,
) {
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
}
