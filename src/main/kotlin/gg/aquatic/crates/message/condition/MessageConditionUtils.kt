package gg.aquatic.crates.message.condition

import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal fun EditorFieldContext.matchesMessageConditionSubtype(id: String): Boolean {
    val currentType = findMessageConditionSubtypeId() ?: return false
    return currentType.equals(id, ignoreCase = true)
}

internal fun EditorFieldContext.findMessageConditionSubtypeId(): String? {
    val direct = (value as? JsonObject)?.get("type")?.let { it as? JsonPrimitive }?.contentOrNull
    if (direct != null) {
        return direct
    }

    val numericIndex = pathSegments.indexOfLast { it.toIntOrNull() != null }
    if (numericIndex == -1) {
        return null
    }

    val element = root.findByPath(pathSegments.take(numericIndex + 1)) as? JsonObject ?: return null
    return element["type"]?.let { it as? JsonPrimitive }?.contentOrNull
}

private fun JsonElement.findByPath(path: List<String>): JsonElement? {
    var current: JsonElement = this
    for (segment in path) {
        current = when (val index = segment.toIntOrNull()) {
            null -> (current as? JsonObject)?.get(segment) ?: return null
            else -> (current as? JsonArray)?.getOrNull(index) ?: return null
        }
    }
    return current
}
