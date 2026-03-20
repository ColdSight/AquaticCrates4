package gg.aquatic.crates.data.action

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.bukkit.Material

@OptIn(ExperimentalSerializationApi::class)
object RewardActionFormats {
    val module = SerializersModule {
        polymorphic(RewardActionData::class) {
            subclass(GiveItemRewardActionData::class)
            subclass(MessageRewardActionData::class)
            subclass(ActionbarRewardActionData::class)
            subclass(CommandRewardActionData::class)
            subclass(SoundRewardActionData::class)
            subclass(StopSoundRewardActionData::class)
            subclass(TitleRewardActionData::class)
            subclass(CloseInventoryRewardActionData::class)
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

    val legacyYaml = Yaml.default
}

object RewardActionTypes {
    data class Definition(
        val id: String,
        val displayName: String,
        val description: List<String>,
        val icon: Material,
        val factory: () -> RewardActionData,
        val descriptorFactory: () -> SerialDescriptor,
    )

    val definitions: List<Definition> = listOf(
        Definition(
            id = "give-item",
            displayName = "Give Item",
            description = listOf(
                "Gives the configured item directly",
                "to the player inventory."
            ),
            icon = Material.DIAMOND,
            factory = { GiveItemRewardActionData() },
            descriptorFactory = { GiveItemRewardActionData.serializer().descriptor }
        ),
        Definition(
            id = "message",
            displayName = "Message",
            description = listOf(
                "Sends one or more chat lines",
                "to the winning player."
            ),
            icon = Material.PAPER,
            factory = { MessageRewardActionData() },
            descriptorFactory = { MessageRewardActionData.serializer().descriptor }
        ),
        Definition(
            id = "actionbar",
            displayName = "Actionbar",
            description = listOf(
                "Shows a short actionbar message",
                "above the hotbar."
            ),
            icon = Material.NAME_TAG,
            factory = { ActionbarRewardActionData() },
            descriptorFactory = { ActionbarRewardActionData.serializer().descriptor }
        ),
        Definition(
            id = "command",
            displayName = "Command",
            description = listOf(
                "Executes one or more commands",
                "as console or player."
            ),
            icon = Material.COMMAND_BLOCK,
            factory = { CommandRewardActionData() },
            descriptorFactory = { CommandRewardActionData.serializer().descriptor }
        ),
        Definition(
            id = "sound",
            displayName = "Play Sound",
            description = listOf(
                "Plays a sound with custom",
                "volume and pitch."
            ),
            icon = Material.NOTE_BLOCK,
            factory = { SoundRewardActionData() },
            descriptorFactory = { SoundRewardActionData.serializer().descriptor }
        ),
        Definition(
            id = "stop-sound",
            displayName = "Stop Sound",
            description = listOf(
                "Stops a specific sound for",
                "the winning player."
            ),
            icon = Material.BARRIER,
            factory = { StopSoundRewardActionData() },
            descriptorFactory = { StopSoundRewardActionData.serializer().descriptor }
        ),
        Definition(
            id = "title",
            displayName = "Title",
            description = listOf(
                "Shows a title and subtitle",
                "with timing settings."
            ),
            icon = Material.BOOK,
            factory = { TitleRewardActionData() },
            descriptorFactory = { TitleRewardActionData.serializer().descriptor }
        ),
        Definition(
            id = "close-inventory",
            displayName = "Close Inventory",
            description = listOf(
                "Closes the current inventory",
                "for the winning player."
            ),
            icon = Material.CHEST,
            factory = { CloseInventoryRewardActionData() },
            descriptorFactory = { CloseInventoryRewardActionData.serializer().descriptor }
        ),
    )
    private val definitionsById = definitions.associateBy { it.id }

    fun parse(raw: String): String? {
        return definitionsById.keys.firstOrNull { it.equals(raw.trim(), ignoreCase = true) }
    }

    fun definition(id: String): Definition? {
        return definitionsById[id]
    }

    fun create(id: String): RewardActionData? {
        return definition(id)?.factory?.invoke()
    }

    fun defaultElement(id: String): JsonElement? {
        val action = create(id) ?: return null
        return RewardActionFormats.json.encodeToJsonElement(
            PolymorphicSerializer(RewardActionData::class),
            action
        )
    }

    fun descriptor(id: String): SerialDescriptor? {
        return definition(id)?.descriptorFactory?.invoke()
    }
}
