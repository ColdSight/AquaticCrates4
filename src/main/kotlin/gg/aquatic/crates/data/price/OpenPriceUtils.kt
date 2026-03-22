package gg.aquatic.crates.data.price

import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun EditorFieldContext.matchesSubtype(id: String): Boolean {
    val currentType = findOpenPriceSubtypeId() ?: return false
    return currentType.equals(id, ignoreCase = true)
}

internal fun EditorFieldContext.findOpenPriceSubtypeId(): String? {
    val direct = (value as? JsonObject)
        ?.get("type")
        ?.let { it as? JsonPrimitive }
        ?.contentOrNull
    if (direct != null) {
        return direct
    }

    val numericIndex = pathSegments.indexOfLast { it.toIntOrNull() != null }
    if (numericIndex == -1) {
        return null
    }

    val priceElement = root.findByPath(pathSegments.take(numericIndex + 1)) as? JsonObject ?: return null
    return priceElement["type"]
        ?.let { it as? JsonPrimitive }
        ?.contentOrNull
}

private fun JsonElement.findByPath(path: List<String>): JsonElement? {
    var current: JsonElement = this
    for (segment in path) {
        current = when (val index = segment.toIntOrNull()) {
            null -> {
                if (current !is JsonObject) return null
                current[segment] ?: return null
            }

            else -> {
                if (current !is JsonArray) return null
                current.getOrNull(index) ?: return null
            }
        }
    }

    return current
}
