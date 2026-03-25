package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.DayOfMonthCondition
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
@SerialName("day-of-month")
data class DayOfMonthPlayerConditionData(
    val days: List<Int> = listOf(1),
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        val normalizedDays = days.filter { it in 1..31 }.distinct()
        return ConditionHandle(
            DayOfMonthCondition,
            ObjectArguments(mapOf("days" to normalizedDays))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<DayOfMonthPlayerConditionData>.defineEditor() {
            list(
                DayOfMonthPlayerConditionData::days,
                displayName = "Days",
                iconMaterial = Material.CLOCK,
                description = listOf("Allowed days of month for this condition."),
                newValueFactory = EditorEntryFactories.int(
                    prompt = "Enter day of month or range:",
                    min = 1,
                    max = 31,
                    unique = true
                )
            )
        }
    }
}
