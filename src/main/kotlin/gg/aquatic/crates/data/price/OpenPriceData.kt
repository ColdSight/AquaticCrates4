package gg.aquatic.crates.data.price

import gg.aquatic.crates.open.OpenPriceHandle
import kotlinx.serialization.Serializable
import org.bukkit.inventory.ItemStack

@Serializable
abstract class OpenPriceData {
    abstract fun toHandle(crateId: String, keyItem: ItemStack): OpenPriceHandle
}
