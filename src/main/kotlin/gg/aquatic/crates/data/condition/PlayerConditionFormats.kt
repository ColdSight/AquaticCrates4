package gg.aquatic.crates.data.condition

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.bukkit.Material

@OptIn(ExperimentalSerializationApi::class)
object PlayerConditionFormats {
    val module = SerializersModule {
        polymorphic(PlayerConditionData::class) {
            subclass(PermissionPlayerConditionData::class)
        }
    }

    val json = Json {
        serializersModule = module
        classDiscriminator = "type"
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
}

object PlayerConditionTypes {
    data class Definition(
        val id: String,
        val displayName: String,
        val description: List<String>,
        val icon: Material,
        val factory: () -> PlayerConditionData,
        val descriptorFactory: () -> SerialDescriptor,
    )

    val definitions: List<Definition> = listOf(
        Definition(
            id = "permission",
            displayName = "Permission",
            description = listOf(
                "Requires the player to have",
                "a specific permission node."
            ),
            icon = Material.TRIPWIRE_HOOK,
            factory = { PermissionPlayerConditionData() },
            descriptorFactory = { PermissionPlayerConditionData.serializer().descriptor }
        )
    )

    private val definitionsById = definitions.associateBy { it.id }

    fun definition(id: String): Definition? {
        return definitionsById[id]
    }

    fun create(id: String): PlayerConditionData? {
        return definition(id)?.factory?.invoke()
    }

    fun defaultElement(id: String): JsonElement? {
        val condition = create(id) ?: return null
        return PlayerConditionFormats.json.encodeToJsonElement(
            PolymorphicSerializer(PlayerConditionData::class),
            condition
        )
    }

    fun descriptor(id: String): SerialDescriptor? {
        return definition(id)?.descriptorFactory?.invoke()
    }
}
