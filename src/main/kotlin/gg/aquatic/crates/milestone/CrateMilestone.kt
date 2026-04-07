package gg.aquatic.crates.milestone

import gg.aquatic.crates.reward.Reward
import net.kyori.adventure.text.Component

data class CrateMilestone(
    val milestone: Int,
    val displayName: Component?,
    val rewards: Collection<Reward>,
)
