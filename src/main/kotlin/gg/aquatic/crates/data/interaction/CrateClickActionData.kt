package gg.aquatic.crates.data.interaction

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.interact.CrateClickBinder
import gg.aquatic.crates.interact.DestroyCrateClickAction
import gg.aquatic.crates.interact.OpenCrateClickAction
import gg.aquatic.crates.interact.PlayerProxyCrateClickActions
import gg.aquatic.crates.interact.PreviewCrateClickAction
import gg.aquatic.execute.ActionHandle
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
abstract class CrateClickActionData {
    abstract fun toActionHandle(): ActionHandle<CrateClickBinder>
}

@Serializable
@SerialName("preview")
class PreviewCrateClickActionData : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> =
        ActionHandle(PreviewCrateClickAction, ObjectArguments(emptyMap()))
}

@Serializable
@SerialName("open")
class OpenCrateClickActionData : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> =
        ActionHandle(OpenCrateClickAction, ObjectArguments(emptyMap()))
}

@Serializable
@SerialName("destroy")
class DestroyCrateClickActionData : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> =
        ActionHandle(DestroyCrateClickAction, ObjectArguments(emptyMap()))
}

@Serializable
@SerialName("message")
data class MessageCrateClickActionData(
    val lines: List<String> = listOf("<green>Interacted with the crate!")
) : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> {
        return ActionHandle(
            PlayerProxyCrateClickActions.message,
            ObjectArguments(
                mapOf("message" to PaperMessage.of(lines.map(String::toMMComponent)))
            )
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<MessageCrateClickActionData>.defineEditor() {
            list(
                MessageCrateClickActionData::lines,
                displayName = "Lines",
                description = listOf("Chat lines sent to the player after this click."),
                newValueFactory = EditorEntryFactories.text("Enter message line:")
            )
        }
    }
}

@Serializable
@SerialName("actionbar")
data class ActionbarCrateClickActionData(
    val message: String = "<green>Crate clicked!"
) : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> {
        return ActionHandle(
            PlayerProxyCrateClickActions.actionbar,
            ObjectArguments(mapOf("message" to message))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<ActionbarCrateClickActionData>.defineEditor() {
            field(
                ActionbarCrateClickActionData::message,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter actionbar message:", showFormattedPreview = true),
                displayName = "Message",
                description = listOf("Message shown above the hotbar after this click.")
            )
        }
    }
}

@Serializable
@SerialName("command")
data class CommandCrateClickActionData(
    val commands: List<String> = emptyList(),
    val playerExecutor: Boolean = false,
) : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> {
        return ActionHandle(
            PlayerProxyCrateClickActions.command,
            ObjectArguments(
                mapOf(
                    "command" to commands,
                    "player-executor" to playerExecutor
                )
            )
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<CommandCrateClickActionData>.defineEditor() {
            list(
                CommandCrateClickActionData::commands,
                displayName = "Commands",
                description = listOf("Commands executed after this click."),
                newValueFactory = EditorEntryFactories.text("Enter command:")
            )
            field(
                CommandCrateClickActionData::playerExecutor,
                displayName = "Player Executor",
                description = listOf("If enabled, commands run as the player instead of console.")
            )
        }
    }
}

@Serializable
@SerialName("sound")
data class SoundCrateClickActionData(
    val sound: String = "minecraft:entity.player.levelup",
    val volume: Double = 1.0,
    val pitch: Double = 1.0,
) : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> {
        return ActionHandle(
            PlayerProxyCrateClickActions.sound,
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
        fun TypedNestedSchemaBuilder<SoundCrateClickActionData>.defineEditor() {
            field(
                SoundCrateClickActionData::sound,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter sound key:"),
                displayName = "Sound",
                description = listOf("Namespaced sound key that should be played.")
            )
            field(
                SoundCrateClickActionData::volume,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter sound volume:", min = 0.0),
                displayName = "Volume",
                description = listOf("Playback volume of the sound.")
            )
            field(
                SoundCrateClickActionData::pitch,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter sound pitch:", min = 0.0),
                displayName = "Pitch",
                description = listOf("Playback pitch of the sound.")
            )
        }
    }
}

@Serializable
@SerialName("stop-sound")
data class StopSoundCrateClickActionData(
    val sound: String = "minecraft:entity.player.levelup",
) : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> {
        return ActionHandle(
            PlayerProxyCrateClickActions.stopSound,
            ObjectArguments(mapOf("sound" to sound))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<StopSoundCrateClickActionData>.defineEditor() {
            field(
                StopSoundCrateClickActionData::sound,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter sound key:"),
                displayName = "Sound",
                description = listOf("Namespaced sound key that should be stopped.")
            )
        }
    }
}

@Serializable
@SerialName("title")
data class TitleCrateClickActionData(
    val title: String = "<green>Crate!",
    val subtitle: String = "",
    val fadeIn: Int = 0,
    val stay: Int = 60,
    val fadeOut: Int = 0,
) : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> {
        return ActionHandle(
            PlayerProxyCrateClickActions.title,
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
        fun TypedNestedSchemaBuilder<TitleCrateClickActionData>.defineEditor() {
            field(
                TitleCrateClickActionData::title,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter title text:", showFormattedPreview = true),
                displayName = "Title",
                description = listOf("Main title text shown to the player.")
            )
            field(
                TitleCrateClickActionData::subtitle,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter subtitle text:", showFormattedPreview = true),
                displayName = "Subtitle",
                description = listOf("Secondary line shown below the title.")
            )
            field(
                TitleCrateClickActionData::fadeIn,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter fade in ticks:", min = 0),
                displayName = "Fade In",
                description = listOf("Ticks used for the title fade-in animation.")
            )
            field(
                TitleCrateClickActionData::stay,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter stay ticks:", min = 0),
                displayName = "Stay",
                description = listOf("Ticks the title stays fully visible.")
            )
            field(
                TitleCrateClickActionData::fadeOut,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter fade out ticks:", min = 0),
                displayName = "Fade Out",
                description = listOf("Ticks used for the title fade-out animation.")
            )
        }
    }
}

@Serializable
@SerialName("close-inventory")
class CloseInventoryCrateClickActionData : CrateClickActionData() {
    override fun toActionHandle(): ActionHandle<CrateClickBinder> {
        return ActionHandle(
            PlayerProxyCrateClickActions.closeInventory,
            ObjectArguments(emptyMap())
        )
    }
}
