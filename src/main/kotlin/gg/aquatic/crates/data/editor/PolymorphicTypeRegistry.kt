package gg.aquatic.crates.data.editor

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import com.charleskorn.kaml.YamlNode
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
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
    private val yaml: Yaml,
    definitions: List<PolymorphicTypeDefinition<T>>,
) {
    val definitions: List<PolymorphicTypeDefinition<T>> = definitions
    private val definitionsById = definitions.associateBy { it.id }

    fun definition(id: String): PolymorphicTypeDefinition<T>? = definitionsById[id]
    fun create(id: String): T? = definition(id)?.factory?.invoke()
    fun descriptor(id: String): SerialDescriptor? = definition(id)?.descriptorFactory?.invoke()
    fun parse(raw: String): String? = definitionsById.keys.firstOrNull { it.equals(raw.trim(), ignoreCase = true) }
    fun selectionDefinitions(): List<PolymorphicSelectionMenu.Definition> = definitions.map { definition ->
        PolymorphicSelectionMenu.Definition(
            id = definition.id,
            displayName = definition.displayName,
            description = definition.description,
            icon = definition.icon
        )
    }

    fun defaultElement(id: String): YamlNode? {
        val element = create(id) ?: return null
        return yaml.encodeToNode(
            PolymorphicSerializer(baseClass.kotlin),
            element
        )
    }
}

fun createPolymorphicYaml(module: kotlinx.serialization.modules.SerializersModule): Yaml = Yaml(
    serializersModule = module,
    configuration = YamlConfiguration(
        yamlNamingStrategy = YamlNamingStrategy.KebabCase,
        polymorphismStyle = PolymorphismStyle.Property,
        polymorphismPropertyName = "type"
    )
)
