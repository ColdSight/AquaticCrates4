package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.ActionHandle
import gg.aquatic.execute.action.impl.TitleAction
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
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
    override fun toActionHandle(): ActionHandle<Player> {
        return ActionHandle(
            TitleAction,
            ObjectArguments(
                mapOf(
                    "title" to title,
                    "subtitle" to subtitle,
                    "fade-in" to fadeIn,
                    "stay" to stay,
                    "fade-out" to fadeOut
                )
            )
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<TitleRewardActionData>.defineEditor() {
            field(
                TitleRewardActionData::title,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter title text:", showFormattedPreview = true),
                displayName = "Title",
                description = listOf("Main title text shown to the player.")
            )
            field(
                TitleRewardActionData::subtitle,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter subtitle text:", showFormattedPreview = true),
                displayName = "Subtitle",
                description = listOf("Secondary line shown below the title.")
            )
            field(
                TitleRewardActionData::fadeIn,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter fade in ticks:", min = 0),
                displayName = "Fade In",
                description = listOf("Ticks used for the title fade-in animation.")
            )
            field(
                TitleRewardActionData::stay,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter stay ticks:", min = 0),
                displayName = "Stay",
                description = listOf("Ticks the title stays fully visible.")
            )
            field(
                TitleRewardActionData::fadeOut,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter fade out ticks:", min = 0),
                displayName = "Fade Out",
                description = listOf("Ticks used for the title fade-out animation.")
            )
        }
    }
}
