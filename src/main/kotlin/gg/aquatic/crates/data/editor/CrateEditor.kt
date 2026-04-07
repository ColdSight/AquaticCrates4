package gg.aquatic.crates.data.editor

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.Messages
import gg.aquatic.crates.data.CrateData
import gg.aquatic.crates.data.CrateDataEditorSchema
import gg.aquatic.crates.data.CrateDataFormats
import gg.aquatic.crates.data.CrateStorage
import gg.aquatic.crates.message.replacePlaceholder
import gg.aquatic.waves.serialization.editor.SerializableEditor
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object CrateEditor {

    fun open(player: Player, id: String) {
        SerializableEditor.startEditing(
            player = player,
            title = Component.text("Editing crate: $id"),
            serializer = CrateData.serializer(),
            yaml = CrateDataFormats.yaml,
            schema = CrateDataEditorSchema,
            loadFresh = {
                CrateStorage.loadData(id)
            },
            onSave = { crateData ->
                CrateStorage.save(id, crateData)
                VirtualsCtx {
                    CratesPlugin.reload()
                }
                Messages.CRATE_SAVED.message()
                    .replacePlaceholder("%crate_id%", id)
                    .send(player)
            }
        )
    }
}
