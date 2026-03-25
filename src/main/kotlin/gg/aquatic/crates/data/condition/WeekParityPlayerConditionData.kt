package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.WeekParityCondition
import gg.aquatic.waves.serialization.editor.meta.EnumFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.EnumFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
@SerialName("week-parity")
data class WeekParityPlayerConditionData(
    val parity: String = "odd",
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        val normalized = if (parity.equals("even", true)) "even" else "odd"
        return ConditionHandle(
            WeekParityCondition,
            ObjectArguments(mapOf("parity" to normalized))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<WeekParityPlayerConditionData>.defineEditor() {
            field(
                WeekParityPlayerConditionData::parity,
                EnumFieldAdapter,
                EnumFieldConfig(
                    prompt = "Enter week parity:",
                    values = { listOf("odd", "even") }
                ),
                displayName = "Parity",
                iconMaterial = Material.COMPARATOR,
                description = listOf("Chooses whether odd or even ISO weeks should match.")
            )
        }
    }
}
