package gg.aquatic.crates.data

import gg.aquatic.crates.limit.LimitHandle
import gg.aquatic.crates.stats.CrateStatsTimeframe
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class LimitData(
    val timeframe: CrateStatsTimeframe = CrateStatsTimeframe.DAY,
    val limit: Int = 1,
) {
    fun normalized(): LimitData {
        return copy(limit = limit.coerceAtLeast(1))
    }

    fun toHandle(): LimitHandle {
        val normalized = normalized()
        return LimitHandle(
            timeframe = normalized.timeframe,
            limit = normalized.limit,
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<LimitData>.defineEditor() {
            field(
                LimitData::timeframe,
                displayName = "Timeframe",
                iconMaterial = Material.CLOCK,
                description = listOf("Which rolling timeframe this limit applies to.")
            )
            field(
                LimitData::limit,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter limit amount:", min = 1),
                displayName = "Limit",
                iconMaterial = Material.BARRIER,
                description = listOf("Maximum allowed wins/opens for this timeframe.")
            )
        }
    }
}
