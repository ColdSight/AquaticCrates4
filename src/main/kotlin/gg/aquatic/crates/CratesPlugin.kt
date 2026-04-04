package gg.aquatic.crates

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.await.awaitStartupDependencies
import gg.aquatic.crates.command.initializeCommands
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.data.CrateStorage
import gg.aquatic.crates.open.currency.CrateKeyCurrency
import gg.aquatic.kregistry.bootstrap.RegistryHolder
import gg.aquatic.kurrency.Currency
import gg.aquatic.kurrency.impl.VirtualCurrency
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
            val virtualCurrencies = CrateHandler.crates.keys.associateWith { crateId ->
                VirtualCurrency.of("aqcrates:key:$crateId")
            }
            val currencies = CrateHandler.crates.keys.map { crateId ->
                CrateKeyCurrency(
                    crateId,
                    { CrateHandler.crates[crateId] },
                    virtualCurrencies.getValue(crateId)
                )
            }

            registry(StackedItem.ITEM_REGISTRY_KEY) {
                for ((id, crate) in CrateHandler.crates) {
                    crate.crateItem.register(this, "acrates_chest", id) { e ->
                        crate.handleCrateItemInteractions(e)
                    }
                }
            }

            registry(VirtualCurrency.REGISTRY_KEY) {
                for (currency in virtualCurrencies.values) {
                    add(currency.id, currency)
                }
            }

            registry(Currency.REGISTRY_KEY) {
                for (currency in currencies) {
                    add(currency.id, currency)
                }
            }
        }
    }

    override fun onEnable() {
        initializeCommands()

        VirtualsCtx {
            awaitStartupDependencies()
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

    fun crateKeyVirtualCurrency(crateId: String): VirtualCurrency {
        val currencyId = "aqcrates:key:$crateId"
        return VirtualCurrency.REGISTRY[currencyId]
            ?: error("Virtual currency '$currencyId' is not registered.")
    }

    fun crateKeyCurrency(crateId: String): CrateKeyCurrency {
        val currencyId = "aqcrates:key:$crateId"
        return Currency.REGISTRY[currencyId] as? CrateKeyCurrency
            ?: error("Crate key currency '$currencyId' is not registered.")
    }
}
