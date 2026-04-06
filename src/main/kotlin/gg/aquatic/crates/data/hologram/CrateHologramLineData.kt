package gg.aquatic.crates.data.hologram

import gg.aquatic.kholograms.serialize.LineSettings
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import org.bukkit.inventory.ItemStack

@Serializable
abstract class CrateHologramLineData {
    abstract fun toSettings(rewardEntries: List<RewardHologramEntry> = emptyList()): List<LineSettings>
}

data class RewardHologramEntry(
    val item: ItemStack,
    val displayName: Component,
)
