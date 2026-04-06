package gg.aquatic.crates.data.interactable

import gg.aquatic.clientside.serialize.ClientsideEntitySettings
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.EntityType

@Serializable
@SerialName("entity")
data class EntityCrateInteractableData(
    val entityType: String = EntityType.ARMOR_STAND.name,
    val viewRange: Int = 50,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val offsetZ: Double = 0.0,
) : CrateInteractableData() {

    override fun toSettings() = ClientsideEntitySettings(
        entityType = runCatching { EntityType.valueOf(entityType.uppercase()) }.getOrDefault(EntityType.ARMOR_STAND),
        viewRange = viewRange,
        offsetX = offsetX,
        offsetY = offsetY,
        offsetZ = offsetZ
    )

    companion object {
        fun TypedNestedSchemaBuilder<EntityCrateInteractableData>.defineEditor() {
            field(
                EntityCrateInteractableData::entityType,
                EnumFieldAdapter,
                EnumFieldConfig(
                    prompt = "Enter entity type:",
                    values = { EntityType.entries.map { it.name } }
                ),
                displayName = "Entity Type",
                description = listOf("Entity type used for this clientside interactable.")
            )
            field(
                EntityCrateInteractableData::viewRange,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter interactable view range:", min = 1),
                displayName = "View Range",
                description = listOf("Maximum distance where this clientside interactable stays visible.")
            )
            defineInteractableOffsetEditor(
                EntityCrateInteractableData::offsetX,
                EntityCrateInteractableData::offsetY,
                EntityCrateInteractableData::offsetZ
            )
        }
    }
}
