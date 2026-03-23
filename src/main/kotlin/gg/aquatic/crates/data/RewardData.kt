package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.data.action.RewardActionData
import gg.aquatic.crates.data.action.RewardActionSelectionMenu
import gg.aquatic.crates.data.action.defineRewardActionEditor
import gg.aquatic.crates.data.condition.PlayerConditionData
import gg.aquatic.crates.data.condition.PlayerConditionSelectionMenu
import gg.aquatic.crates.data.condition.definePlayerConditionEditor
import gg.aquatic.crates.data.editor.RewardRarityFieldAdapter
import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.crates.data.price.OpenPriceGroupData
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.RewardPurchaseHandler
import gg.aquatic.execute.executeActions
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.ClickType
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import org.bukkit.inventory.ItemStack
import org.bukkit.Material

@Serializable
data class RewardData(
    val displayName: String? = null,
    val previewItem: StackedItemData = StackedItemData(material = Material.CHEST.name),
    val fallbackPreviewItem: StackedItemData? = null,
    val conditions: List<@Polymorphic PlayerConditionData> = emptyList(),
    val rarity: String = CrateData.DEFAULT_RARITY_ID,
    val chance: Double = 1.0,
    val cost: List<OpenPriceGroupData> = emptyList(),
    val winActions: List<@Polymorphic RewardActionData> = emptyList(),
) {

    fun normalized(
        availableRarities: Set<String>,
        fallbackRarityId: String,
        currentCrateId: String? = null,
        existingCrateIds: Set<String> = emptySet(),
    ): RewardData {
        val resolvedRarity = rarity.takeIf { it in availableRarities } ?: fallbackRarityId
        return copy(
            rarity = resolvedRarity,
            cost = cost.map { it.normalized(currentCrateId, existingCrateIds) }
        )
    }

    fun toReward(id: String, crateId: String, crateKeyItem: ItemStack, rarity: gg.aquatic.crates.reward.RewardRarity): Reward {
        val previewItemStack = previewItem.asStacked().getItem()
        val fallbackItemStack = fallbackPreviewItem?.asStacked()?.getItem()
        val resolvedActions = winActions.ifEmpty {
            listOf(RewardActionData.fromRewardItem(previewItem))
        }
        val purchaseManager = cost.firstOrNull { it.prices.isNotEmpty() }?.toOpenPriceGroup(crateId, crateKeyItem)?.let { priceGroup ->
            RewardPurchaseHandler(
                price = priceGroup,
                failAction = { }
            )
        }

        return Reward(
            id = id,
            displayName = displayName?.toMMComponent(),
            previewItem = { previewItemStack.clone() },
            fallbackItem = fallbackItemStack?.let { built -> { built.clone() } },
            winActions = resolvedActions.map { it.toActionHandle() },
            conditions = conditions.map { it.toConditionHandle() },
            purchaseManager = purchaseManager,
            clickHandler = { reward, player, clickType: ButtonType ->
                when (clickType) {
                    ButtonType.LEFT -> {
                        if (reward.isPurchasable) {
                            reward.tryPurchase(player)
                        }
                    }
                    ButtonType.SHIFT_LEFT -> {
                        if (player.hasPermission("aquaticcrates.admin")) {
                            reward.winActions.executeActions(player)
                        }
                    }
                    else -> {}
                }
            },
            rarity = rarity,
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
            field(
                RewardData::chance,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter reward chance:", min = 0.0),
                displayName = "Chance",
                iconMaterial = Material.EMERALD,
                description = listOf("Relative weight used inside the selected rarity.")
            )
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
                with(OpenPriceGroupData) {
                    defineEditor()
                }
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
}
