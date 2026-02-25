package gg.aquatic.crates.crate

import gg.aquatic.snapshotmap.SuspendingSnapshotMap
import org.bukkit.Location
import java.util.concurrent.ConcurrentHashMap

object CrateHandler {

    val crates = ConcurrentHashMap<String, Crate>()
    val crateHandles = SuspendingSnapshotMap<Location, CrateHandle>()

    fun spawnCrate(location: Location, crate: Crate, persistent: Boolean): CrateHandle {
        val crateHandle = CrateHandle(crate, location, persistent).apply {
            println("Registering interactable")
            interactables.forEach { it.register() } }
        crateHandles[location.toBlockLocation()] = crateHandle
        return crateHandle
    }
}