package gg.aquatic.crates.crate

import gg.aquatic.common.audience.GlobalAudience
import gg.aquatic.replace.PlaceholderContext
import org.bukkit.Location

class CrateHandle(
    val crate: Crate,
    val location: Location,
    val persistent: Boolean,
) {

    val hologram = crate.hologram?.create(location, { PlaceholderContext.player })
    val interactables = crate.interactables.map {
        it.create(location, GlobalAudience()) { obj, player, isLeft ->
            player.sendMessage("You have interacted the crate! $isLeft")
        }
    }

    suspend fun destroy() {
        hologram?.destroy()
        interactables.forEach { it.destroy() }
    }
}