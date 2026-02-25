package gg.aquatic.crates.crate

import gg.aquatic.snapshotmap.SuspendingSnapshotMap
import org.bukkit.Location
import java.util.concurrent.ConcurrentHashMap

object CrateHandler {

    val crates = ConcurrentHashMap<String, Crate>()
    val crateHandles = SuspendingSnapshotMap<Location, CrateHandle>()

}