package gg.aquatic.crates.crate

import gg.aquatic.common.audience.GlobalAudience
import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.interact.CrateClickBinder
import gg.aquatic.crates.interact.CrateClickType
import gg.aquatic.crates.debug.CratesDebug
import gg.aquatic.execute.executeActions
import gg.aquatic.replace.PlaceholderContext
import org.bukkit.Location

class CrateHandle(
    val crate: Crate,
    val location: Location,
    val persistent: Boolean,
) {

    val hologram = crate.hologram?.create(location.clone().add(0.5, 1.0, 0.5), { PlaceholderContext.player })
    val interactables = crate.interactables.map {
        it.toSettings().create(location, GlobalAudience()) { obj, player, isLeft ->
            CrateInteractionGuard.claimCrateInteraction(player.uniqueId)
            if (!CrateInteractionGuard.tryMarkExecuted(player.uniqueId)) {
                return@create
            }

            val clickType = CrateClickType.fromInteraction(isLeft, player)
            val usingKeyMapping = crate.isHoldingKey(player)
            val actions = if (usingKeyMapping) {
                crate.keyClickMapping.actions(clickType)
            } else {
                crate.crateClickMapping.actions(clickType)
            }

            CratesDebug.message(player, 1, "You have interacted the crate! $clickType -> ${actions.size} action(s)")
            if (actions.isEmpty()) {
                return@create
            }

            val binder = CrateClickBinder(
                player = player,
                crate = crate,
                crateHandle = this@CrateHandle,
                clickType = clickType,
                usingKeyMapping = usingKeyMapping
            )

            VirtualsCtx {
                actions.map { it.toActionHandle() }.executeActions(binder) { _, str -> str }
            }
        }
    }

    fun destroy() {
        CrateHandler.removeHandle(this)
        VirtualsCtx {
            hologram?.destroy()
        }
        interactables.forEach { it.destroy() }
    }
}
