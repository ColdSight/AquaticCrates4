package gg.aquatic.crates.data

import gg.aquatic.waves.serialization.editor.meta.EditableModel

object CrateDataEditorSchema : EditableModel<CrateData>(CrateData.serializer()) {
    override fun resolveDescriptor(context: gg.aquatic.waves.serialization.editor.meta.EditorFieldContext) =
        resolveCrateDataDescriptor(context)

    override fun gg.aquatic.waves.serialization.editor.meta.TypedEditorSchemaBuilder<CrateData>.define() =
        defineCrateDataRootSchema()
}
