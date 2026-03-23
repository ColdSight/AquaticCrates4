package gg.aquatic.crates.crate.preview

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.kmenu.menu.settings.PrivateMenuSettings
import org.bukkit.entity.Player

interface PreviewMenuSettings {
    suspend fun open(player: Player, crate: Crate, crateHandle: CrateHandle?)

    class Basic(
        val rewardSlots: Collection<Int>,
        val invSettings: PrivateMenuSettings,
    ): PreviewMenuSettings {

        override suspend fun open(player: Player, crate: Crate, crateHandle: CrateHandle?) {
            val menu = PreviewListMenu.create(player, crate, crateHandle, this)
            menu.open()
        }
    }

    class CustomPages(
        val pages: Collection<Basic>
    ): PreviewMenuSettings {
        override suspend fun open(player: Player, crate: Crate, crateHandle: CrateHandle?) {
            if (pages.isEmpty()) return
            val menu = PreviewCustomPagesMenu.create(player, crate, crateHandle, this, 0)
            menu.open()
        }
    }
}
