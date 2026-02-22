package gg.aquatic.crates.reward

import gg.aquatic.crates.open.OpenPriceGroup
import org.bukkit.entity.Player

class RewardPurchaseHandler(
    val price: OpenPriceGroup,
    val failAction: suspend (player: Player) -> Unit
) {

    suspend fun tryPurchase(player: Player): Boolean {
        val success = price.tryTake(player, 1)
        if (!success) failAction(player)
        return success
    }

}