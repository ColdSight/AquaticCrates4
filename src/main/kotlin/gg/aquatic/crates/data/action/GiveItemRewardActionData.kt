package gg.aquatic.crates.data.action

import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.execute.ActionHandle
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.entity.Player

@Serializable
@SerialName("give-item")
data class GiveItemRewardActionData(
    val item: StackedItemData = StackedItemData(material = Material.DIAMOND.name)
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> {
        val builtItem = item.asStacked().getItem()
        return inlinePlayerAction {
            it.inventory.addItem(builtItem.clone())
        }
    }

    companion object {
        fun TypedNestedSchemaBuilder<GiveItemRewardActionData>.defineEditor() {
            group(GiveItemRewardActionData::item) {
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
        }
    }
}
