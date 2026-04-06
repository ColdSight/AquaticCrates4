package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.preview.PreviewMenuSettings
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.crates.data.menu.MenuInventoryData
import gg.aquatic.kmenu.menu.settings.PrivateMenuSettings
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.EntryFactory
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.bukkit.Material

@Serializable
data class PreviewMenuData(
    val previewType: String = PREVIEW_TYPE_AUTOMATIC,
    val inventory: MenuInventoryData = MenuInventoryData(),
    val title: String = "<yellow>Preview Menu",
    val rewardSlots: List<Int> = listOf(10, 11, 12, 13, 14, 15, 16),
    val randomRewardSlots: List<Int> = emptyList(),
    val randomRewardSwitchTicks: Int = 20,
    val randomRewardUnique: Boolean = false,
    val rewardLore: List<String> = emptyList(),
    val customButtons: Map<String, PreviewButtonData> = emptyMap(),
    val pages: List<PreviewPageData> = emptyList(),
) {

    fun toPreviewSettings(): PreviewMenuSettings {
        val type = normalizePreviewType(previewType)
        if (type == PREVIEW_TYPE_CUSTOM_PAGES && pages.isNotEmpty()) {
            return PreviewMenuSettings.CustomPages(
                pages.map { it.toBasicSettings() }
            )
        }

        return toBasicSettings()
    }

    fun toBasicSettings(): PreviewMenuSettings.Basic {
        val inventorySettings = inventory.toRuntimeSettings()

        val components = hashMapOf<String, gg.aquatic.kmenu.menu.settings.IButtonSettings>()

        customButtons.forEach { (id, button) ->
            components[id] = button.toButtonSettings(id)
        }

        return PreviewMenuSettings.Basic(
            rewardSlots = rewardSlots.distinct(),
            randomRewardSlots = randomRewardSlots.distinct(),
            randomRewardSwitchTicks = randomRewardSwitchTicks.coerceAtLeast(1),
            randomRewardUnique = randomRewardUnique,
            rewardLore = rewardLore,
            invSettings = PrivateMenuSettings(
                inventorySettings.inventoryType,
                title.toMMComponent(),
                components
            ),
            anvilSettings = inventorySettings.anvil
        )
    }

    companion object {
        private val schemaJson = Json { encodeDefaults = true }

        fun TypedNestedSchemaBuilder<PreviewMenuData>.defineEditor() {
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
                iconMaterial = Material.NAME_TAG,
                description = listOf("Menu title shown at the top of the preview inventory."),
                visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
            )
            list(
                PreviewMenuData::rewardSlots,
                "Reward Slots",
                iconMaterial = Material.HOPPER,
                description = listOf("Slots where reward icons can appear in the preview menu."),
                newValueFactory = EditorEntryFactories.int("Enter reward slot or range (e.g. 10-16 or 10,12,14):", unique = true),
                visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
            )
            list(
                PreviewMenuData::randomRewardSlots,
                "Random Reward Slots",
                iconMaterial = Material.CHEST,
                description = listOf("Slots where randomly selected reward icons can appear in the preview menu."),
                newValueFactory = EditorEntryFactories.int("Enter random reward slot or range (e.g. 19-25):", unique = true),
                visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
            )
            field(
                PreviewMenuData::randomRewardSwitchTicks,
                gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter,
                gg.aquatic.waves.serialization.editor.meta.IntFieldConfig(prompt = "Enter random reward switch ticks:", min = 1),
                displayName = "Random Reward Switch Ticks",
                iconMaterial = Material.CLOCK,
                description = listOf("How often random reward slots reroll to a new reward."),
                visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
            )
            field(
                PreviewMenuData::randomRewardUnique,
                displayName = "Random Reward Unique",
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
                iconMaterial = Material.STONE_BUTTON,
                description = listOf(
                    "Additional buttons shown in the preview menu.",
                    "Use ID 'next-page' or 'prev-page' to add pagination buttons.",
                    "Pagination buttons are only visible when another page is available."
                ),
                mapKeyPrompt = "Enter custom button ID:",
                newMapEntryFactory = EditorEntryFactories.map(
                    keyPrompt = "Enter custom button ID:",
                    keyValidator = { if (CrateEditorValidators.crateIdRegex.matches(it)) null else "Use only letters, numbers, '_' or '-'." },
                    valueFactory = { buttonId ->
                        schemaJson.encodeToJsonElement(
                            PreviewButtonData.serializer(),
                            PreviewButtonData(
                                item = gg.aquatic.crates.data.item.StackedItemData(
                                    material = Material.PAPER.name,
                                    displayName = "<yellow>$buttonId"
                                )
                            )
                        )
                    }
                ),
                visibleWhen = { it.isPreviewType(PREVIEW_TYPE_AUTOMATIC) }
            ) {
                with(PreviewButtonData) {
                    defineEditor()
                }
            }
            list(
                PreviewMenuData::pages,
                displayName = "Pages",
                iconMaterial = Material.BOOK,
                description = listOf("Custom preview pages used when preview type is set to custom-pages."),
                newValueFactory = EntryFactory { _, _ ->
                    schemaJson.encodeToJsonElement(PreviewPageData.serializer(), PreviewPageData())
                },
                visibleWhen = { it.isPreviewType(PREVIEW_TYPE_CUSTOM_PAGES) }
            ) {
                with(PreviewPageData) {
                    defineEditor()
                }
            }
        }
    }
}

@Serializable
data class PreviewPageData(
    val inventory: MenuInventoryData = MenuInventoryData(),
    val title: String = "<yellow>Preview Page",
    val rewardSlots: List<Int> = listOf(10, 11, 12, 13, 14, 15, 16),
    val randomRewardSlots: List<Int> = emptyList(),
    val randomRewardSwitchTicks: Int = 20,
    val randomRewardUnique: Boolean = false,
    val rewardLore: List<String> = emptyList(),
    val customButtons: Map<String, PreviewButtonData> = emptyMap(),
) {
    fun toBasicSettings(): PreviewMenuSettings.Basic {
        return PreviewMenuData(
            inventory = inventory,
            title = title,
            rewardSlots = rewardSlots,
            randomRewardSlots = randomRewardSlots,
            randomRewardSwitchTicks = randomRewardSwitchTicks,
            randomRewardUnique = randomRewardUnique,
            rewardLore = rewardLore,
            customButtons = customButtons
        ).toBasicSettings()
    }

    companion object {
        private val schemaJson = Json { encodeDefaults = true }

        fun TypedNestedSchemaBuilder<PreviewPageData>.defineEditor() {
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
                iconMaterial = Material.NAME_TAG,
                description = listOf("Menu title shown at the top of this preview page.")
            )
            list(
                PreviewPageData::rewardSlots,
                "Reward Slots",
                iconMaterial = Material.HOPPER,
                description = listOf("Slots where reward icons can appear on this page."),
                newValueFactory = EditorEntryFactories.int("Enter reward slot or range (e.g. 10-16 or 10,12,14):", unique = true)
            )
            list(
                PreviewPageData::randomRewardSlots,
                "Random Reward Slots",
                iconMaterial = Material.CHEST,
                description = listOf("Slots where randomly selected reward icons can appear on this page."),
                newValueFactory = EditorEntryFactories.int("Enter random reward slot or range (e.g. 19-25):", unique = true)
            )
            field(
                PreviewPageData::randomRewardSwitchTicks,
                gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter,
                gg.aquatic.waves.serialization.editor.meta.IntFieldConfig(prompt = "Enter random reward switch ticks:", min = 1),
                displayName = "Random Reward Switch Ticks",
                iconMaterial = Material.CLOCK,
                description = listOf("How often random reward slots reroll to a new reward on this page.")
            )
            field(
                PreviewPageData::randomRewardUnique,
                displayName = "Random Reward Unique",
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
                iconMaterial = Material.STONE_BUTTON,
                description = listOf(
                    "Additional buttons shown on this page.",
                    "Use ID 'next-page' or 'prev-page' to add pagination buttons.",
                    "Pagination buttons are only visible when another page is available."
                ),
                mapKeyPrompt = "Enter custom button ID:",
                newMapEntryFactory = EditorEntryFactories.map(
                    keyPrompt = "Enter custom button ID:",
                    keyValidator = { if (CrateEditorValidators.crateIdRegex.matches(it)) null else "Use only letters, numbers, '_' or '-'." },
                    valueFactory = { buttonId ->
                        schemaJson.encodeToJsonElement(
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
            ) {
                with(PreviewButtonData) {
                    defineEditor()
                }
            }
        }
    }
}

const val PREVIEW_TYPE_AUTOMATIC = "automatic"
const val PREVIEW_TYPE_CUSTOM_PAGES = "custom-pages"

private fun normalizePreviewType(raw: String): String {
    return when {
        raw.equals(PREVIEW_TYPE_CUSTOM_PAGES, ignoreCase = true) -> PREVIEW_TYPE_CUSTOM_PAGES
        else -> PREVIEW_TYPE_AUTOMATIC
    }
}

private fun EditorFieldContext.isPreviewType(type: String): Boolean {
    val current = when (val currentValue = value as? JsonObject) {
        null -> null
        else -> (currentValue["previewType"] as? JsonPrimitive)?.content
    }
    if (current != null) {
        return normalizePreviewType(current).equals(type, true)
    }

    val rootType = (root as? JsonObject)
        ?.get("preview")
        ?.let { it as? JsonObject }
        ?.get("previewType")
        ?.let { it as? JsonPrimitive }
        ?.content

    return normalizePreviewType(rootType ?: PREVIEW_TYPE_AUTOMATIC).equals(type, true)
}
