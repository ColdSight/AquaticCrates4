package gg.aquatic.crates.open

import org.bukkit.entity.Player
import java.math.BigDecimal

class OpenPriceGroup(
    val prices: List<OpenPriceHandle>,
    val onFail: suspend (player: Player) -> Unit
) {

    suspend fun balance(player: Player): BigDecimal {
        return prices.sumOf { it.currency.getBalance(player) }
    }

    suspend fun tryTake(player: Player, amount: Int): Boolean {
        val success = tryTakeInternal(player, amount)
        if (!success) onFail(player)
        return success
    }

    private suspend fun tryTakeInternal(player: Player, amount: Int): Boolean {
        if (!prices.all { it.has(player, amount) }) return false
        return prices.all { it.tryTake(player, amount) }
    }
}