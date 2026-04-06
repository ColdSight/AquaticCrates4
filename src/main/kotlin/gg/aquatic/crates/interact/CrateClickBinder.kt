package gg.aquatic.crates.interact

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import org.bukkit.entity.Player

data class CrateClickBinder(
    val player: Player,
    val crate: Crate,
    val crateHandle: CrateHandle?,
    val clickType: CrateClickType,
    val usingKeyMapping: Boolean,
)