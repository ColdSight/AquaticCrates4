package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.ActionHandle
import gg.aquatic.execute.action.impl.CommandAction
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
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
    override fun toActionHandle(): ActionHandle<Player> {
        return ActionHandle(
            CommandAction,
            ObjectArguments(
                mapOf(
                    "command" to commands,
                    "player-executor" to playerExecutor
                )
            )
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<CommandRewardActionData>.defineEditor() {
            list(
                CommandRewardActionData::commands,
                displayName = "Commands",
                description = listOf("Commands executed when the reward is won."),
                newValueFactory = EditorEntryFactories.text("Enter command:")
            )
            field(
                CommandRewardActionData::playerExecutor,
                displayName = "Player Executor",
                description = listOf("If enabled, commands run as the player instead of console.")
            )
        }
    }
}
