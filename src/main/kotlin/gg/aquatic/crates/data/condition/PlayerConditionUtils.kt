package gg.aquatic.crates.data.condition

import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import kotlinx.serialization.json.*

internal fun EditorFieldContext.matchesConditionSubtype(id: String): Boolean {
    val currentType = findConditionSubtypeId() ?: return false
    return currentType.equals(id, ignoreCase = true)
}

internal fun EditorFieldContext.findConditionSubtypeId(): String? {
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

    val conditionElement = root.findConditionByPath(pathSegments.take(numericIndex + 1)) as? JsonObject ?: return null
    return conditionElement["type"]
        ?.let { it as? JsonPrimitive }
        ?.contentOrNull
}

private fun JsonElement.findConditionByPath(path: List<String>): JsonElement? {
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
