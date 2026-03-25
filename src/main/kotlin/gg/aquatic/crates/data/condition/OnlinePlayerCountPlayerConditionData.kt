package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.OnlinePlayerCountCondition
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
@SerialName("online-player-count")
data class OnlinePlayerCountPlayerConditionData(
    val min: Int = 0,
    val max: Int? = null,
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        return ConditionHandle(
            OnlinePlayerCountCondition,
            ObjectArguments(
                mapOf(
                    "min" to min.coerceAtLeast(0),
                    "max" to max?.coerceAtLeast(0)
                )
            )
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<OnlinePlayerCountPlayerConditionData>.defineEditor() {
            field(
                OnlinePlayerCountPlayerConditionData::min,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter minimum online player count:", min = 0),
                displayName = "Min Players",
                iconMaterial = Material.PLAYER_HEAD,
                description = listOf("Minimum number of online players required.")
            )
            field(
                OnlinePlayerCountPlayerConditionData::max,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter maximum online player count:", min = 0),
                displayName = "Max Players",
                iconMaterial = Material.SKELETON_SKULL,
                description = listOf(
                    "Optional maximum number of online players allowed.",
                    "Press Q to clear it back to null."
                )
            )
        }
    }
}
