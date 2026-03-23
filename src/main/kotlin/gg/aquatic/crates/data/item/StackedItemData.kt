package gg.aquatic.crates.data.item

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.crates.data.editor.ItemFlagFieldAdapter
import gg.aquatic.stacked.ItemHandler
import gg.aquatic.stacked.StackedItem
import gg.aquatic.stacked.impl.StackedItemImpl
import gg.aquatic.stacked.option.*
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import java.util.*

@Serializable
data class StackedItemData(
    val material: String = Material.STONE.name,
    val displayName: String? = null,
    val lore: List<String> = emptyList(),
    val amount: Int = 1,
    val customModelDataLegacy: Int? = null,
    val customModelColors: List<String> = emptyList(),
    val customModelFloats: List<Float> = emptyList(),
    val customModelFlags: List<Boolean> = emptyList(),
    val customModelStrings: List<String> = emptyList(),
    val itemModel: String? = null,
    val damage: Int? = null,
    val maxDamage: Int? = null,
    val maxStackSize: Int? = null,
    val unbreakable: Boolean = false,
    val hideTooltip: Boolean = false,
    val rarity: String? = null,
    val spawnerType: String? = null,
    val tooltipStyle: String? = null,
    val dyeColor: String? = null,
    val enchants: List<String> = emptyList(),
    val flags: List<String> = emptyList(),
) {

    fun asStacked(): StackedItemImpl {
        val baseItem = resolveBaseItem()
        val options = mutableListOf<ItemOptionHandle>()

        options += AmountOptionHandle(amount.coerceIn(1, 64))

        displayName?.let {
            options += DisplayNameOptionHandle(it.toMMComponent())
        }

        if (lore.isNotEmpty()) {
            options += LoreOptionHandle(lore.map { it.toMMComponent() })
        }

        customModelDataLegacy?.let {
            options += CustomModelDataLegacyOptionHandle(it)
        }

        val modelColors = customModelColors.mapNotNull(::parseColor)
        if (modelColors.isNotEmpty() || customModelFloats.isNotEmpty() || customModelFlags.isNotEmpty() || customModelStrings.isNotEmpty()) {
            options += CustomModelDataOptionHandle(
                colors = modelColors,
                floats = customModelFloats,
                flags = customModelFlags,
                strings = customModelStrings
            )
        }

        itemModel?.let(::parseKey)?.let {
            options += ItemModelOptionHandle(it)
        }

        damage?.let {
            options += DamageOptionHandle(it)
        }

        maxDamage?.let {
            options += MaxDamageOptionHandle(it)
        }

        maxStackSize?.let {
            options += MaxStackSizeOptionHandle(it)
        }

        if (unbreakable) {
            options += UnbreakableOptionHandle()
        }

        if (hideTooltip) {
            options += HideTooltipOptionHandle(true)
        }

        rarity?.let(::parseRarity)?.let {
            options += RarityOptionHandle(it)
        }

        spawnerType?.let(::parseEntityType)?.let {
            options += SpawnerTypeOptionHandle(it)
        }

        tooltipStyle?.let(::parseKey)?.let {
            options += TooltipStyleOptionHandle(it)
        }

        dyeColor?.let(::parseColor)?.let {
            options += DyeOptionHandle(it)
        }

        val parsedEnchants = enchants.mapNotNull(::parseEnchantLine).toMap()
        if (parsedEnchants.isNotEmpty()) {
            options += EnchantsOptionHandle(parsedEnchants)
        }

        val parsedFlags = flags.mapNotNull(::parseItemFlag)
        if (parsedFlags.isNotEmpty()) {
            options += FlagsOptionHandle(parsedFlags)
        }

        return ItemHandler.Impl.create(baseItem, options)
    }

    private fun resolveBaseItem(): ItemStack {
        val raw = material.trim()
        if (raw.isEmpty()) return ItemStack(Material.STONE)

        parseVanillaMaterial(raw)?.let { vanilla ->
            return ItemStack(vanilla)
        }

        val splitIdx = raw.indexOf(':')
        if (splitIdx <= 0 || splitIdx >= raw.lastIndex) {
            return ItemStack(Material.STONE)
        }

        val factoryId = raw.substring(0, splitIdx).trim()
        val itemId = raw.substring(splitIdx + 1).trim()
        if (factoryId.isEmpty() || itemId.isEmpty()) return ItemStack(Material.STONE)

        return resolveFactoryItem(factoryId, itemId)?.clone() ?: ItemStack(Material.STONE)
    }

    private fun resolveFactoryItem(factory: String, itemId: String): ItemStack? {
        val candidates = linkedSetOf(
            factory,
            factory.lowercase(Locale.ROOT),
            factory.uppercase(Locale.ROOT)
        )

        for (candidate in candidates) {
            val built = StackedItem.ITEM_FACTORIES[candidate]?.create(itemId)
            if (built != null) return built
        }
        return null
    }

    private fun parseVanillaMaterial(raw: String): Material? {
        return Material.matchMaterial(raw)
            ?: Material.matchMaterial(raw.uppercase(Locale.ROOT))
    }

    private fun parseKey(raw: String): Key? {
        return runCatching { Key.key(raw) }.getOrNull()
    }

    private fun parseRarity(raw: String): ItemRarity? {
        return runCatching { ItemRarity.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()
    }

    private fun parseEntityType(raw: String): EntityType? {
        return runCatching { EntityType.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()
    }

    private fun parseEnchantLine(raw: String): Pair<String, Int>? {
        val value = raw.trim()
        val splitIdx = value.lastIndexOf(':')
        if (splitIdx <= 0 || splitIdx >= value.lastIndex) return null

        val enchant = value.substring(0, splitIdx).trim()
        val level = value.substring(splitIdx + 1).trim().toIntOrNull() ?: return null
        if (enchant.isEmpty()) return null

        return enchant to level
    }

    private fun parseItemFlag(raw: String): ItemFlag? {
        return runCatching { ItemFlag.valueOf(raw.uppercase(Locale.ROOT)) }.getOrNull()
    }

    private fun parseColor(raw: String): Color? {
        val value = raw.trim()
        if (value.isEmpty()) return null

        if (value.startsWith("#") && value.length == 7) {
            return runCatching {
                Color.fromRGB(
                    value.substring(1, 3).toInt(16),
                    value.substring(3, 5).toInt(16),
                    value.substring(5, 7).toInt(16)
                )
            }.getOrNull()
        }

        val split = if (';' in value) value.split(";") else value.split(",")
        if (split.size != 3) return null

        val red = split[0].trim().toIntOrNull() ?: return null
        val green = split[1].trim().toIntOrNull() ?: return null
        val blue = split[2].trim().toIntOrNull() ?: return null
        return runCatching { Color.fromRGB(red, green, blue) }.getOrNull()
    }

    companion object {
        fun TypedNestedSchemaBuilder<StackedItemData>.defineBasicEditor(
            materialLabel: String,
            nameLabel: String,
            namePrompt: String,
            loreLabel: String,
            amountLabel: String,
            materialPrompt: String = "Enter material or Factory:ItemId:",
        ) {
            field(
                StackedItemData::material,
                MaterialLikeFieldAdapter,
                MaterialLikeFieldConfig(prompt = materialPrompt),
                displayName = materialLabel,
                iconMaterial = Material.BRICKS,
                description = listOf("Base item material or Factory:ItemId used for this item.")
            )
            field(
                StackedItemData::displayName,
                TextFieldAdapter,
                TextFieldConfig(prompt = namePrompt, showFormattedPreview = true),
                displayName = nameLabel,
                iconMaterial = Material.NAME_TAG,
                description = listOf("Custom item name shown to players.")
            )
            list(
                StackedItemData::lore,
                loreLabel,
                iconMaterial = Material.WRITABLE_BOOK,
                description = listOf("Lore lines shown under the item name."),
                newValueFactory = EditorEntryFactories.text("Enter lore line:")
            )
            field(
                StackedItemData::amount,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter amount:", min = 1, max = 64),
                displayName = amountLabel,
                iconMaterial = Material.COPPER_INGOT,
                description = listOf("Item stack size given or shown in menus.")
            )
        }

        fun TypedNestedSchemaBuilder<StackedItemData>.defineFullEditor(
            materialLabel: String,
            materialPrompt: String,
            nameLabel: String,
            namePrompt: String,
            loreLabel: String,
        ) {
            defineBasicEditor(
                materialLabel = materialLabel,
                materialPrompt = materialPrompt,
                nameLabel = nameLabel,
                namePrompt = namePrompt,
                loreLabel = loreLabel,
                amountLabel = "Amount"
            )
            field(
                StackedItemData::customModelDataLegacy,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter legacy custom model data:"),
                displayName = "Legacy CMD",
                iconMaterial = Material.REDSTONE,
                description = listOf("Legacy CustomModelData integer value.")
            )
            list(
                StackedItemData::customModelColors,
                "Model Colors",
                iconMaterial = Material.MAGENTA_DYE,
                description = listOf("Color entries used by modern item model data."),
                newValueFactory = EditorEntryFactories.text(
                    prompt = "Enter color (#RRGGBB or r;g;b):",
                    validator = { if (CrateEditorValidators.isValidColor(it)) null else "Invalid color." }
                )
            )
            list(
                StackedItemData::customModelFloats,
                "Model Floats",
                iconMaterial = Material.AMETHYST_SHARD,
                description = listOf("Float entries used by modern item model data."),
                newValueFactory = EditorEntryFactories.float("Enter float value:")
            )
            list(
                StackedItemData::customModelFlags,
                "Model Flags",
                iconMaterial = Material.LEVER,
                description = listOf("Boolean flags used by modern item model data."),
                newValueFactory = EditorEntryFactories.boolean("Enter boolean value (true/false):")
            )
            list(
                StackedItemData::customModelStrings,
                "Model Strings",
                iconMaterial = Material.PAPER,
                description = listOf("String entries used by modern item model data."),
                newValueFactory = EditorEntryFactories.text("Enter model string:")
            )
            field(
                StackedItemData::itemModel,
                TextFieldAdapter,
                TextFieldConfig(
                    prompt = "Enter item model key (namespace:key):",
                    validator = CrateEditorValidators::validateNamespacedKey
                ),
                displayName = "Item Model",
                iconMaterial = Material.ITEM_FRAME,
                description = listOf("Namespaced item model key used by the client.")
            )
            field(
                StackedItemData::damage,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter damage:", min = 0),
                displayName = "Damage",
                iconMaterial = Material.IRON_SWORD,
                description = listOf("Current durability damage applied to the item.")
            )
            field(
                StackedItemData::maxDamage,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter max damage:", min = 1),
                displayName = "Max Damage",
                iconMaterial = Material.ANVIL,
                description = listOf("Overrides the maximum durability of the item.")
            )
            field(
                StackedItemData::maxStackSize,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter max stack size:", min = 1, max = 99),
                displayName = "Max Stack Size",
                iconMaterial = Material.CHEST,
                description = listOf("Overrides how many items can stack together.")
            )
            field(
                StackedItemData::rarity,
                EnumFieldAdapter,
                EnumFieldConfig(
                    prompt = "Enter item rarity:",
                    values = { ItemRarity.entries.map { it.name } }
                ),
                displayName = "Rarity",
                iconMaterial = Material.NETHER_STAR,
                description = listOf("Vanilla item rarity shown by the client.")
            )
            field(
                StackedItemData::spawnerType,
                EnumFieldAdapter,
                EnumFieldConfig(
                    prompt = "Enter entity type:",
                    values = { EntityType.entries.map { it.name } }
                ),
                displayName = "Spawner Type",
                iconMaterial = Material.SPAWNER,
                description = listOf("Entity type stored in the spawner item.")
            )
            field(
                StackedItemData::tooltipStyle,
                TextFieldAdapter,
                TextFieldConfig(
                    prompt = "Enter tooltip style key (namespace:key):",
                    validator = CrateEditorValidators::validateNamespacedKey
                ),
                displayName = "Tooltip Style",
                iconMaterial = Material.PAINTING,
                description = listOf("Namespaced tooltip style key used by the client.")
            )
            field(
                StackedItemData::dyeColor,
                ColorFieldAdapter,
                ColorFieldConfig(),
                displayName = "Dye Color",
                iconMaterial = Material.MAGENTA_DYE,
                description = listOf("Leather armor or dyed item color.")
            )
            list(
                StackedItemData::enchants,
                "Enchants",
                iconMaterial = Material.ENCHANTING_TABLE,
                description = listOf("Enchant entries in format namespace:enchant:level."),
                newValueFactory = EditorEntryFactories.text("Enter enchant in format enchant:level:", CrateEditorValidators::validateEnchantLine)
            )
            list(
                StackedItemData::flags,
                displayName = "Flags",
                iconMaterial = Material.BOOK,
                description = listOf("Vanilla item flags hidden on the tooltip."),
                adapter = ItemFlagFieldAdapter
            )
        }
    }
}
