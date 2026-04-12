package gg.aquatic.crates.data

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.crates.data.editor.encodeToNode
import gg.aquatic.crates.data.editor.stringContentOrNull
import gg.aquatic.crates.data.menu.MenuInventoryData
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.EntryFactory
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import org.bukkit.Material

fun TypedNestedSchemaBuilder<PreviewMenuData>.definePreviewMenuEditor() {
    field(PreviewMenuData::previewType, visibleWhen = { false })
    include<PreviewMenuData>(visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }) {
        group(PreviewMenuData::inventory) {
            with(MenuInventoryData) {
                defineEditor(typeLabel = "Preview Inventory Type")
            }
        }
    }
    field(
        PreviewMenuData::title,
        TextFieldAdapter,
        TextFieldConfig(prompt = "Enter preview title:", showFormattedPreview = true),
        displayName = "Preview Title",
        searchTags = listOf("title", "preview title", "menu title", "inventory title"),
        iconMaterial = Material.NAME_TAG,
        description = listOf("Menu title shown at the top of the preview inventory."),
        visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
    )
    list(
        PreviewMenuData::rewardSlots,
        "Reward Slots",
        searchTags = listOf("reward slots", "slots", "static reward slots", "preview slots"),
        iconMaterial = Material.HOPPER,
        description = listOf("Slots where reward icons can appear in the preview menu."),
        newValueFactory = EditorEntryFactories.int("Enter reward slot or range (e.g. 10-16 or 10,12,14):", unique = true),
        visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
    )
    list(
        PreviewMenuData::randomRewardSlots,
        "Random Reward Slots",
        searchTags = listOf("random slots", "random reward slots", "rolling rewards", "random preview"),
        iconMaterial = Material.CHEST,
        description = listOf("Slots where randomly selected reward icons can appear in the preview menu."),
        newValueFactory = EditorEntryFactories.int("Enter random reward slot or range (e.g. 19-25):", unique = true),
        visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
    )
    field(
        PreviewMenuData::randomRewardSwitchTicks,
        IntFieldAdapter,
        IntFieldConfig(prompt = "Enter random reward switch ticks:", min = 1),
        displayName = "Random Reward Switch Ticks",
        searchTags = listOf("switch ticks", "reroll", "refresh interval", "random reward speed"),
        iconMaterial = Material.CLOCK,
        description = listOf("How often random reward slots reroll to a new reward."),
        visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
    )
    field(
        PreviewMenuData::randomRewardUnique,
        displayName = "Random Reward Unique",
        searchTags = listOf("unique", "unique rewards", "no duplicates", "random unique"),
        prompt = "Enter true or false:",
        iconMaterial = Material.COMPARATOR,
        description = listOf(
            "If enabled, each random reward slot shows a different reward.",
            "Extra slots are hidden when there are not enough unique rewards."
        ),
        visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
    )
    list(
        PreviewMenuData::rewardLore,
        displayName = "Reward Lore",
        searchTags = listOf("reward lore", "preview lore", "extra lore", "tooltip"),
        iconMaterial = Material.WRITABLE_BOOK,
        description = listOf(
            "Extra lore appended to preview reward items.",
            "Useful for showing reward metadata in preview."
        ),
        newValueFactory = EditorEntryFactories.text("Enter reward lore line:")
    )
    map(
        PreviewMenuData::customButtons,
        displayName = "Custom Buttons",
        searchTags = listOf("buttons", "custom buttons", "pagination", "next page", "prev page"),
        iconMaterial = Material.STONE_BUTTON,
        description = listOf(
            "Additional buttons shown in the preview menu.",
            "Use ID 'next-page' or 'prev-page' to add pagination buttons.",
            "Pagination buttons are only visible when another page is available."
        ),
        mapKeyPrompt = "Enter custom button ID:",
        newMapEntryFactory = previewButtonEntryFactory(),
        visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
    ) {
        with(PreviewButtonData) { defineEditor() }
    }
    list(
        PreviewMenuData::pages,
        displayName = "Pages",
        searchTags = listOf("pages", "custom pages", "multi page", "page list"),
        iconMaterial = Material.BOOK,
        description = listOf("Custom preview pages used when preview type is set to custom-pages."),
        newValueFactory = EntryFactory { _, _ ->
            CrateDataFormats.yaml.encodeToNode(PreviewPageData.serializer(), PreviewPageData())
        },
        visibleWhen = { it.isPreviewType(PREVIEW_TYPE_CUSTOM_PAGES) }
    ) {
        definePreviewPageEditor()
    }
}

fun TypedNestedSchemaBuilder<PreviewPageData>.definePreviewPageEditor() {
    group(PreviewPageData::inventory) {
        with(MenuInventoryData) {
            defineEditor(typeLabel = "Page Inventory Type")
        }
    }
    field(
        PreviewPageData::title,
        TextFieldAdapter,
        TextFieldConfig(prompt = "Enter page title:", showFormattedPreview = true),
        displayName = "Page Title",
        searchTags = listOf("page title", "title", "menu title", "inventory title"),
        iconMaterial = Material.NAME_TAG,
        description = listOf("Menu title shown at the top of this preview page.")
    )
    list(
        PreviewPageData::rewardSlots,
        "Reward Slots",
        searchTags = listOf("reward slots", "slots", "static reward slots", "page slots"),
        iconMaterial = Material.HOPPER,
        description = listOf("Slots where reward icons can appear on this page."),
        newValueFactory = EditorEntryFactories.int("Enter reward slot or range (e.g. 10-16 or 10,12,14):", unique = true)
    )
    list(
        PreviewPageData::randomRewardSlots,
        "Random Reward Slots",
        searchTags = listOf("random slots", "random reward slots", "rolling rewards", "page random rewards"),
        iconMaterial = Material.CHEST,
        description = listOf("Slots where randomly selected reward icons can appear on this page."),
        newValueFactory = EditorEntryFactories.int("Enter random reward slot or range (e.g. 19-25):", unique = true)
    )
    field(
        PreviewPageData::randomRewardSwitchTicks,
        IntFieldAdapter,
        IntFieldConfig(prompt = "Enter random reward switch ticks:", min = 1),
        displayName = "Random Reward Switch Ticks",
        searchTags = listOf("switch ticks", "reroll", "refresh interval", "random reward speed"),
        iconMaterial = Material.CLOCK,
        description = listOf("How often random reward slots reroll to a new reward on this page.")
    )
    field(
        PreviewPageData::randomRewardUnique,
        displayName = "Random Reward Unique",
        searchTags = listOf("unique", "unique rewards", "no duplicates", "random unique"),
        prompt = "Enter true or false:",
        iconMaterial = Material.COMPARATOR,
        description = listOf(
            "If enabled, each random reward slot on this page shows a different reward.",
            "Extra slots are hidden when there are not enough unique rewards."
        )
    )
    list(
        PreviewPageData::rewardLore,
        displayName = "Reward Lore",
        searchTags = listOf("reward lore", "preview lore", "extra lore", "tooltip"),
        iconMaterial = Material.WRITABLE_BOOK,
        description = listOf(
            "Extra lore appended to preview reward items on this page.",
            "Useful for showing reward metadata in preview."
        ),
        newValueFactory = EditorEntryFactories.text("Enter reward lore line:")
    )
    map(
        PreviewPageData::customButtons,
        displayName = "Custom Buttons",
        searchTags = listOf("buttons", "custom buttons", "pagination", "next page", "prev page"),
        iconMaterial = Material.STONE_BUTTON,
        description = listOf(
            "Additional buttons shown on this page.",
            "Use ID 'next-page' or 'prev-page' to add pagination buttons.",
            "Pagination buttons are only visible when another page is available."
        ),
        mapKeyPrompt = "Enter custom button ID:",
        newMapEntryFactory = previewButtonEntryFactory()
    ) {
        with(PreviewButtonData) { defineEditor() }
    }
}

private fun previewButtonEntryFactory() = EditorEntryFactories.map(
    keyPrompt = "Enter custom button ID:",
    keyValidator = { if (CrateEditorValidators.crateIdRegex.matches(it)) null else "Use only letters, numbers, '_' or '-'." },
    valueFactory = { buttonId ->
        CrateDataFormats.yaml.encodeToNode(
            PreviewButtonData.serializer(),
            PreviewButtonData(
                item = gg.aquatic.crates.data.item.StackedItemData(
                    material = Material.PAPER.name,
                    displayName = "<yellow>$buttonId"
                )
            )
        )
    }
)

internal fun EditorFieldContext.isPreviewType(type: String): Boolean {
    val current = (value as? YamlMap)
        ?.get<YamlNode>("previewType")
        ?.stringContentOrNull
    if (current != null) {
        return normalizePreviewType(current).equals(type, true)
    }

    val rootType = (root as? YamlMap)
        ?.get<YamlNode>("preview")
        ?.let { it as? YamlMap }
        ?.get<YamlNode>("previewType")
        ?.stringContentOrNull

    return normalizePreviewType(rootType ?: PREVIEW_TYPE_AUTOMATIC).equals(type, true)
}
