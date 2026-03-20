package gg.aquatic.crates.data.condition

import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder

fun TypedNestedSchemaBuilder<PlayerConditionData>.definePlayerConditionEditor() {
    include<PermissionPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("permission") }) {
        with(PermissionPlayerConditionData) {
            defineEditor()
        }
    }
}
