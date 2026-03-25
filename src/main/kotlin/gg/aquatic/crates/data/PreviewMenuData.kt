package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.preview.PreviewMenuSettings
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.crates.data.editor.InventoryTypeFieldAdapter
import gg.aquatic.kmenu.inventory.InventoryType
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
    val previewType: String = PreviewType.AUTOMATIC.id,
    val inventoryType: String = "GENERIC9X3",
    val title: String = "<yellow>Preview Menu",
    val rewardSlots: List<Int> = listOf(10, 11, 12, 13, 14, 15, 16),
    val customButtons: Map<String, PreviewButtonData> = emptyMap(),
    val pages: List<PreviewPageData> = emptyList(),
) {

    fun toPreviewSettings(): PreviewMenuSettings {
        val type = PreviewType.of(previewType)
        if (type == PreviewType.CUSTOM_PAGES && pages.isNotEmpty()) {
            return PreviewMenuSettings.CustomPages(
                pages.map { it.toBasicSettings() }
            )
        }

        return toBasicSettings()
    }

    fun toBasicSettings(): PreviewMenuSettings.Basic {
        val resolvedType = runCatching { InventoryType.valueOf(inventoryType.trim()) }
            .getOrDefault(InventoryType.GENERIC9X3)

        val components = hashMapOf<String, gg.aquatic.kmenu.menu.settings.IButtonSettings>()

        customButtons.forEach { (id, button) ->
            components[id] = button.toButtonSettings(id)
        }

        return PreviewMenuSettings.Basic(
            rewardSlots = rewardSlots.distinct(),
            invSettings = PrivateMenuSettings(
                resolvedType,
                title.toMMComponent(),
                components
            )
        )
    }

    companion object {
        private val schemaJson = Json { encodeDefaults = true }

        fun TypedNestedSchemaBuilder<PreviewMenuData>.defineEditor() {
            field(PreviewMenuData::previewType, visibleWhen = { false })
            field(
                PreviewMenuData::inventoryType,
                adapter = InventoryTypeFieldAdapter,
                displayName = "Preview Inventory Type",
                iconMaterial = Material.CHEST,
                description = listOf("Inventory layout used for the crate preview menu."),
                visibleWhen = { it.isPreviewType(PreviewType.AUTOMATIC) }
            )
            field(
                PreviewMenuData::title,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter preview title:", showFormattedPreview = true),
                displayName = "Preview Title",
                iconMaterial = Material.NAME_TAG,
                description = listOf("Menu title shown at the top of the preview inventory."),
                visibleWhen = { it.isPreviewType(PreviewType.AUTOMATIC) }
            )
            list(
                PreviewMenuData::rewardSlots,
                "Reward Slots",
                iconMaterial = Material.HOPPER,
                description = listOf("Slots where reward icons can appear in the preview menu."),
                newValueFactory = EditorEntryFactories.int("Enter reward slot or range (e.g. 10-16 or 10,12,14):", unique = true),
                visibleWhen = { it.isPreviewType(PreviewType.AUTOMATIC) }
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
                visibleWhen = { it.isPreviewType(PreviewType.AUTOMATIC) }
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
                visibleWhen = { it.isPreviewType(PreviewType.CUSTOM_PAGES) }
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
    val inventoryType: String = "GENERIC9X3",
    val title: String = "<yellow>Preview Page",
    val rewardSlots: List<Int> = listOf(10, 11, 12, 13, 14, 15, 16),
    val customButtons: Map<String, PreviewButtonData> = emptyMap(),
) {
    fun toBasicSettings(): PreviewMenuSettings.Basic {
        return PreviewMenuData(
            inventoryType = inventoryType,
            title = title,
            rewardSlots = rewardSlots,
            customButtons = customButtons
        ).toBasicSettings()
    }

    companion object {
        private val schemaJson = Json { encodeDefaults = true }

        fun TypedNestedSchemaBuilder<PreviewPageData>.defineEditor() {
            field(
                PreviewPageData::inventoryType,
                adapter = InventoryTypeFieldAdapter,
                displayName = "Page Inventory Type",
                iconMaterial = Material.CHEST,
                description = listOf("Inventory layout used for this custom preview page.")
            )
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

enum class PreviewType(val id: String) {
    AUTOMATIC("automatic"),
    CUSTOM_PAGES("custom-pages");

    companion object {
        val entries = listOf(AUTOMATIC, CUSTOM_PAGES)

        fun of(raw: String): PreviewType {
            return entries.firstOrNull { it.id.equals(raw, true) } ?: AUTOMATIC
        }
    }
}

private fun EditorFieldContext.isPreviewType(type: PreviewType): Boolean {
    val current = when (val currentValue = value as? JsonObject) {
        null -> null
        else -> (currentValue["previewType"] as? JsonPrimitive)?.content
    }
    if (current != null) {
        return current.equals(type.id, true)
    }

    val rootType = (root as? JsonObject)
        ?.get("preview")
        ?.let { it as? JsonObject }
        ?.get("previewType")
        ?.let { it as? JsonPrimitive }
        ?.content

    return (rootType ?: PreviewType.AUTOMATIC.id).equals(type.id, true)
}
