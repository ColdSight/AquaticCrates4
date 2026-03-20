package gg.aquatic.crates.data.hologram

import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable

@Serializable
data class AnimatedHologramFrameData(
    val stayTicks: Int = 20,
    val line: @Polymorphic CrateHologramLineData = TextCrateHologramLineData(),
) {
    companion object {
        fun TypedNestedSchemaBuilder<AnimatedHologramFrameData>.defineEditor() {
            field(
                AnimatedHologramFrameData::stayTicks,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter frame stay ticks:", min = 1),
                displayName = "Stay Ticks",
                description = listOf("How many ticks this frame stays visible before the animation advances.")
            )
        }
    }
}
