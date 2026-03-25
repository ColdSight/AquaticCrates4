package gg.aquatic.crates.data.hologram

import gg.aquatic.crates.data.editor.PolymorphicSelectionMenu
import gg.aquatic.waves.serialization.editor.meta.EntryFactory
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
object CrateHologramLineFormats {
    val module = SerializersModule {
        polymorphic(CrateHologramLineData::class) {
            subclass(TextCrateHologramLineData::class)
            subclass(ItemCrateHologramLineData::class)
            subclass(AnimatedCrateHologramLineData::class)
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

object CrateHologramLineTypes {
    data class Definition(
        val id: String,
        val displayName: String,
        val description: List<String>,
        val icon: Material,
        val factory: () -> CrateHologramLineData,
        val descriptorFactory: () -> SerialDescriptor,
    )

    val definitions = listOf(
        Definition(
            "text",
            "Text Line",
            listOf("Displays MiniMessage text", "inside a text display hologram."),
            Material.PAPER,
            { TextCrateHologramLineData() },
            { TextCrateHologramLineData.serializer().descriptor }
        ),
        Definition(
            "item",
            "Item Line",
            listOf("Displays an item stack", "inside an item display hologram."),
            Material.ITEM_FRAME,
            { ItemCrateHologramLineData() },
            { ItemCrateHologramLineData.serializer().descriptor }
        ),
        Definition(
            "animated",
            "Animated Line",
            listOf("Cycles between multiple frames", "using any supported hologram line type."),
            Material.CLOCK,
            { AnimatedCrateHologramLineData() },
            { AnimatedCrateHologramLineData.serializer().descriptor }
        ),
    )

    private val byId = definitions.associateBy { it.id }

    fun descriptor(id: String): SerialDescriptor? = byId[id]?.descriptorFactory?.invoke()

    fun defaultElement(id: String): JsonElement? {
        val line = byId[id]?.factory?.invoke() ?: return null
        return CrateHologramLineFormats.json.encodeToJsonElement(
            PolymorphicSerializer(CrateHologramLineData::class),
            line
        )
    }

    fun defaultFrameElement(id: String): JsonElement? {
        val line = byId[id]?.factory?.invoke() ?: return null
        return CrateHologramLineFormats.json.encodeToJsonElement(
            AnimatedHologramFrameData.serializer(),
            AnimatedHologramFrameData(line = line)
        )
    }

    fun definition(id: String): Definition? = byId[id]
}

object HologramLineSelectionMenu {
    private val entrySlots = listOf(11, 13, 15)
    private val definitions = CrateHologramLineTypes.definitions.map {
        PolymorphicSelectionMenu.Definition(it.id, it.displayName, it.description, it.icon)
    }
    private val frameDefinitions = CrateHologramLineTypes.definitions
        .filterNot { it.id == "animated" }
        .map {
            PolymorphicSelectionMenu.Definition(it.id, it.displayName, it.description, it.icon)
        }

    val entryFactory: EntryFactory = PolymorphicSelectionMenu.entryFactory(
        title = "Select Hologram Line",
        entrySlots = entrySlots,
        definitions = definitions,
        elementFactory = CrateHologramLineTypes::defaultElement
    )

    val frameEntryFactory: EntryFactory = PolymorphicSelectionMenu.entryFactory(
        title = "Select Animation Frame",
        entrySlots = entrySlots,
        definitions = frameDefinitions,
        elementFactory = CrateHologramLineTypes::defaultFrameElement
    )

    suspend fun select(player: org.bukkit.entity.Player): String? {
        return PolymorphicSelectionMenu.selectType(
            player = player,
            title = "Select Hologram Line",
            inventoryType = gg.aquatic.kmenu.inventory.InventoryType.GENERIC9X3,
            entrySlots = entrySlots,
            cancelSlot = 22,
            definitions = definitions
        )
    }

    suspend fun selectFrame(player: org.bukkit.entity.Player): String? {
        return PolymorphicSelectionMenu.selectType(
            player = player,
            title = "Select Animation Frame",
            inventoryType = gg.aquatic.kmenu.inventory.InventoryType.GENERIC9X3,
            entrySlots = entrySlots,
            cancelSlot = 22,
            definitions = frameDefinitions
        )
    }
}
