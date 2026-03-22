package gg.aquatic.crates.data.hologram

import gg.aquatic.kholograms.line.AnimatedHologramLine
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("animated")
data class AnimatedCrateHologramLineData(
    val height: Double = 0.3,
    val frames: List<AnimatedHologramFrameData> = listOf(
        AnimatedHologramFrameData(20, TextCrateHologramLineData("<yellow>Frame 1")),
        AnimatedHologramFrameData(20, TextCrateHologramLineData("<gold>Frame 2"))
    ),
) : CrateHologramLineData() {
    override fun toSettings(): AnimatedHologramLine.Settings {
        return AnimatedHologramLine.Settings(
            frames = frames.map { it.stayTicks to it.line.toSettings() }.toMutableList(),
            height = height,
            filter = { true },
            failLine = null
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<AnimatedCrateHologramLineData>.defineEditor() {
            field(
                AnimatedCrateHologramLineData::height,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig(prompt = "Enter line height:", min = 0.0),
                displayName = "Height",
                description = listOf("Vertical space taken by this animated hologram line.")
            )
            list(
                AnimatedCrateHologramLineData::frames,
                displayName = "Frames",
                description = listOf("Frames played by this animated hologram line."),
                newValueFactory = HologramLineSelectionMenu.frameEntryFactory
            ) {
                with(AnimatedHologramFrameData) {
                    defineEditor()
                }
                defineHologramFrameLineEditor()
            }
        }
    }
}
