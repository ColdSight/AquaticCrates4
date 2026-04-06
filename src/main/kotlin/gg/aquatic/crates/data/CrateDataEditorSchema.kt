package gg.aquatic.crates.data

import gg.aquatic.crates.data.action.RewardActionTypes
import gg.aquatic.crates.data.condition.*
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.crates.data.editor.PreviewSectionFieldAdapter
import gg.aquatic.crates.data.hologram.CrateHologramLineTypes
import gg.aquatic.crates.data.hologram.findHologramLineSubtypeId
import gg.aquatic.crates.data.interaction.CrateClickActionTypes
import gg.aquatic.crates.data.interaction.CrateClickMappingData
import gg.aquatic.crates.data.interaction.findCrateClickActionSubtypeId
import gg.aquatic.crates.data.interactable.*
import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.crates.data.price.OpenPriceGroupData
import gg.aquatic.crates.data.price.OpenPriceTypes
import gg.aquatic.crates.data.price.findOpenPriceSubtypeId
import gg.aquatic.crates.data.processor.BasicRewardProcessorData
import gg.aquatic.crates.data.processor.ChooseRewardProcessorData
import gg.aquatic.crates.data.processor.RewardProcessorSectionFieldAdapter
import gg.aquatic.crates.data.processor.RewardProcessorType
import gg.aquatic.crates.data.provider.ConditionalPoolsRewardProviderData
import gg.aquatic.crates.data.provider.RewardProviderSectionFieldAdapter
import gg.aquatic.crates.data.provider.RewardProviderType
import gg.aquatic.crates.data.provider.SimpleRewardProviderData
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.Material

object CrateDataEditorSchema : EditableModel<CrateData>(CrateData.serializer()) {
    override fun resolveDescriptor(context: EditorFieldContext) = when {
        context.pathSegments.contains("hologram") ->
            context.findHologramLineSubtypeId()?.let(CrateHologramLineTypes::descriptor)
        context.pathSegments.contains("interactables") ->
            context.findInteractableSubtypeId()?.let(CrateInteractableTypes::descriptor)
        (context.pathSegments.contains("priceGroups") || context.pathSegments.contains("cost")) && context.pathSegments.contains("prices") ->
            context.findOpenPriceSubtypeId()?.let(OpenPriceTypes::descriptor)
        context.pathSegments.contains("winActions") || context.pathSegments.contains("clickActions") ->
            context.findRewardActionType()?.let(RewardActionTypes::descriptor)
        context.pathSegments.any { it == "crateClickMapping" || it == "keyClickMapping" } ->
            context.findCrateClickActionSubtypeId()?.let(CrateClickActionTypes::descriptor)
        context.pathSegments.contains("openConditions") || context.pathSegments.contains("conditions") ->
            context.findConditionSubtypeId()?.let(PlayerConditionTypes::descriptor)
        else -> null
    }

    override fun TypedEditorSchemaBuilder<CrateData>.define() {
        field(CrateData::rewardProviderType, visibleWhen = { false })
        field(CrateData::rewardProcessorType, visibleWhen = { false })
        field(
            CrateData::displayName,
            TextFieldAdapter,
            TextFieldConfig(prompt = "Enter crate display name:", showFormattedPreview = true),
            displayName = "Display Name",
            iconMaterial = Material.NAME_TAG,
            description = listOf("Main crate name shown in menus and other UI.")
        )
        list(
            CrateData::interactables,
            displayName = "Interactables",
            iconMaterial = Material.ARMOR_STAND,
            description = listOf("Clientside objects players can click to open this crate."),
            newValueFactory = CrateInteractableSelectionMenu.entryFactory
        ) {
            fieldPattern(
                displayName = "Interactable",
                adapter = CrateInteractableEntryFieldAdapter,
                description = listOf(
                    "Left click to edit this interactable.",
                    "Right click to change its interactable type."
                )
            )
            include { with(BlockCrateInteractableData) { defineEditor() } }
            include { with(EntityCrateInteractableData) { defineEditor() } }
            include { with(MEGCrateInteractableData) { defineEditor() } }
            include { with(MultiBlockCrateInteractableData) { defineEditor() } }
        }
        list(
            CrateData::openConditions,
            displayName = "Open Conditions",
            iconMaterial = Material.TRIPWIRE_HOOK,
            description = listOf("Conditions that must pass before the crate can be opened."),
            newValueFactory = OpenPlayerConditionSelectionMenu.entryFactory
        ) {
            definePlayerConditionEditor(
                includeOpenOnlyConditions = true,
                adapter = OpenPlayerConditionEntryFieldAdapter
            )
        }
        field(
            CrateData::disableOpenStats,
            displayName = "Disable Open Stats",
            prompt = "Enter true or false:",
            iconMaterial = Material.LECTERN,
            description = listOf("If enabled, openings and won rewards from this crate will not be written into the stats database.")
        )
        list(
            CrateData::limits,
            displayName = "Limits",
            iconMaterial = Material.CLOCK,
            description = listOf("Per-player rolling limits for how often this crate can be opened.")
        ) {
            with(LimitData) { defineEditor() }
        }
        list(
            CrateData::priceGroups,
            displayName = "Price Groups",
            iconMaterial = Material.GOLD_INGOT,
            description = listOf(
                "Alternative price groups used to open this crate.",
                "If one group can be paid in full, the crate opens."
            ),
            newValueFactory = OpenPriceGroupData.defaultEntryFactory
        ) {
            with(OpenPriceGroupData) { defineEditor() }
        }
        map(
            CrateData::rarities,
            displayName = "Rarities",
            iconMaterial = Material.NETHER_STAR,
            description = listOf(
                "All rarity groups available in this crate.",
                "Rewards from any provider reference these crate-level rarities."
            ),
            mapKeyPrompt = "Enter rarity ID:",
            newMapEntryFactory = EditorEntryFactories.map(
                keyPrompt = "Enter rarity ID:",
                keyValidator = { if (CrateEditorValidators.crateIdRegex.matches(it)) null else "Use only letters, numbers, '_' or '-'." },
                valueFactory = { rarityId ->
                    CrateDataFormats.json.encodeToJsonElement(
                        RewardRarityData.serializer(),
                        RewardRarityData(displayName = rarityId, chance = 1.0)
                    )
                }
            )
        ) {
            with(RewardRarityData) { defineEditor() }
        }
        field(
            CrateData::simpleProvider,
            adapter = RewardProviderSectionFieldAdapter,
            displayName = "Rewards",
            iconMaterial = Material.CHEST_MINECART,
            description = listOf(
                "Active reward provider for this crate.",
                "Left click to edit its settings.",
                "Right click to change the reward provider type."
            ),
            visibleWhen = { it.isRewardProviderType(RewardProviderType.SIMPLE) }
        )
        include<CrateData>(visibleWhen = { it.isRewardProviderType(RewardProviderType.SIMPLE) }) {
            group(CrateData::simpleProvider) {
                with(SimpleRewardProviderData) { defineEditor() }
            }
        }
        field(
            CrateData::conditionalPoolsProvider,
            adapter = RewardProviderSectionFieldAdapter,
            displayName = "Rewards",
            iconMaterial = Material.CHEST_MINECART,
            description = listOf(
                "Active reward provider for this crate.",
                "Left click to edit its settings.",
                "Right click to change the reward provider type."
            ),
            visibleWhen = { it.isRewardProviderType(RewardProviderType.CONDITIONAL_POOLS) }
        )
        include<CrateData>(visibleWhen = { it.isRewardProviderType(RewardProviderType.CONDITIONAL_POOLS) }) {
            group(CrateData::conditionalPoolsProvider) {
                with(ConditionalPoolsRewardProviderData) { defineEditor() }
            }
        }
        field(
            CrateData::basicProcessor,
            adapter = RewardProcessorSectionFieldAdapter,
            displayName = "Reward Processor",
            iconMaterial = Material.HOPPER_MINECART,
            description = listOf(
                "Controls what happens after rewards are rolled.",
                "Left click to edit its settings.",
                "Right click to change the processor type."
            ),
            visibleWhen = { it.isRewardProcessorType(RewardProcessorType.BASIC) }
        )
        include<CrateData>(visibleWhen = { it.isRewardProcessorType(RewardProcessorType.BASIC) }) {
            group(CrateData::basicProcessor) {
                with(BasicRewardProcessorData) { defineEditor() }
            }
        }
        field(
            CrateData::chooseProcessor,
            adapter = RewardProcessorSectionFieldAdapter,
            displayName = "Reward Processor",
            iconMaterial = Material.HOPPER_MINECART,
            description = listOf(
                "Controls what happens after rewards are rolled.",
                "Left click to edit its settings.",
                "Right click to change the processor type."
            ),
            visibleWhen = { it.isRewardProcessorType(RewardProcessorType.CHOOSE) }
        )
        include<CrateData>(visibleWhen = { it.isRewardProcessorType(RewardProcessorType.CHOOSE) }) {
            group(CrateData::chooseProcessor) {
                with(ChooseRewardProcessorData) { defineEditor() }
            }
        }
        group(CrateData::keyItem) {
            with(StackedItemData) {
                defineFullEditor(
                    materialLabel = "Key Material",
                    materialPrompt = "Enter material or Factory:ItemId:",
                    nameLabel = "Key Name",
                    namePrompt = "Enter key display name:",
                    loreLabel = "Key Lore"
                )
            }
        }
        field(
            CrateData::keyMustBeHeld,
            displayName = "Key Must Be Held",
            prompt = "Enter true or false:",
            iconMaterial = Material.TRIPWIRE_HOOK,
            description = listOf("If enabled, players must hold this crate key in hand when opening the crate directly in the world.")
        )
        group(CrateData::crateClickMapping) {
            with(CrateClickMappingData) { defineEditor("Crate Click") }
        }
        group(CrateData::keyClickMapping) {
            with(CrateClickMappingData) { defineEditor("Key Click", allowDestroy = false) }
        }
        optionalGroup(CrateData::hologram) {
            with(CrateHologramData) { defineEditor() }
        }
        field(
            CrateData::preview,
            adapter = PreviewSectionFieldAdapter,
            displayName = "Preview",
            iconMaterial = Material.ENDER_EYE,
            description = listOf(
                "Preview menu configuration for this crate.",
                "Left click to edit it.",
                "Right click to change the preview type."
            )
        )
        optionalGroup(CrateData::preview) {
            with(PreviewMenuData) { defineEditor() }
        }
    }
}

private fun EditorFieldContext.isRewardProviderType(type: RewardProviderType): Boolean {
    val current = when (val currentValue = value as? JsonObject) {
        null -> null
        else -> (currentValue["rewardProviderType"] as? JsonPrimitive)?.content
    }
    if (current != null) {
        return current.equals(type.id, true)
    }

    val rootType = (root as? JsonObject)
        ?.get("rewardProviderType")
        ?.let { it as? JsonPrimitive }
        ?.content

    return (rootType ?: RewardProviderType.SIMPLE.id).equals(type.id, true)
}

private fun EditorFieldContext.isRewardProcessorType(type: RewardProcessorType): Boolean {
    val current = when (val currentValue = value as? JsonObject) {
        null -> null
        else -> (currentValue["rewardProcessorType"] as? JsonPrimitive)?.content
    }
    if (current != null) {
        return current.equals(type.id, true)
    }

    val rootType = (root as? JsonObject)
        ?.get("rewardProcessorType")
        ?.let { it as? JsonPrimitive }
        ?.content

    return (rootType ?: RewardProcessorType.BASIC.id).equals(type.id, true)
}

private fun EditorFieldContext.findRewardActionType(): String? {
    if (!pathSegments.contains("winActions") && !pathSegments.contains("clickActions")) {
        return null
    }

    val direct = (value as? JsonObject)?.get("type")?.let { it as? JsonPrimitive }?.content
    if (direct != null) {
        return direct
    }

    val actionIndex = pathSegments.indexOfLast { it.toIntOrNull() != null }
    if (actionIndex == -1) {
        return null
    }

    var current: JsonElement = root
    for (segment in pathSegments.take(actionIndex + 1)) {
        current = when (val numericIndex = segment.toIntOrNull()) {
            null -> {
                val obj = current as? JsonObject ?: return null
                obj[segment] ?: return null
            }

            else -> {
                val array = current as? JsonArray ?: return null
                array.getOrNull(numericIndex) ?: return null
            }
        }
    }

    return (current as? JsonObject)
        ?.get("type")
        ?.let { it as? JsonPrimitive }
        ?.content
}
