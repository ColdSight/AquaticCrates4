package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.TimeRangeCondition
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalTime

@Serializable
@SerialName("time-range")
data class TimeRangePlayerConditionData(
    val start: String = "09:00",
    val end: String = "18:00",
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        return ConditionHandle(
            TimeRangeCondition,
            ObjectArguments(mapOf("start" to start, "end" to end))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<TimeRangePlayerConditionData>.defineEditor() {
            field(
                TimeRangePlayerConditionData::start,
                TextFieldAdapter,
                TextFieldConfig(
                    prompt = "Enter start time (HH:mm):",
                    validator = ::validateTime
                ),
                displayName = "Start",
                description = listOf("Inclusive start of the allowed time range.")
            )
            field(
                TimeRangePlayerConditionData::end,
                TextFieldAdapter,
                TextFieldConfig(
                    prompt = "Enter end time (HH:mm):",
                    validator = ::validateTime
                ),
                displayName = "End",
                description = listOf("Inclusive end of the allowed time range.")
            )
        }

        private fun validateTime(raw: String): String? {
            return if (runCatching { LocalTime.parse(raw.trim()) }.isSuccess) null else "Use HH:mm format."
        }
    }
}
