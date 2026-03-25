package gg.aquatic.crates.await

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.events.ModelRegistrationEvent
import com.ticxo.modelengine.api.generator.ModelGenerator
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import dev.lone.itemsadder.api.ItemsAdder
import gg.aquatic.crates.CratesPlugin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitAll
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

suspend fun awaitStartupDependencies() {
    val awaiters = buildList {
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            add(awaitItemsAdder())
        }
        if (Bukkit.getPluginManager().getPlugin("ModelEngine") != null) {
            add(awaitModelEngine())
        }
    }

    awaiters.awaitAll()
}

private fun awaitItemsAdder(): CompletableDeferred<Unit> {
    val deferred = CompletableDeferred<Unit>()

    if (runCatching { ItemsAdder.areItemsLoaded() }.getOrDefault(false)) {
        deferred.complete(Unit)
        return deferred
    }

    val listener = object : Listener {
        @EventHandler
        fun onItemsAdderLoad(event: ItemsAdderLoadDataEvent) {
            HandlerList.unregisterAll(this)
            deferred.complete(Unit)
        }
    }

    Bukkit.getPluginManager().registerEvents(listener, CratesPlugin)
    deferred.invokeOnCompletion {
        HandlerList.unregisterAll(listener)
    }
    return deferred
}

private fun awaitModelEngine(): CompletableDeferred<Unit> {
    val deferred = CompletableDeferred<Unit>()

    val initialized = runCatching {
        ModelEngineAPI.getAPI().modelGenerator.isInitialized
    }.getOrDefault(false)
    if (initialized) {
        deferred.complete(Unit)
        return deferred
    }

    val listener = object : Listener {
        @EventHandler
        fun onModelRegistration(event: ModelRegistrationEvent) {
            if (event.phase != ModelGenerator.Phase.FINISHED) return
            HandlerList.unregisterAll(this)
            deferred.complete(Unit)
        }
    }

    Bukkit.getPluginManager().registerEvents(listener, CratesPlugin)
    deferred.invokeOnCompletion {
        HandlerList.unregisterAll(listener)
    }
    return deferred
}
