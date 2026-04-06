package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionEditors.defineMessageLinesEditor
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionHandles
import gg.aquatic.execute.ActionHandle
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("message")
data class MessageRewardActionData(
    val lines: List<String> = listOf("<green>You won a reward!")
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> = PlayerExecuteActionHandles.rewardMessage(lines)

    companion object {
        fun TypedNestedSchemaBuilder<MessageRewardActionData>.defineEditor() {
            defineMessageLinesEditor(
                MessageRewardActionData::lines,
                "Chat lines sent to the player after winning."
            )
        }
    }
}
