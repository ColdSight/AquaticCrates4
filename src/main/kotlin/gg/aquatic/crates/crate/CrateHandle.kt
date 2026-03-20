package gg.aquatic.crates.crate

import gg.aquatic.common.audience.GlobalAudience
import gg.aquatic.common.coroutine.VirtualsCtx
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
            player.sendMessage("You have interacted the crate! $isLeft")
            if (isLeft) {
                if (player.isSneaking && player.hasPermission("aqcrates.admin")) {
                    destroy()
                    player.sendMessage("Crate destroyed!")
                    return@create
                }
                val preview = crate.preview ?: return@create
                VirtualsCtx {
                    preview.open(player, crate, this@CrateHandle)
                    player.sendMessage("Crate preview opened!")
                }
                return@create
            }

            VirtualsCtx {
                player.sendMessage("Opening the crate!")
                if (crate.tryOpen(player, this@CrateHandle)) {
                    player.sendMessage("Crate opened!")
                }
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
