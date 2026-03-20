package gg.aquatic.crates.data.action

import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.execute.ActionHandle
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

@Serializable
abstract class RewardActionData {
    abstract fun toActionHandle(): ActionHandle<Player>

    companion object {
        fun fromRewardItem(item: StackedItemData): RewardActionData {
            return GiveItemRewardActionData(item)
        }
    }
}
