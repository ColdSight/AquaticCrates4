package gg.aquatic.crates.message

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.Messages
import gg.aquatic.waves.serialization.editor.SerializableEditor
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object MessagesEditor {

    fun open(player: Player) {
        SerializableEditor.startEditing(
            player = player,
            title = Component.text("Editing messages"),
            serializer = MessagesFileData.serializer(),
            json = MessagesFormats.json,
            schema = MessagesLocaleEditorSchema,
            loadFresh = {
                MessageStorage.loadData()
            },
            onSave = { data ->
                MessageStorage.saveData(data)
                VirtualsCtx {
                    Messages.load()
                }
            }
        )
    }
}
