package gg.aquatic.crates.data.action

import gg.aquatic.common.toMMComponent
import gg.aquatic.execute.ActionHandle
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
        return inlinePlayerAction { player ->
            lines.forEach { line ->
                player.sendMessage(line.toMMComponent())
            }
        }
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
