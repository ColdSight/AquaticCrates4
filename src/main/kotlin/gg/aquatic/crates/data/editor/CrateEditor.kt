package gg.aquatic.crates.data.editor

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.data.CrateData
import gg.aquatic.crates.data.CrateDataEditorSchema
import gg.aquatic.crates.data.CrateDataFormats
import gg.aquatic.crates.data.CrateStorage
import gg.aquatic.waves.serialization.editor.SerializableEditor
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object CrateEditor {

    fun open(player: Player, id: String) {
        SerializableEditor.startEditing(
            player = player,
            title = Component.text("Editing crate: $id"),
            serializer = CrateData.serializer(),
            json = CrateDataFormats.json,
            schema = CrateDataEditorSchema,
            loadFresh = {
                CrateStorage.loadData(id)
            },
            onSave = { crateData ->
                CrateStorage.save(id, crateData)
                VirtualsCtx {
                    CratesPlugin.reload()
                }
                player.sendMessage("Saved crate '$id'.")
            }
        )
    }
}
