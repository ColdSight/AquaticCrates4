package gg.aquatic.crates.await

import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent
import dev.lone.itemsadder.api.ItemsAdder
import gg.aquatic.common.event
import gg.aquatic.common.unregister
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.bukkit.Bukkit

internal object ItemsAdderStartupAwaiter : StartupDependencyAwaiter {
    override fun isAvailable(): Boolean {
        return Bukkit.getPluginManager().getPlugin("ItemsAdder") != null
    }

    override fun await(): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()

        // ItemsAdder currently does not expose a better readiness check for this use case.
        @Suppress("DEPRECATION")
        if (runCatching { ItemsAdder.areItemsLoaded() }.getOrDefault(false)) {
            deferred.complete(Unit)
            return deferred
        }

        val event = event<ItemsAdderLoadDataEvent> {
            deferred.complete(Unit)
        }

        deferred.invokeOnCompletion {
            event.unregister()
        }

        return deferred
    }
}
