package gg.aquatic.crates.data.hologram

import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

fun TypedNestedSchemaBuilder<CrateHologramLineData>.defineHologramLineEditor() {
    include<TextCrateHologramLineData>(visibleWhen = { it.matchesHologramLineSubtype("text") }) {
        with(TextCrateHologramLineData) {
            defineEditor()
        }
    }
    include<ItemCrateHologramLineData>(visibleWhen = { it.matchesHologramLineSubtype("item") }) {
        with(ItemCrateHologramLineData) {
            defineEditor()
        }
    }
    include<AnimatedCrateHologramLineData>(visibleWhen = { it.matchesHologramLineSubtype("animated") }) {
        with(AnimatedCrateHologramLineData) {
            defineEditor()
        }
    }
}

fun TypedNestedSchemaBuilder<AnimatedHologramFrameData>.defineHologramFrameLineEditor() {
    include<TextCrateHologramLineData>(visibleWhen = { it.matchesHologramLineSubtype("text") }) {
        group(AnimatedHologramFrameData::line) {
            with(TextCrateHologramLineData) { defineEditor() }
        }
    }
    include<ItemCrateHologramLineData>(visibleWhen = { it.matchesHologramLineSubtype("item") }) {
        group(AnimatedHologramFrameData::line) {
            with(ItemCrateHologramLineData) { defineEditor() }
        }
    }
}

internal fun EditorFieldContext.matchesHologramLineSubtype(id: String): Boolean {
    return findHologramLineSubtypeId()?.equals(id, ignoreCase = true) == true
}

internal fun EditorFieldContext.findHologramLineSubtypeId(): String? {
    val direct = (value as? JsonObject)?.get("type")?.let { it as? JsonPrimitive }?.contentOrNull
    if (direct != null) return direct

    var current: JsonElement = root
    var candidate: String? = null
    for (segment in pathSegments) {
        current = when (val index = segment.toIntOrNull()) {
            null -> {
                val obj = current as? JsonObject ?: return candidate
                obj[segment] ?: return candidate
            }
            else -> {
                val arr = current as? JsonArray ?: return candidate
                arr.getOrNull(index) ?: return candidate
            }
        }

        val currentType = (current as? JsonObject)?.get("type")
            ?.let { it as? JsonPrimitive }
            ?.contentOrNull
        if (currentType != null) {
            candidate = currentType
        }
    }

    return candidate
}
