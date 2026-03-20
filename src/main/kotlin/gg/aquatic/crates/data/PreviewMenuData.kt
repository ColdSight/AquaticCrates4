package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.preview.PreviewMenuSettings
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.settings.ButtonSettings
import gg.aquatic.kmenu.menu.settings.PrivateMenuSettings
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bukkit.Material

@Serializable
data class PreviewMenuData(
    val inventoryType: String = "GENERIC9X3",
    val title: String = "<yellow>Preview Menu",
    val rewardSlots: List<Int> = listOf(10, 11, 12, 13, 14, 15, 16),
    val previousPageSlots: List<Int> = emptyList(),
    val nextPageSlots: List<Int> = emptyList(),
    val customButtons: Map<String, PreviewButtonData> = emptyMap(),
) {

    fun toPreviewSettings(): PreviewMenuSettings.Basic {
        val resolvedType = runCatching { InventoryType.valueOf(inventoryType.trim()) }
            .getOrDefault(InventoryType.GENERIC9X3)

        val components = hashMapOf<String, gg.aquatic.kmenu.menu.settings.IButtonSettings>()

        if (previousPageSlots.isNotEmpty()) {
            components["prev-page"] = ButtonSettings(
                "prev-page",
                stackedItem(Material.ARROW) {
                    displayName = "<yellow>Previous page".toMMComponent()
                }.getItem(),
                previousPageSlots.distinct(),
                emptyList(),
                null,
                0,
                -1,
                null
            )
        }

        if (nextPageSlots.isNotEmpty()) {
            components["next-page"] = ButtonSettings(
                "next-page",
                stackedItem(Material.ARROW) {
                    displayName = "<yellow>Next page".toMMComponent()
                }.getItem(),
                nextPageSlots.distinct(),
                emptyList(),
                null,
                0,
                -1,
                null
            )
        }

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
            field(
                PreviewMenuData::inventoryType,
                TextFieldAdapter,
                TextFieldConfig(
                    prompt = "Enter inventory type:",
                    validator = CrateEditorValidators::validateInventoryType
                ),
                displayName = "Preview Inventory Type",
                description = listOf("Inventory layout used for the crate preview menu.")
            )
            field(
                PreviewMenuData::title,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter preview title:", showFormattedPreview = true),
                displayName = "Preview Title",
                description = listOf("Menu title shown at the top of the preview inventory.")
            )
            list(
                PreviewMenuData::rewardSlots,
                "Reward Slots",
                description = listOf("Slots where reward icons can appear in the preview menu."),
                newValueFactory = EditorEntryFactories.int("Enter reward slot:")
            )
            list(
                PreviewMenuData::previousPageSlots,
                "Previous Page Slots",
                description = listOf("Slots used for the previous-page button."),
                newValueFactory = EditorEntryFactories.int("Enter previous-page slot:")
            )
            list(
                PreviewMenuData::nextPageSlots,
                "Next Page Slots",
                description = listOf("Slots used for the next-page button."),
                newValueFactory = EditorEntryFactories.int("Enter next-page slot:")
            )
            map(
                PreviewMenuData::customButtons,
                displayName = "Custom Buttons",
                description = listOf("Additional static buttons shown in the preview menu."),
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
