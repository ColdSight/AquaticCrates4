package gg.aquatic.crates.message

import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class MessageClickActionData(
    val type: MessageClickActionType = MessageClickActionType.RUN_COMMAND,
    val value: String = "",
) {
    companion object {
        fun TypedNestedSchemaBuilder<MessageClickActionData>.defineEditor() {
            field(
                MessageClickActionData::type,
                displayName = "Type",
                iconMaterial = Material.LEVER,
                description = listOf("Click action type applied to this message component.")
            )
            field(
                MessageClickActionData::value,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter click action value:"),
                displayName = "Value",
                iconMaterial = Material.PAPER,
                description = listOf(
                    "Value used by this click action.",
                    "URL for open-url, command for run/suggest, text for copy.",
                    "For page click use: next, prev, current, or a page number."
                )
            )
        }
    }
}
