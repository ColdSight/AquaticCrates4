package gg.aquatic.crates.data.hologram

import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.kholograms.line.ItemHologramLine
import gg.aquatic.waves.serialization.editor.meta.EnumFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.EnumFieldConfig
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Display
import org.bukkit.entity.ItemDisplay
import org.joml.Vector3f

@Serializable
@SerialName("item")
data class ItemCrateHologramLineData(
    val item: StackedItemData = StackedItemData(material = org.bukkit.Material.DIAMOND.name),
    val height: Double = 0.3,
    val scale: Double = 1.0,
    val billboard: String = Display.Billboard.CENTER.name,
    val itemDisplayTransform: String = ItemDisplay.ItemDisplayTransform.GROUND.name,
    val transformationDuration: Int = 0,
    val teleportInterpolation: Int = 0,
    val translationX: Double = 0.0,
    val translationY: Double = 0.0,
    val translationZ: Double = 0.0,
) : CrateHologramLineData() {
    override fun toSettings(): ItemHologramLine.Settings {
        return ItemHologramLine.Settings(
            item = item.asStacked().getItem(),
            height = height,
            scale = scale.toFloat(),
            billboard = runCatching { Display.Billboard.valueOf(billboard) }.getOrDefault(Display.Billboard.CENTER),
            itemDisplayTransform = runCatching { ItemDisplay.ItemDisplayTransform.valueOf(itemDisplayTransform) }
                .getOrDefault(ItemDisplay.ItemDisplayTransform.GROUND),
            filter = { true },
            failLine = null,
            transformationDuration = transformationDuration,
            teleportInterpolation = teleportInterpolation,
            translation = Vector3f(translationX.toFloat(), translationY.toFloat(), translationZ.toFloat())
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<ItemCrateHologramLineData>.defineEditor() {
            group(ItemCrateHologramLineData::item) {
                with(StackedItemData) {
                    defineBasicEditor(
                        materialLabel = "Item Material",
                        nameLabel = "Item Name",
                        namePrompt = "Enter hologram item display name:",
                        loreLabel = "Item Lore",
                        amountLabel = "Item Amount"
                    )
                }
            }
            field(
                ItemCrateHologramLineData::height,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig(prompt = "Enter line height:", min = 0.0),
                displayName = "Height",
                description = listOf("Vertical space taken by this hologram line.")
            )
            field(
                ItemCrateHologramLineData::scale,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig(prompt = "Enter scale:", min = 0.0),
                displayName = "Scale",
                description = listOf("Display scale applied to this item line.")
            )
            field(
                ItemCrateHologramLineData::billboard,
                EnumFieldAdapter,
                EnumFieldConfig(prompt = "Enter billboard mode:", values = { Display.Billboard.entries.map { it.name } }),
                displayName = "Billboard",
                description = listOf("Billboard mode used by the display entity.")
            )
            field(
                ItemCrateHologramLineData::itemDisplayTransform,
                EnumFieldAdapter,
                EnumFieldConfig(prompt = "Enter item display transform:", values = { ItemDisplay.ItemDisplayTransform.entries.map { it.name } }),
                displayName = "Item Transform",
                description = listOf("Display transform used for the item display entity.")
            )
            field(
                ItemCrateHologramLineData::transformationDuration,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter transformation duration:", min = 0),
                displayName = "Transformation Duration",
                description = listOf("Interpolation duration for transformation changes.")
            )
            field(
                ItemCrateHologramLineData::teleportInterpolation,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter teleport interpolation:", min = 0),
                displayName = "Teleport Interpolation",
                description = listOf("Interpolation duration used when the line moves.")
            )
            field(
                ItemCrateHologramLineData::translationX,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig(prompt = "Enter translation X:"),
                displayName = "Translation X",
                description = listOf("X translation offset of the display entity.")
            )
            field(
                ItemCrateHologramLineData::translationY,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig(prompt = "Enter translation Y:"),
                displayName = "Translation Y",
                description = listOf("Y translation offset of the display entity.")
            )
            field(
                ItemCrateHologramLineData::translationZ,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter,
                gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig(prompt = "Enter translation Z:"),
                displayName = "Translation Z",
                description = listOf("Z translation offset of the display entity.")
            )
        }
    }
}
