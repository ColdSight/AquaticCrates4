package gg.aquatic.crates.data.interactable

import gg.aquatic.crates.data.editor.findByPath
import gg.aquatic.crates.data.editor.mapValue
import gg.aquatic.crates.data.editor.stringContentOrNull
import gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import org.bukkit.Material

internal fun EditorFieldContext.findInteractableSubtypeId(): String? {
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

internal fun <T> TypedNestedSchemaBuilder<T>.defineInteractableOffsetEditor(
    offsetX: kotlin.reflect.KProperty1<T, Double>,
    offsetY: kotlin.reflect.KProperty1<T, Double>,
    offsetZ: kotlin.reflect.KProperty1<T, Double>,
) {
    field(
        offsetX,
        DoubleFieldAdapter,
        DoubleFieldConfig(prompt = "Enter offset X:"),
        displayName = "Offset X",
        iconMaterial = Material.ARROW,
        description = listOf("Offsets the interactable spawn position on the X axis.")
    )
    field(
        offsetY,
        DoubleFieldAdapter,
        DoubleFieldConfig(prompt = "Enter offset Y:"),
        displayName = "Offset Y",
        iconMaterial = Material.SPECTRAL_ARROW,
        description = listOf("Offsets the interactable spawn position on the Y axis.")
    )
    field(
        offsetZ,
        DoubleFieldAdapter,
        DoubleFieldConfig(prompt = "Enter offset Z:"),
        displayName = "Offset Z",
        iconMaterial = Material.TIPPED_ARROW,
        description = listOf("Offsets the interactable spawn position on the Z axis.")
    )
}
