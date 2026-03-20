package gg.aquatic.crates.data

import gg.aquatic.crates.data.action.RewardActionData
import gg.aquatic.crates.data.action.RewardActionSelectionMenu
import gg.aquatic.crates.data.action.defineRewardActionEditor
import gg.aquatic.crates.data.condition.PlayerConditionData
import gg.aquatic.crates.data.condition.PlayerConditionSelectionMenu
import gg.aquatic.crates.data.condition.definePlayerConditionEditor
import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.execute.checkConditions
import gg.aquatic.execute.executeActions
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.menu.settings.ButtonSettings
import gg.aquatic.kmenu.menu.settings.ClickSettings
import gg.aquatic.kmenu.menu.settings.IButtonSettings
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class PreviewButtonData(
    val item: StackedItemData = StackedItemData(
        material = Material.PAPER.name,
        displayName = "<yellow>Custom Button"
    ),
    val slots: List<Int> = emptyList(),
    val viewConditions: List<@Polymorphic PlayerConditionData> = emptyList(),
    val clickActions: List<@Polymorphic RewardActionData> = emptyList(),
) {

    fun toButtonSettings(id: String): IButtonSettings {
        val builtItem = item.asStacked().getItem()
        val conditions = viewConditions.map { it.toConditionHandle() }
        val actions = clickActions.map { it.toActionHandle() }

        return ButtonSettings(
            id,
            builtItem,
            slots.distinct(),
            viewRequirements = listOf { player ->
                conditions.checkConditions(player)
            },
            click = ClickSettings(
                hashMapOf(
                    ButtonType.LEFT to mutableListOf({ player, updater ->
                        actions.executeActions(player, updater)
                    })
                )
            ),
            priority = 0,
            updateEvery = -1,
            failComponent = null
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<PreviewButtonData>.defineEditor() {
            group(PreviewButtonData::item) {
                with(StackedItemData) {
                    defineBasicEditor(
                        materialLabel = "Button Material",
                        nameLabel = "Button Name",
                        namePrompt = "Enter button display name:",
                        loreLabel = "Button Lore",
                        amountLabel = "Button Amount"
                    )
                }
            }
            list(
                PreviewButtonData::slots,
                displayName = "Slots",
                description = listOf("Slots where this custom button should appear."),
                newValueFactory = EditorEntryFactories.int("Enter button slot:")
            )
            list(
                PreviewButtonData::viewConditions,
                displayName = "View Conditions",
                description = listOf("Conditions that must pass for the button to be visible."),
                newValueFactory = PlayerConditionSelectionMenu.entryFactory
            ) {
                definePlayerConditionEditor()
            }
            list(
                PreviewButtonData::clickActions,
                displayName = "Click Actions",
                description = listOf("Actions executed when the player left-clicks this button."),
                newValueFactory = RewardActionSelectionMenu.entryFactory
            ) {
                defineRewardActionEditor()
            }
        }
    }
}
