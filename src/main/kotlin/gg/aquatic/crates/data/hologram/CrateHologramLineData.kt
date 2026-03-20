package gg.aquatic.crates.data.hologram

import gg.aquatic.kholograms.serialize.LineSettings
import kotlinx.serialization.Serializable

@Serializable
abstract class CrateHologramLineData {
    abstract fun toSettings(): LineSettings
}
