package gg.aquatic.crates.util

import gg.aquatic.treepapi.updatePAPIPlaceholders
import org.bukkit.entity.Player

fun String.replacePlayerPlaceholder(player: Player): String {
    val updated = replace("%player%", player.name)
    return runCatching { updated.updatePAPIPlaceholders(player) }.getOrElse { updated }
}

fun <T> withPlayerPlaceholder(player: Player, updater: ((T, String) -> String)? = null): (T, String) -> String {
    return { binder, text ->
        val updated = updater?.invoke(binder, text) ?: text
        updated.replacePlayerPlaceholder(player)
    }
}
