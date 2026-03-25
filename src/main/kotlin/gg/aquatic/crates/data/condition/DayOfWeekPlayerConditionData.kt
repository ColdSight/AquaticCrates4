package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.DayOfWeekCondition
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material
import java.time.DayOfWeek

@Serializable
@SerialName("day-of-week")
data class DayOfWeekPlayerConditionData(
    val days: List<String> = listOf(DayOfWeek.MONDAY.name),
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        val normalizedDays = days
            .mapNotNull { raw -> runCatching { DayOfWeek.valueOf(raw.trim().uppercase()) }.getOrNull()?.name }
            .distinct()
        return ConditionHandle(
            DayOfWeekCondition,
            ObjectArguments(mapOf("days" to normalizedDays))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<DayOfWeekPlayerConditionData>.defineEditor() {
            list(
                DayOfWeekPlayerConditionData::days,
                displayName = "Days",
                iconMaterial = Material.CLOCK,
                description = listOf("Allowed weekdays for this condition."),
                newValueFactory = EditorEntryFactories.text(
                    prompt = "Enter day of week:",
                    validator = {
                        if (runCatching { DayOfWeek.valueOf(it.trim().uppercase()) }.isSuccess) null
                        else "Use a valid day like MONDAY."
                    }
                )
            )
        }
    }
}
