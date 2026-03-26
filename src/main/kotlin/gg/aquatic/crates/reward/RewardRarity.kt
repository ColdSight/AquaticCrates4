package gg.aquatic.crates.reward

import gg.aquatic.crates.util.Weightable
import net.kyori.adventure.text.Component

class RewardRarity(
    val id: String,
    val displayName: Component,
    override val chance: Double,
): Weightable