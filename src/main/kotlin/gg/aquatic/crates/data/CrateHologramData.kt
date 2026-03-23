package gg.aquatic.crates.data

import gg.aquatic.crates.data.hologram.CrateHologramLineData
import gg.aquatic.crates.data.hologram.HologramLineSelectionMenu
import gg.aquatic.crates.data.hologram.defineHologramLineEditor
import gg.aquatic.kholograms.Hologram
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class CrateHologramData(
    val lines: List<@Polymorphic CrateHologramLineData> = emptyList(),
    val viewDistance: Int = 20,
) {

    fun toSettings(): Hologram.Settings? {
        if (lines.isEmpty()) return null

        return Hologram.Settings(
            lines = lines.map { it.toSettings() },
            filter = { true },
            viewDistance = viewDistance
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<CrateHologramData>.defineEditor() {
            list(
                CrateHologramData::lines,
                "Hologram Lines",
                iconMaterial = Material.END_CRYSTAL,
                description = listOf("All hologram lines displayed above the crate, including text, item and animated lines."),
                newValueFactory = HologramLineSelectionMenu.entryFactory
            ) {
                defineHologramLineEditor()
            }
            field(
                CrateHologramData::viewDistance,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter hologram view distance:", min = 1),
                displayName = "Hologram View Distance",
                iconMaterial = Material.SPYGLASS,
                description = listOf("Maximum distance where the hologram is rendered.")
            )
        }
    }
}
