package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionEditors.defineTitleEditor
import gg.aquatic.crates.data.playeraction.PlayerExecuteActionHandles
import gg.aquatic.execute.ActionHandle
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("title")
data class TitleRewardActionData(
    val title: String = "<green>Reward!",
    val subtitle: String = "",
    val fadeIn: Int = 0,
    val stay: Int = 60,
    val fadeOut: Int = 0,
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> =
        PlayerExecuteActionHandles.rewardTitle(title, subtitle, fadeIn, stay, fadeOut)

    companion object {
        fun TypedNestedSchemaBuilder<TitleRewardActionData>.defineEditor() {
            defineTitleEditor(
                TitleRewardActionData::title,
                TitleRewardActionData::subtitle,
                TitleRewardActionData::fadeIn,
                TitleRewardActionData::stay,
                TitleRewardActionData::fadeOut,
            )
        }
    }
}
