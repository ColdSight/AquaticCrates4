package gg.aquatic.crates.crate

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.common.location.world.AwaitingWorld
import gg.aquatic.snapshotmap.SuspendingSnapshotMap
import gg.aquatic.crates.CratesPlugin
import kotlinx.coroutines.withContext
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentHashMap

object CrateHandler {

    val crates = ConcurrentHashMap<String, Crate>()
    val crateHandles = SuspendingSnapshotMap<Location, CrateHandle>()
    private val awaitingPlacedCrates = ConcurrentHashMap<String, MutableSet<String>>()
    private val persistenceSuppressed = AtomicBoolean(false)
    private val persistenceEpoch = AtomicLong(0L)

    private val placedCratesFile: File
        get() = File(CratesPlugin.dataFolder, "placedcrates.yml")

    fun spawnCrate(location: Location, crate: Crate, persistent: Boolean): CrateHandle {
        val blockLocation = location.toBlockLocation()
        crateHandles[blockLocation]?.destroy()

        val crateHandle = CrateHandle(crate, location, persistent).apply {
            println("Registering interactable")
            interactables.forEach { it.register() }
        }
        crateHandles[blockLocation] = crateHandle
        if (!persistenceSuppressed.get()) {
            scheduleSavePlacedCrates()
        }
        return crateHandle
    }

    fun removeHandle(crateHandle: CrateHandle) {
        val blockLocation = crateHandle.location.toBlockLocation()
        val existing = crateHandles[blockLocation]
        if (existing === crateHandle) {
            crateHandles.remove(blockLocation)
        }
        if (!persistenceSuppressed.get()) {
            scheduleSavePlacedCrates()
        }
    }

    suspend fun reloadPlacedCrates(reloadCrates: () -> Unit) {
        savePlacedCrates()
        runWithoutPersistenceUpdates {
            despawnAll()
            reloadCrates()
            respawnPersistentCrates()
        }
    }

    suspend fun shutdown() {
        savePlacedCrates()
        runWithoutPersistenceUpdates {
            despawnAll()
        }
    }

    private fun despawnAll() {
        val handles = crateHandles.values.toList()
        handles.forEach { it.destroy() }
        crateHandles.clear()
    }

    private suspend fun respawnPersistentCrates() {
        awaitingPlacedCrates.clear()
        val entries = withContext(VirtualsCtx) {
            if (!placedCratesFile.exists()) {
                return@withContext emptyList()
            }

            val config = YamlConfiguration.loadConfiguration(placedCratesFile)
            buildList {
                for (worldName in config.getKeys(false)) {
                    for (entry in config.getStringList(worldName)) {
                        val parts = entry.split(";")
                        if (parts.size < 5) continue

                        val x = parts[1].toDoubleOrNull() ?: continue
                        val y = parts[2].toDoubleOrNull() ?: continue
                        val z = parts[3].toDoubleOrNull() ?: continue
                        val yaw = parts[4].toFloatOrNull() ?: 0f

                        add(PlacedCrateEntry(worldName, parts[0], x, y, z, yaw))
                    }
                }
            }
        }

        for (entry in entries) {
            val crate = crates[entry.crateId] ?: continue
            val serializedEntry = entry.serialized()
            awaitingPlacedCrates
                .computeIfAbsent(entry.worldName) { Collections.synchronizedSet(linkedSetOf()) }
                .add(serializedEntry)
            AwaitingWorld.create(entry.worldName) { world ->
                awaitingPlacedCrates[entry.worldName]?.remove(serializedEntry)
                if (awaitingPlacedCrates[entry.worldName]?.isEmpty() == true) {
                    awaitingPlacedCrates.remove(entry.worldName)
                }
                spawnCrate(Location(world, entry.x, entry.y, entry.z, entry.yaw, 0f), crate, true)
            }
        }
    }

    private suspend fun savePlacedCrates(expectedEpoch: Long? = null) {
        withContext(VirtualsCtx) {
            if (expectedEpoch != null && expectedEpoch != persistenceEpoch.get()) {
                return@withContext
            }
            if (persistenceSuppressed.get()) {
                return@withContext
            }
            placedCratesFile.parentFile.mkdirs()

            val config = YamlConfiguration()
            val grouped = crateHandles.values
                .groupBy { it.location.world?.name ?: return@groupBy "__invalid__" }
                .filterKeys { it != "__invalid__" }

            for ((worldName, handles) in grouped) {
                val serialized = handles.map { handle ->
                    "${handle.crate.id};${handle.location.x};${handle.location.y};${handle.location.z};${handle.location.yaw}"
                }.toMutableList()
                awaitingPlacedCrates[worldName]?.forEach { pending ->
                    if (pending !in serialized) {
                        serialized += pending
                    }
                }
                config.set(worldName, serialized)
            }

            for ((worldName, pendingEntries) in awaitingPlacedCrates) {
                if (config.contains(worldName)) continue
                config.set(worldName, pendingEntries.toList())
            }

            config.save(placedCratesFile)
        }
    }

    private fun scheduleSavePlacedCrates() {
        val expectedEpoch = persistenceEpoch.get()
        VirtualsCtx {
            savePlacedCrates(expectedEpoch)
        }
    }

    private suspend fun runWithoutPersistenceUpdates(block: suspend () -> Unit) {
        persistenceEpoch.incrementAndGet()
        persistenceSuppressed.set(true)
        try {
            block()
        } finally {
            persistenceSuppressed.set(false)
        }
    }

    private data class PlacedCrateEntry(
        val worldName: String,
        val crateId: String,
        val x: Double,
        val y: Double,
        val z: Double,
        val yaw: Float,
    ) {
        fun serialized(): String = "$crateId;$x;$y;$z;$yaw"
    }
}
