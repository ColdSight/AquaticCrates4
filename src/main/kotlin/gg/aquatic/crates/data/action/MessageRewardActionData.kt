package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.common.toMMComponent
import gg.aquatic.execute.ActionHandle
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.waves.util.action.MessageAction
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("message")
data class MessageRewardActionData(
    val lines: List<String> = listOf("<green>You won a reward!")
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> {
        return ActionHandle(
            MessageAction,
            ObjectArguments(
                mapOf(
                    "message" to PaperMessage.of(lines.map(String::toMMComponent))
                )
            )
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<MessageRewardActionData>.defineEditor() {
            list(
                MessageRewardActionData::lines,
                displayName = "Lines",
                description = listOf("Chat lines sent to the player after winning."),
                newValueFactory = EditorEntryFactories.text("Enter message line:")
            )
        }
    }
}
