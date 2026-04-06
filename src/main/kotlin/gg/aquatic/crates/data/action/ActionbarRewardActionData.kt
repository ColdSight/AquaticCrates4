package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionEditors.defineFormattedMessageEditor
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionHandles
import gg.aquatic.execute.ActionHandle
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("actionbar")
data class ActionbarRewardActionData(
    val message: String = "<green>You won a reward!"
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> = PlayerExecuteActionHandles.rewardActionbar(message)

    companion object {
        fun TypedNestedSchemaBuilder<ActionbarRewardActionData>.defineEditor() {
            defineFormattedMessageEditor(
                ActionbarRewardActionData::message,
                "Enter actionbar message:",
                "Message shown above the hotbar when the reward is won."
            )
        }
    }
}
