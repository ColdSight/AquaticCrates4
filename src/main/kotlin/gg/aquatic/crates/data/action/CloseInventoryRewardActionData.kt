package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.ActionHandle
import gg.aquatic.execute.action.impl.CloseInventory
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
@SerialName("close-inventory")
data class CloseInventoryRewardActionData(
    val close: Boolean = true
) : RewardActionData() {
    override fun toActionHandle(): ActionHandle<Player> {
        return ActionHandle(CloseInventory, ObjectArguments(emptyMap()))
    }

    companion object {
        fun TypedNestedSchemaBuilder<CloseInventoryRewardActionData>.defineEditor() {
            field(
                CloseInventoryRewardActionData::close,
                displayName = "Enabled",
                description = listOf("If enabled, closes the current inventory after winning.")
            )
        }
    }
}
