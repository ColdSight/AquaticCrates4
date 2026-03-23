package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.Condition
import gg.aquatic.execute.condition.ConditionHandle
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
abstract class PlayerConditionData {
    abstract fun toConditionHandle(): ConditionHandle<Player>
}
