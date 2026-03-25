package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.MonthCondition
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material
import java.time.Month

@Serializable
@SerialName("month")
data class MonthPlayerConditionData(
    val months: List<String> = listOf(Month.JANUARY.name),
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        val normalizedMonths = months
            .mapNotNull { raw -> runCatching { Month.valueOf(raw.trim().uppercase()) }.getOrNull()?.name }
            .distinct()
        return ConditionHandle(
            MonthCondition,
            ObjectArguments(mapOf("months" to normalizedMonths))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<MonthPlayerConditionData>.defineEditor() {
            list(
                MonthPlayerConditionData::months,
                displayName = "Months",
                iconMaterial = Material.CLOCK,
                description = listOf("Allowed months for this condition."),
                newValueFactory = EditorEntryFactories.text(
                    prompt = "Enter month:",
                    validator = {
                        if (runCatching { Month.valueOf(it.trim().uppercase()) }.isSuccess) null
                        else "Use a valid month like JANUARY."
                    }
                )
            )
        }
    }
}
