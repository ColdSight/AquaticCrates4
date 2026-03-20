package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.ActionHandle
import gg.aquatic.execute.action.impl.SoundStopAction
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("stop-sound")
data class StopSoundRewardActionData(
    val sound: String = "minecraft:entity.player.levelup"
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> {
        return ActionHandle(
            SoundStopAction,
            ObjectArguments(mapOf("sound" to sound))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<StopSoundRewardActionData>.defineEditor() {
            field(
                StopSoundRewardActionData::sound,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter sound key to stop:"),
                displayName = "Sound",
                description = listOf("Namespaced sound key that should be stopped.")
            )
        }
    }
}
