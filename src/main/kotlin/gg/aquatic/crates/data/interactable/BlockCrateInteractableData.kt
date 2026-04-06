package gg.aquatic.crates.data.interactable

import gg.aquatic.clientside.serialize.ClientsideBlockSettings
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("block")
data class BlockCrateInteractableData(
    val block: BlockDefinitionData = BlockDefinitionData(),
    val viewRange: Int = 50,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val offsetZ: Double = 0.0,
) : CrateInteractableData() {

    override fun toSettings() = ClientsideBlockSettings(
        block = block.toBlokk(),
        viewRange = viewRange,
        offsetX = offsetX,
        offsetY = offsetY,
        offsetZ = offsetZ
    )

    companion object {
        fun TypedNestedSchemaBuilder<BlockCrateInteractableData>.defineEditor() {
            group(BlockCrateInteractableData::block) {
                with(BlockDefinitionData) {
                    defineEditor(
                        titlePrefix = "Block",
                        materialDescription = listOf("Single clientside block shown for this crate interactable.")
                    )
                }
            }
            field(
                BlockCrateInteractableData::viewRange,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter interactable view range:", min = 1),
                displayName = "View Range",
                description = listOf("Maximum distance where this clientside interactable stays visible.")
            )
            defineInteractableOffsetEditor(
                BlockCrateInteractableData::offsetX,
                BlockCrateInteractableData::offsetY,
                BlockCrateInteractableData::offsetZ
            )
        }
    }
}
