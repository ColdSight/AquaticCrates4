package gg.aquatic.crates.data.price

import gg.aquatic.crates.data.editor.findByPath
import gg.aquatic.crates.data.editor.mapValue
import gg.aquatic.crates.data.editor.stringContentOrNull
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext

internal fun EditorFieldContext.matchesSubtype(id: String): Boolean {
    val currentType = findOpenPriceSubtypeId() ?: return false
    return currentType.equals(id, ignoreCase = true)
}

internal fun EditorFieldContext.findOpenPriceSubtypeId(): String? {
    val direct = value.mapValue("type")?.stringContentOrNull
    if (direct != null) {
        return direct
    }

    val numericIndex = pathSegments.indexOfLast { it.toIntOrNull() != null }
    if (numericIndex == -1) {
        return null
    }

    return root.findByPath(pathSegments.take(numericIndex + 1))
        ?.mapValue("type")
        ?.stringContentOrNull
}
