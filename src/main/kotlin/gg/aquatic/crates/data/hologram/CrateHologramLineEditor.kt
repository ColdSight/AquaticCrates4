package gg.aquatic.crates.data.hologram

import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.json.*

fun TypedNestedSchemaBuilder<CrateHologramLineData>.defineHologramLineEditor() {
    fieldPattern(
        displayName = "Hologram Line",
        adapter = HologramLineEntryFieldAdapter,
        description = listOf(
            "Left click to edit this hologram line.",
            "Right click to change its line type."
        )
    )
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
    fieldPattern(
        "line",
        displayName = "Animation Frame",
        adapter = HologramFrameLineEntryFieldAdapter,
        description = listOf(
            "Left click to edit this animation frame.",
            "Right click to change its frame line type."
        )
    )
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
