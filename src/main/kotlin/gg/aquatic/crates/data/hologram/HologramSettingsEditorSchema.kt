package gg.aquatic.crates.data.hologram

import gg.aquatic.crates.data.CrateHologramData
import gg.aquatic.waves.serialization.editor.meta.EditableModel
import gg.aquatic.waves.serialization.editor.meta.TypedEditorSchemaBuilder

object HologramSettingsEditorSchema : EditableModel<CrateHologramData>(CrateHologramData.serializer()) {
    override fun TypedEditorSchemaBuilder<CrateHologramData>.define() {
        include<CrateHologramData> {
            with(CrateHologramData) { defineEditor() }
        }
    }
}
