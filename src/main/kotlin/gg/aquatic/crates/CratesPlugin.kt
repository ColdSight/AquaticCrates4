package gg.aquatic.crates

import gg.aquatic.blokk.impl.VanillaBlock
import gg.aquatic.clientside.serialize.ClientsideBlockSettings
import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.command.initializeCommands
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.kregistry.bootstrap.RegistryHolder
import gg.aquatic.stacked.StackedItem
import gg.aquatic.stacked.register
import gg.aquatic.waves.Waves
import org.bukkit.Material
import org.bukkit.plugin.java.JavaPlugin

object CratesPlugin : JavaPlugin(), RegistryHolder {
    override fun onLoad() {
        registryBootstrap(Waves) {
            pre {
                CrateHandler.crates += "test" to Crate(
                    "test",
                    "Test".toMMComponent(),
                    null,
                    listOf(),
                    interactables = listOf(
                        ClientsideBlockSettings(VanillaBlock(Material.CHEST.createBlockData()), 50)
                    ),
                    rewards = listOf()
                )
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
    }

    override fun onDisable() {
    }
}