package gg.aquatic.crates.data.editor

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.bukkit.Material

data class PolymorphicTypeDefinition<T : Any>(
    val id: String,
    val displayName: String,
    val description: List<String>,
    val icon: Material,
    val factory: () -> T,
    val descriptorFactory: () -> SerialDescriptor,
)

class PolymorphicTypeRegistry<T : Any>(
    private val baseClass: Class<T>,
    private val json: Json,
    definitions: List<PolymorphicTypeDefinition<T>>,
) {
    val definitions: List<PolymorphicTypeDefinition<T>> = definitions
    private val definitionsById = definitions.associateBy { it.id }

    fun definition(id: String): PolymorphicTypeDefinition<T>? = definitionsById[id]
    fun create(id: String): T? = definition(id)?.factory?.invoke()
    fun descriptor(id: String): SerialDescriptor? = definition(id)?.descriptorFactory?.invoke()
    fun parse(raw: String): String? = definitionsById.keys.firstOrNull { it.equals(raw.trim(), ignoreCase = true) }

    fun defaultElement(id: String): JsonElement? {
        val element = create(id) ?: return null
        return json.encodeToJsonElement(
            PolymorphicSerializer(baseClass.kotlin),
            element
        )
    }
}

fun createPolymorphicJson(module: kotlinx.serialization.modules.SerializersModule): Json = Json {
    serializersModule = module
    classDiscriminator = "type"
    prettyPrint = true
    prettyPrintIndent = "  "
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun createPolymorphicYaml(module: kotlinx.serialization.modules.SerializersModule): Yaml = Yaml(
    serializersModule = module,
    configuration = YamlConfiguration(
        yamlNamingStrategy = YamlNamingStrategy.KebabCase,
        polymorphismStyle = PolymorphismStyle.Property,
        polymorphismPropertyName = "type"
    )
)
