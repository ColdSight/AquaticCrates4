package gg.aquatic.crates.data

import gg.aquatic.crates.data.action.RewardActionSelectionMenu
import gg.aquatic.crates.data.action.defineRewardActionEditor
import gg.aquatic.crates.data.condition.PlayerConditionSelectionMenu
import gg.aquatic.crates.data.condition.definePlayerConditionEditor
import gg.aquatic.crates.data.editor.RewardRarityFieldAdapter
import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.crates.data.price.OpenPriceGroupData
import gg.aquatic.crates.data.range.RewardAmountRangeData
import gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import org.bukkit.Material

object RewardDataEditorSchema {
    fun TypedNestedSchemaBuilder<RewardData>.defineEditor() {
        field(
            RewardData::displayName,
            TextFieldAdapter,
            TextFieldConfig(prompt = "Enter reward display name:", showFormattedPreview = true),
            displayName = "Reward Name",
            iconMaterial = Material.NAME_TAG,
            description = listOf("Optional custom name used for the reward in previews.")
        )
        list(
            RewardData::conditions,
            displayName = "Conditions",
            iconMaterial = Material.IRON_BARS,
            description = listOf("Conditions that must pass before this reward can be selected."),
            newValueFactory = PlayerConditionSelectionMenu.entryFactory
        ) {
            definePlayerConditionEditor()
        }
        list(
            RewardData::limits,
            displayName = "Limits",
            iconMaterial = Material.CLOCK,
            description = listOf("Per-player rolling limits for how often this reward can be won.")
        ) {
            with(LimitData) { defineEditor() }
        }
        field(
            RewardData::chance,
            DoubleFieldAdapter,
            DoubleFieldConfig(prompt = "Enter reward chance:", min = 0.0),
            displayName = "Chance",
            iconMaterial = Material.EMERALD,
            description = listOf("Relative weight used inside the selected rarity.")
        )
        list(
            RewardData::amountRanges,
            displayName = "Amount Ranges",
            iconMaterial = Material.COPPER_INGOT,
            description = listOf(
                "Weighted random amount multiplier for this reward.",
                "If empty, the reward uses amount 1.",
                "The rolled value is available in win actions as %random-amount%."
            )
        ) {
            with(RewardAmountRangeData) {
                defineEditor(
                    minLabel = "Min Amount",
                    maxLabel = "Max Amount",
                    chanceLabel = "Range Weight"
                )
            }
        }
        list(
            RewardData::cost,
            displayName = "Cost",
            iconMaterial = Material.GOLD_INGOT,
            description = listOf(
                "Alternative price groups used to purchase this reward directly in preview.",
                "If empty, the reward cannot be purchased."
            ),
            newValueFactory = OpenPriceGroupData.defaultEntryFactory
        ) {
            with(OpenPriceGroupData) { defineEditor() }
        }
        field(
            RewardData::rarity,
            adapter = RewardRarityFieldAdapter,
            displayName = "Rarity",
            iconMaterial = Material.NETHER_STAR,
            description = listOf("Which crate rarity group this reward belongs to.")
        )
        list(
            RewardData::winActions,
            displayName = "Win Actions",
            iconMaterial = Material.BLAZE_POWDER,
            description = listOf(
                "Actions executed when this reward is won.",
                "If empty, the preview item is given automatically."
            ),
            newValueFactory = RewardActionSelectionMenu.entryFactory
        ) {
            defineRewardActionEditor()
        }
        group(RewardData::previewItem) {
            with(StackedItemData) {
                defineBasicEditor(
                    materialLabel = "Preview Material",
                    nameLabel = "Preview Name",
                    namePrompt = "Enter preview display name:",
                    loreLabel = "Preview Lore",
                    amountLabel = "Preview Amount"
                )
            }
        }
        field(
            RewardData::fallbackPreviewItem,
            displayName = "Fallback Item",
            iconMaterial = Material.CHEST_MINECART,
            description = listOf(
                "Optional item shown in preview when the player does not meet this reward's conditions.",
                "This lets you show a locked or unavailable variant instead of the normal preview item.",
                "If left unset, the normal preview item is shown even when the reward is unavailable.",
                "Click to create it. Press Q to clear it back to null."
            )
        )
        optionalGroup(RewardData::fallbackPreviewItem) {
            with(StackedItemData) {
                defineBasicEditor(
                    materialLabel = "Fallback Material",
                    nameLabel = "Fallback Name",
                    namePrompt = "Enter fallback display name:",
                    loreLabel = "Fallback Lore",
                    amountLabel = "Fallback Amount"
                )
            }
        }
    }
}
