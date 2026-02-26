package gg.aquatic.crates

import gg.aquatic.blokk.impl.VanillaBlock
import gg.aquatic.clientside.serialize.ClientsideBlockSettings
import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.command.initializeCommands
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.crate.preview.PreviewMenuSettings
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.RewardRarity
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.settings.PrivateMenuSettings
import gg.aquatic.kregistry.bootstrap.RegistryHolder
import gg.aquatic.stacked.StackedItem
import gg.aquatic.stacked.register
import gg.aquatic.waves.Waves
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

object CratesPlugin : JavaPlugin(), RegistryHolder {
    override fun onLoad() {
        registryBootstrap(Waves) {
            pre {
                CrateHandler.crates += "test" to Crate(
                    "test",
                    ItemStack(Material.TRIPWIRE_HOOK),
                    "Test".toMMComponent(),
                    null,
                    listOf(),
                    interactables = listOf(
                        ClientsideBlockSettings(VanillaBlock(Material.CHEST.createBlockData()), 50)
                    ),
                    rewards = listOf(
                        Reward(
                            "test",
                            "Test".toMMComponent(),
                            {
                                ItemStack(Material.DIAMOND)
                            },
                            null,
                            { player ->
                                player.sendMessage("You have won the test reward!")
                            },
                            { true },
                            null,
                            { _, _ ->
                                // Purchasing, etc.
                            },
                            RewardRarity("test", "Test".toMMComponent(), 1.0),
                            1.0
                        )
                    ),
                    preview = PreviewMenuSettings.Basic(
                        listOf(10, 11, 12, 13, 14, 15, 16),
                        PrivateMenuSettings(
                            InventoryType.GENERIC9X1,
                            "Preview Menu".toMMComponent(),
                            hashMapOf()
                        )
                    )
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

    fun reload() {
        Waves.rebuildRegistries(this)
    }
}