package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.WeekOfYearModuloCondition
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("week-of-year-mod")
data class WeekOfYearModuloPlayerConditionData(
    val modulo: Int = 3,
    val equals: Int = 0,
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        val normalizedModulo = modulo.coerceAtLeast(1)
        val normalizedEquals = equals.coerceIn(0, normalizedModulo - 1)
        return ConditionHandle(
            WeekOfYearModuloCondition,
            ObjectArguments(
                mapOf(
                    "modulo" to normalizedModulo,
                    "equals" to normalizedEquals
                )
            )
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<WeekOfYearModuloPlayerConditionData>.defineEditor() {
            field(
                WeekOfYearModuloPlayerConditionData::modulo,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter modulo:", min = 1),
                displayName = "Modulo",
                description = listOf("Divisor used for week-of-year modulo calculation.")
            )
            field(
                WeekOfYearModuloPlayerConditionData::equals,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter expected value:", min = 0),
                displayName = "Equals",
                description = listOf("Expected result of week-of-year % modulo.")
            )
        }
    }
}
