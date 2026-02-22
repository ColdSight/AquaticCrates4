package gg.aquatic.crates.open

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import org.bukkit.entity.Player

fun interface OpenConditions {
    suspend fun check(player: Player, crate: Crate, crateHandle: CrateHandle?): Boolean

    companion object {
        val DUMMY = OpenConditions { _, _, _ -> true }
    }
}