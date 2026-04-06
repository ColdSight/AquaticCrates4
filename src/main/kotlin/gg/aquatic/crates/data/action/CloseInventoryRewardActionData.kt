package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionEditors.defineCloseInventoryToggle
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionHandles
import gg.aquatic.execute.ActionHandle
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("close-inventory")
data class CloseInventoryRewardActionData(
    val close: Boolean = true
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> = PlayerExecuteActionHandles.rewardCloseInventory()

    companion object {
        fun TypedNestedSchemaBuilder<CloseInventoryRewardActionData>.defineEditor() {
            defineCloseInventoryToggle(
                CloseInventoryRewardActionData::close,
                "If enabled, closes the current inventory after winning."
            )
        }
    }
}
