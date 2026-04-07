package gg.aquatic.crates.debug

import gg.aquatic.crates.CratesPlugin
import org.bukkit.entity.Player

object CratesDebug {

    fun enabled(level: Int): Boolean = CratesPlugin.debugLevel >= level

    fun log(level: Int, message: String) {
        log(CratesLogCategory.GENERAL, level, message)
    }

    fun log(category: CratesLogCategory, level: Int, message: String) {
        if (!enabled(level)) return
        CratesLogger.info(category, "[debug:$level] $message")
    }

    fun message(player: Player, level: Int, message: String) {
        if (!enabled(level)) return
        player.sendMessage(message)
    }
}
