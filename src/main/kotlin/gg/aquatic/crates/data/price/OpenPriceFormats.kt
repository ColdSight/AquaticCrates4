package gg.aquatic.crates.data.price

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
object OpenPriceFormats {
    val module = SerializersModule {
        polymorphic(OpenPriceData::class) {
            subclass(CrateKeyOpenPriceData::class)
            subclass(VaultOpenPriceData::class)
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

object OpenPriceTypes {
    data class Definition(
        val id: String,
        val displayName: String,
        val description: List<String>,
        val icon: Material,
        val factory: () -> OpenPriceData,
        val descriptorFactory: () -> SerialDescriptor,
    )

    val definitions: List<Definition> = listOf(
        Definition(
            id = "crate-key",
            displayName = "Crate Key",
            description = listOf(
                "Consumes the key for this crate.",
                "This is the default open price."
            ),
            icon = Material.TRIPWIRE_HOOK,
            factory = { CrateKeyOpenPriceData() },
            descriptorFactory = { CrateKeyOpenPriceData.serializer().descriptor }
        ),
        Definition(
            id = "vault",
            displayName = "Vault Money",
            description = listOf(
                "Consumes money through Vault.",
                "Uses the server economy provider."
            ),
            icon = Material.GOLD_INGOT,
            factory = { VaultOpenPriceData() },
            descriptorFactory = { VaultOpenPriceData.serializer().descriptor }
        )
    )

    private val definitionsById = definitions.associateBy { it.id }

    fun definition(id: String): Definition? = definitionsById[id]

    fun descriptor(id: String): SerialDescriptor? {
        return definition(id)?.descriptorFactory?.invoke()
    }

    fun defaultElement(id: String): JsonElement? {
        val price = definition(id)?.factory?.invoke() ?: return null
        return OpenPriceFormats.json.encodeToJsonElement(
            PolymorphicSerializer(OpenPriceData::class),
            price
        )
    }
}
