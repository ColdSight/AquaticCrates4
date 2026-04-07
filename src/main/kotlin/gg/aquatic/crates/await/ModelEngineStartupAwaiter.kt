package gg.aquatic.crates.await

import com.ticxo.modelengine.api.ModelEngineAPI
import com.ticxo.modelengine.api.events.ModelRegistrationEvent
import com.ticxo.modelengine.api.generator.ModelGenerator
import gg.aquatic.common.event
import gg.aquatic.common.unregister
import gg.aquatic.crates.CratesPlugin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

internal object ModelEngineStartupAwaiter : StartupDependencyAwaiter {
    override fun isAvailable(): Boolean {
        return Bukkit.getPluginManager().getPlugin("ModelEngine") != null
    }

    override fun await(): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()

        val initialized = runCatching {
            ModelEngineAPI.getAPI().modelGenerator.isInitialized
        }.getOrDefault(false)
        if (initialized) {
            deferred.complete(Unit)
            return deferred
        }

        val event = event<ModelRegistrationEvent> {
            if (it.phase != ModelGenerator.Phase.FINISHED) return@event
            deferred.complete(Unit)
        }

        deferred.invokeOnCompletion {
            event.unregister()
        }

        return deferred
    }
}
