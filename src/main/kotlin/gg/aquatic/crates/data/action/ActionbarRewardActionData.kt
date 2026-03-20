package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.ActionHandle
import gg.aquatic.execute.action.impl.ActionbarAction
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("actionbar")
data class ActionbarRewardActionData(
    val message: String = "<green>You won a reward!"
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> {
        return ActionHandle(
            ActionbarAction,
            ObjectArguments(mapOf("message" to message))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<ActionbarRewardActionData>.defineEditor() {
            field(
                ActionbarRewardActionData::message,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter actionbar message:", showFormattedPreview = true),
                displayName = "Message",
                description = listOf("Message shown above the hotbar when the reward is won.")
            )
        }
    }
}
