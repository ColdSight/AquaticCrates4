package gg.aquatic.crates.data.range

import gg.aquatic.crates.reward.RewardAmountRange
import gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class RewardAmountRangeData(
    val min: Int = 1,
    val max: Int = 1,
    val chance: Double = 1.0,
) {
    fun normalized(): RewardAmountRangeData {
        val safeMin = min.coerceAtLeast(1)
        val safeMax = max.coerceAtLeast(1)
        return copy(
            min = safeMin,
            max = safeMax,
            chance = chance.coerceAtLeast(0.0)
        )
    }

    fun toRange(): RewardAmountRange {
        val normalized = normalized()
        return RewardAmountRange(
            min = normalized.min,
            max = normalized.max,
            chance = normalized.chance
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<RewardAmountRangeData>.defineEditor(
            minLabel: String = "Min",
            maxLabel: String = "Max",
            chanceLabel: String = "Weight",
        ) {
            field(
                RewardAmountRangeData::min,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter minimum amount:", min = 1),
                displayName = minLabel,
                iconMaterial = Material.GREEN_DYE,
                description = listOf("Minimum rolled amount for this range.")
            )
            field(
                RewardAmountRangeData::max,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter maximum amount:", min = 1),
                displayName = maxLabel,
                iconMaterial = Material.RED_DYE,
                description = listOf("Maximum rolled amount for this range.")
            )
            field(
                RewardAmountRangeData::chance,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter range weight:", min = 0.0),
                displayName = chanceLabel,
                iconMaterial = Material.EMERALD,
                description = listOf("Relative weight used when rolling this range.")
            )
        }
    }
}
