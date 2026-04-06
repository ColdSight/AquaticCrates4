package gg.aquatic.crates.message

import gg.aquatic.crates.Messages
import gg.aquatic.waves.serialization.editor.meta.TypedEditorSchemaBuilder
import kotlin.reflect.KProperty1

internal fun TypedEditorSchemaBuilder<MessagesFileData>.messageField(
    property: KProperty1<MessagesFileData, EditableMessageData>,
    metadata: Messages,
) {
    field(
        property,
        displayName = metadata.displayName,
        iconMaterial = metadata.icon,
        adapter = MessageEntryFieldAdapter,
        description = buildList {
            addAll(metadata.description)
            if (metadata.placeholders.isEmpty()) {
                add("Available placeholders: none.")
            } else {
                add("Available placeholders:")
                addAll(metadata.placeholders.map { "- $it" })
            }
        }
    )
    group(property) {
        with(EditableMessageData) {
            defineEditor()
        }
    }
}
