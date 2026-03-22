package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.ActionHandle
import gg.aquatic.execute.action.impl.SoundAction
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("sound")
data class SoundRewardActionData(
    val sound: String = "minecraft:entity.player.levelup",
    val volume: Double = 1.0,
    val pitch: Double = 1.0,
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> {
        return ActionHandle(
            SoundAction,
            ObjectArguments(
                mapOf(
                    "sound" to sound,
                    "volume" to volume.toFloat(),
                    "pitch" to pitch.toFloat()
                )
            )
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<SoundRewardActionData>.defineEditor() {
            field(
                SoundRewardActionData::sound,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter sound key:"),
                displayName = "Sound",
                description = listOf("Namespaced sound key that should be played.")
            )
            field(
                SoundRewardActionData::volume,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter sound volume:", min = 0.0),
                displayName = "Volume",
                description = listOf("Playback volume of the sound.")
            )
            field(
                SoundRewardActionData::pitch,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter sound pitch:", min = 0.0),
                displayName = "Pitch",
                description = listOf("Playback pitch of the sound.")
            )
        }
    }
}
