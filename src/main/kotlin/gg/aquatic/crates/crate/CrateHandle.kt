package gg.aquatic.crates.crate

import gg.aquatic.common.audience.GlobalAudience
import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.interact.CrateClickType
import gg.aquatic.crates.interact.CrateInteractionService
import gg.aquatic.replace.PlaceholderContext
import org.bukkit.Location

class CrateHandle(
    val crate: Crate,
    val location: Location,
    val persistent: Boolean,
) {

    val hologram = crate.hologram?.create(
        location.clone().add(0.5, 1.0 + crate.hologramYOffset, 0.5),
        { PlaceholderContext.player }
    )
    val interactables = crate.interactables.map {
        it.toSettings().create(location, GlobalAudience()) { _, player, isLeft ->
            val clickType = CrateClickType.fromInteraction(isLeft, player)
            CrateInteractionService.handleCrateInteraction(this@CrateHandle, player, clickType)
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
