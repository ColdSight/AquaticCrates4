package gg.aquatic.crates.data.condition

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import org.bukkit.entity.Player

data class CrateOpenConditionBinder(
    val player: Player,
    val crate: Crate,
    val crateHandle: CrateHandle?,
)
