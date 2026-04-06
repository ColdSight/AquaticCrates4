package gg.aquatic.crates.interact

import org.bukkit.entity.Player

enum class CrateClickType {
    RIGHT,
    SHIFT_RIGHT,
    LEFT,
    SHIFT_LEFT;

    companion object {
        fun fromInteraction(isLeft: Boolean, player: Player): CrateClickType {
            return when {
                isLeft && player.isSneaking -> SHIFT_LEFT
                isLeft -> LEFT
                player.isSneaking -> SHIFT_RIGHT
                else -> RIGHT
            }
        }
    }
}