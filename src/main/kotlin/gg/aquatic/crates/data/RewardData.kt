package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.data.action.RewardActionData
import gg.aquatic.crates.data.action.RewardActionSelectionMenu
import gg.aquatic.crates.data.action.defineRewardActionEditor
import gg.aquatic.crates.data.condition.PermissionPlayerConditionData
import gg.aquatic.crates.data.condition.PlayerConditionData
import gg.aquatic.crates.data.condition.PlayerConditionSelectionMenu
import gg.aquatic.crates.data.condition.definePlayerConditionEditor
import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.crates.reward.Reward
import gg.aquatic.kmenu.inventory.ClickType
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class RewardData(
    val displayName: String? = null,
    val previewItem: StackedItemData = StackedItemData(material = Material.CHEST.name),
    val rewardItem: StackedItemData = StackedItemData(material = Material.DIAMOND.name),
    val fallbackPreviewItem: StackedItemData? = null,
    val requiredPermission: String? = null,
    val conditions: List<@Polymorphic PlayerConditionData> = emptyList(),
    val rarity: RewardRarityData = RewardRarityData(),
    val chance: Double = 1.0,
    val winActions: List<@Polymorphic RewardActionData> = emptyList(),
) {

    fun toReward(id: String): Reward {
        val previewItemStack = previewItem.asStacked().getItem()
        val fallbackItemStack = fallbackPreviewItem?.asStacked()?.getItem()
        val rewardConditions = conditions.ifEmpty {
            requiredPermission?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { listOf(PermissionPlayerConditionData(it)) }
                ?: emptyList()
        }
        val resolvedActions = winActions.ifEmpty {
            listOf(RewardActionData.fromRewardItem(rewardItem))
        }

        return Reward(
            id = id,
            displayName = displayName?.toMMComponent(),
            previewItem = { previewItemStack.clone() },
            fallbackItem = fallbackItemStack?.let { built -> { built.clone() } },
            winActions = resolvedActions.map { it.toActionHandle() },
            conditions = rewardConditions.map { it.toConditionHandle() },
            purchaseManager = null,
            clickHandler = { _, _: ClickType -> },
            rarity = rarity.toRewardRarity(),
            chance = chance
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<RewardData>.defineEditor() {
            field(
                RewardData::displayName,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter reward display name:", showFormattedPreview = true),
                displayName = "Reward Name",
                description = listOf("Optional custom name used for the reward in previews.")
            )
            field(
                RewardData::requiredPermission,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter required permission, or leave empty:"),
                displayName = "Permission",
                description = listOf("Players must have this permission to win this reward.")
            )
            list(
                RewardData::conditions,
                displayName = "Conditions",
                description = listOf("Conditions that must pass before this reward can be selected."),
                newValueFactory = PlayerConditionSelectionMenu.entryFactory
            ) {
                definePlayerConditionEditor()
            }
            field(
                RewardData::chance,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter reward chance:", min = 0.0),
                displayName = "Chance",
                description = listOf("Relative weight used when the crate chooses a reward.")
            )

            list(
                RewardData::winActions,
                displayName = "Win Actions",
                description = listOf(
                    "Actions executed when this reward is won.",
                    "If empty, the reward item is given automatically."
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

            group(RewardData::rewardItem) {
                with(StackedItemData) {
                    defineBasicEditor(
                        materialLabel = "Reward Material",
                        nameLabel = "Reward Item Name",
                        namePrompt = "Enter reward item display name:",
                        loreLabel = "Reward Item Lore",
                        amountLabel = "Reward Amount"
                    )
                }
            }

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

            group(RewardData::rarity) {
                with(RewardRarityData) {
                    defineEditor()
                }
            }
        }
    }
}
