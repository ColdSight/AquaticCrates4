package gg.aquatic.crates.data.interaction

import gg.aquatic.crates.data.editor.findPolymorphicSubtypeId
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext

internal fun EditorFieldContext.matchesCrateClickActionSubtype(id: String): Boolean {
    val currentType = findCrateClickActionSubtypeId() ?: return false
    return currentType.equals(id, ignoreCase = true)
}

internal fun EditorFieldContext.findCrateClickActionSubtypeId(): String? {
    return findPolymorphicSubtypeId()
}
