package gg.aquatic.crates

import gg.aquatic.crates.command.initializeCommands
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.data.CrateStorage
import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.kregistry.bootstrap.RegistryHolder
import gg.aquatic.stacked.StackedItem
import gg.aquatic.stacked.register
import gg.aquatic.waves.Waves
import kotlinx.coroutines.runBlocking
import org.bukkit.plugin.java.JavaPlugin

object CratesPlugin : JavaPlugin(), RegistryHolder {
    override fun onLoad() {
        registryBootstrap(Waves) {
            pre {
                CrateHandler.crates.clear()
                CrateHandler.crates.putAll(CrateStorage.loadAllCrates())
            }

            registry(StackedItem.ITEM_REGISTRY_KEY) {
                for ((id, crate) in CrateHandler.crates) {
                    crate.crateItem.register(this, "acrates_chest", id) { e ->
                        crate.handleCrateItemInteractions(e)
                    }
                }
            }
        }
    }

    override fun onEnable() {
        initializeCommands()
        VirtualsCtx {
            CrateHandler.loadPlacedCrates()
        }
    }

    override fun onDisable() {
        runBlocking(VirtualsCtx) {
            CrateHandler.shutdown()
        }
    }

    suspend fun reload() {
        CrateHandler.reloadPlacedCrates {
            CrateHandler.crates.clear()
            CrateHandler.crates.putAll(CrateStorage.loadAllCrates())
            Waves.rebuildRegistries(this)
        }
    }
}
