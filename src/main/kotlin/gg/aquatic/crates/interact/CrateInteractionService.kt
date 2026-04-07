package gg.aquatic.crates.interact

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.debug.CratesDebug
import gg.aquatic.execute.executeActions
import gg.aquatic.stacked.event.StackedItemInteractEvent
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CrateInteractionService {
    private const val WINDOW_MILLIS = 75L
    private val interactionWindow = CrateInteractionWindow(WINDOW_MILLIS)

    fun handleCrateInteraction(crateHandle: CrateHandle, player: Player, clickType: CrateClickType) {
        val now = System.currentTimeMillis()
        interactionWindow.claimCrateInteraction(player.uniqueId, now)
        if (!interactionWindow.tryMarkExecuted(player.uniqueId, now)) {
            return
        }

        val crate = crateHandle.crate
        val usingKeyMapping = crate.isHoldingKey(player)
        val actions = if (usingKeyMapping) crate.keyClickMapping.actions(clickType) else crate.crateClickMapping.actions(clickType)

        CratesDebug.message(player, 1, "You have interacted the crate! $clickType -> ${actions.size} action(s)")
        if (actions.isEmpty()) {
            return
        }

        val binder = CrateClickBinder(
            player = player,
            crate = crate,
            crateHandle = crateHandle,
            clickType = clickType,
            usingKeyMapping = usingKeyMapping
        )

        VirtualsCtx {
            actions.map { it.toActionHandle() }.executeActions(binder) { _, str -> str }
        }
    }

    fun handleKeyInteraction(crate: Crate, event: StackedItemInteractEvent, clickType: CrateClickType) {
        val player = event.player
        val startedAt = System.currentTimeMillis()
        val originalEvent = event.originalEvent as? PlayerInteractEvent
        event.cancelled = true

        fun execute() {
            if (interactionWindow.wasCrateInteractionClaimedSince(player.uniqueId, startedAt)) {
                return
            }
            if (!interactionWindow.tryMarkExecuted(player.uniqueId, System.currentTimeMillis())) {
                return
            }

            val actions = crate.keyClickMapping.actions(clickType)
            CratesDebug.message(player, 1, "You have interacted with the key! $clickType -> ${actions.size} action(s)")
            if (actions.isEmpty()) {
                return
            }

            val binder = CrateClickBinder(
                player = player,
                crate = crate,
                crateHandle = null,
                clickType = clickType,
                usingKeyMapping = true
            )

            VirtualsCtx {
                actions.map { it.toActionHandle() }.executeActions(binder) { _, str -> str }
            }
        }

        if (originalEvent?.clickedBlock != null) {
            CratesPlugin.server.scheduler.runTask(CratesPlugin, Runnable { execute() })
        } else {
            execute()
        }
    }

    fun isPhysicalCrateInteraction(event: PlayerInteractEvent?): Boolean {
        return event?.clickedBlock?.location?.let { CrateHandler.crateHandles[it] } != null
    }
}
