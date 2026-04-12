package gg.aquatic.crates.open

import gg.aquatic.kurrency.Currency
import org.bukkit.entity.Player
import java.math.BigDecimal

class OpenPriceGroup(
    val prices: List<OpenPriceHandle>,
    val onFail: suspend (player: Player) -> Unit
) {
    suspend fun tryTake(player: Player, amount: Int): Boolean {
        val success = tryTakeInternal(player, amount)
        if (!success) onFail(player)
        return success
    }

    private suspend fun tryTakeInternal(player: Player, amount: Int): Boolean {
        if (prices.isEmpty()) return true

        val taken = ArrayList<Pair<Currency, BigDecimal>>(prices.size)

        for (price in prices) {
            val currency = price.currency
            val amount = price.price * amount.toBigDecimal()
            if (amount == BigDecimal.ZERO) continue

            if (!currency.tryTake(player, amount)) {
                for ((takenCurrency, takenAmount) in taken.reversed()) {
                    takenCurrency.give(player, takenAmount)
                }
                return false
            }

            taken += currency to amount
        }

        return true
    }

    suspend fun maxAffordable(player: Player): Long {
        if (prices.isEmpty()) {
            return Long.MAX_VALUE
        }

        var maxAffordable = Long.MAX_VALUE
        for (price in prices) {
            maxAffordable = minOf(maxAffordable, price.maxAffordable(player))
        }

        return maxAffordable
    }

    suspend fun refund(player: Player, amount: Int) {
        if (amount <= 0) {
            return
        }

        for (price in prices) {
            price.give(player, amount)
        }
    }
}
