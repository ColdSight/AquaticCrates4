package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionEditors.defineCommandEditor
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionHandles
import gg.aquatic.execute.ActionHandle
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("command")
data class CommandRewardActionData(
    val commands: List<String> = emptyList(),
    val playerExecutor: Boolean = false,
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> =
        PlayerExecuteActionHandles.rewardCommand(commands, playerExecutor)

    companion object {
        fun TypedNestedSchemaBuilder<CommandRewardActionData>.defineEditor() {
            defineCommandEditor(
                CommandRewardActionData::commands,
                CommandRewardActionData::playerExecutor,
                "Commands executed when the reward is won."
            )
        }
    }
}
