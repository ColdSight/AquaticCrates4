package gg.aquatic.crates.data.condition

import gg.aquatic.execute.condition.ConditionHandle
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
abstract class PlayerConditionData {
    open fun toConditionHandle(): ConditionHandle<Player> {
        error("${this::class.simpleName} is an open-only condition and cannot be used as a player condition.")
    }

    open fun toOpenConditionHandle(): CrateOpenConditionHandle {
        return CrateOpenConditionHandle.fromPlayer(toConditionHandle())
    }
}
