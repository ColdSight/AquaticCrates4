package gg.aquatic.crates.data.interaction

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
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
object CrateClickActionFormats {
    val module = SerializersModule {
        polymorphic(CrateClickActionData::class) {
            subclass(PreviewCrateClickActionData::class)
            subclass(OpenCrateClickActionData::class)
            subclass(DestroyCrateClickActionData::class)
            subclass(MessageCrateClickActionData::class)
            subclass(ActionbarCrateClickActionData::class)
            subclass(CommandCrateClickActionData::class)
            subclass(SoundCrateClickActionData::class)
            subclass(StopSoundCrateClickActionData::class)
            subclass(TitleCrateClickActionData::class)
            subclass(CloseInventoryCrateClickActionData::class)
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

    val yaml = Yaml(
        serializersModule = module,
        configuration = YamlConfiguration(
            yamlNamingStrategy = YamlNamingStrategy.KebabCase,
            polymorphismStyle = PolymorphismStyle.Property,
            polymorphismPropertyName = "type"
        )
    )
}

object CrateClickActionTypes {
    data class Definition(
        val id: String,
        val displayName: String,
        val description: List<String>,
        val icon: Material,
        val factory: () -> CrateClickActionData,
        val descriptorFactory: () -> SerialDescriptor,
    )

    val definitions: List<Definition> = listOf(
        Definition(
            id = "preview",
            displayName = "Preview",
            description = listOf("Opens the crate preview menu."),
            icon = Material.ENDER_EYE,
            factory = { PreviewCrateClickActionData() },
            descriptorFactory = { PreviewCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "open",
            displayName = "Open",
            description = listOf("Attempts to open the crate."),
            icon = Material.CHEST,
            factory = { OpenCrateClickActionData() },
            descriptorFactory = { OpenCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "destroy",
            displayName = "Destroy",
            description = listOf("Destroys the placed crate.", "Only useful for direct crate interaction."),
            icon = Material.BARRIER,
            factory = { DestroyCrateClickActionData() },
            descriptorFactory = { DestroyCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "message",
            displayName = "Message",
            description = listOf("Sends one or more chat lines", "to the clicking player."),
            icon = Material.PAPER,
            factory = { MessageCrateClickActionData() },
            descriptorFactory = { MessageCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "actionbar",
            displayName = "Actionbar",
            description = listOf("Shows a short actionbar message", "above the hotbar."),
            icon = Material.NAME_TAG,
            factory = { ActionbarCrateClickActionData() },
            descriptorFactory = { ActionbarCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "command",
            displayName = "Command",
            description = listOf("Executes one or more commands", "as console or player."),
            icon = Material.COMMAND_BLOCK,
            factory = { CommandCrateClickActionData() },
            descriptorFactory = { CommandCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "sound",
            displayName = "Play Sound",
            description = listOf("Plays a sound with custom", "volume and pitch."),
            icon = Material.NOTE_BLOCK,
            factory = { SoundCrateClickActionData() },
            descriptorFactory = { SoundCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "stop-sound",
            displayName = "Stop Sound",
            description = listOf("Stops a specific sound", "for the clicking player."),
            icon = Material.JUKEBOX,
            factory = { StopSoundCrateClickActionData() },
            descriptorFactory = { StopSoundCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "title",
            displayName = "Title",
            description = listOf("Shows a title and subtitle", "with timing settings."),
            icon = Material.BOOK,
            factory = { TitleCrateClickActionData() },
            descriptorFactory = { TitleCrateClickActionData.serializer().descriptor }
        ),
        Definition(
            id = "close-inventory",
            displayName = "Close Inventory",
            description = listOf("Closes the current inventory", "for the clicking player."),
            icon = Material.CHEST_MINECART,
            factory = { CloseInventoryCrateClickActionData() },
            descriptorFactory = { CloseInventoryCrateClickActionData.serializer().descriptor }
        ),
    )

    private val definitionsById = definitions.associateBy { it.id }

    fun definition(id: String): Definition? = definitionsById[id]
    fun create(id: String): CrateClickActionData? = definition(id)?.factory?.invoke()
    fun descriptor(id: String): SerialDescriptor? = definition(id)?.descriptorFactory?.invoke()

    fun defaultElement(id: String): JsonElement? {
        val action = create(id) ?: return null
        return CrateClickActionFormats.json.encodeToJsonElement(
            PolymorphicSerializer(CrateClickActionData::class),
            action
        )
    }
}
