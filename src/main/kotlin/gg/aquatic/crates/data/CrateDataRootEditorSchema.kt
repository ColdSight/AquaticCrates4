package gg.aquatic.crates.data

import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.crates.data.editor.PreviewSectionFieldAdapter
import gg.aquatic.crates.data.editor.encodeToNode
import gg.aquatic.crates.data.editor.switchingSection
import gg.aquatic.crates.data.hologram.HologramSettingsSectionFieldAdapter
import gg.aquatic.crates.data.interaction.InteractionSettingsSectionFieldAdapter
import gg.aquatic.crates.data.key.KeySettingsSectionFieldAdapter
import gg.aquatic.crates.data.processor.BasicRewardProcessorData
import gg.aquatic.crates.data.processor.ChooseRewardProcessorData
import gg.aquatic.crates.data.processor.RewardProcessorSectionFieldAdapter
import gg.aquatic.crates.data.processor.RewardProcessorType
import gg.aquatic.crates.data.provider.ConditionalPoolsRewardProviderData
import gg.aquatic.crates.data.provider.RewardProviderSectionFieldAdapter
import gg.aquatic.crates.data.provider.RewardProviderType
import gg.aquatic.crates.data.provider.SimpleRewardProviderData
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedEditorSchemaBuilder
import org.bukkit.Material

internal fun TypedEditorSchemaBuilder<CrateData>.defineCrateDataRootSchema() {
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
    field(
        CrateData::interactables,
        adapter = InteractionSettingsSectionFieldAdapter,
        displayName = "Interaction Settings",
        iconMaterial = Material.ARMOR_STAND,
        description = listOf(
            "Open behaviour, interactables, price groups and crate click mapping.",
            "All interaction and opening rules are grouped here."
        )
    )
    field(CrateData::openConditions, visibleWhen = { false })
    field(CrateData::disableOpenStats, visibleWhen = { false })
    field(CrateData::limits, visibleWhen = { false })
    field(CrateData::priceGroups, visibleWhen = { false })
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
                CrateDataFormats.yaml.encodeToNode(
                    RewardRarityData.serializer(),
                    RewardRarityData(displayName = rarityId, chance = 1.0)
                )
            }
        )
    ) {
        with(RewardRarityData) { defineEditor() }
    }
    switchingSection(
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
    ) {
        with(SimpleRewardProviderData) { defineEditor() }
    }
    switchingSection(
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
    ) {
        with(ConditionalPoolsRewardProviderData) { defineEditor() }
    }
    switchingSection(
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
    ) {
        with(BasicRewardProcessorData) { defineEditor() }
    }
    switchingSection(
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
    ) {
        with(ChooseRewardProcessorData) { defineEditor() }
    }
    field(
        CrateData::keyItem,
        adapter = KeySettingsSectionFieldAdapter,
        displayName = "Key Settings",
        iconMaterial = Material.TRIPWIRE_HOOK,
        description = listOf(
            "All key-related settings for this crate.",
            "Edit the key item, key interaction mapping and hold requirement here."
        )
    )
    field(CrateData::keyMustBeHeld, visibleWhen = { false })
    field(CrateData::keyClickMapping, visibleWhen = { false })
    field(CrateData::crateClickMapping, visibleWhen = { false })
    field(
        CrateData::hologram,
        adapter = HologramSettingsSectionFieldAdapter,
        displayName = "Hologram Settings",
        iconMaterial = Material.END_CRYSTAL,
        description = listOf(
            "All hologram settings for this crate.",
            "Edit lines and view distance here."
        )
    )
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
        definePreviewMenuEditor()
    }
}
