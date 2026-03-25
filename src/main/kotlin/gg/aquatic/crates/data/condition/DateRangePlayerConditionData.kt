package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.DateRangeCondition
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
@SerialName("date-range")
data class DateRangePlayerConditionData(
    val start: String = "2026-01-01",
    val end: String = "2026-12-31",
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        return ConditionHandle(
            DateRangeCondition,
            ObjectArguments(mapOf("start" to start, "end" to end))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<DateRangePlayerConditionData>.defineEditor() {
            field(
                DateRangePlayerConditionData::start,
                TextFieldAdapter,
                TextFieldConfig(
                    prompt = "Enter start date (YYYY-MM-DD):",
                    validator = ::validateDate
                ),
                displayName = "Start Date",
                description = listOf("Inclusive start date for this condition.")
            )
            field(
                DateRangePlayerConditionData::end,
                TextFieldAdapter,
                TextFieldConfig(
                    prompt = "Enter end date (YYYY-MM-DD):",
                    validator = ::validateDate
                ),
                displayName = "End Date",
                description = listOf("Inclusive end date for this condition.")
            )
        }

        private fun validateDate(raw: String): String? {
            return if (runCatching { LocalDate.parse(raw.trim()) }.isSuccess) null else "Use YYYY-MM-DD format."
        }
    }
}
