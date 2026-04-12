package gg.aquatic.crates.open

import gg.aquatic.kurrency.Currency
import org.bukkit.entity.Player
import java.math.BigDecimal
import java.math.RoundingMode

class OpenPriceHandle(
    currencyResolver: () -> Currency,
    val price: BigDecimal
) {
    val currency: Currency by lazy(currencyResolver)

    suspend fun has(player: Player, amount: Int): Boolean {
        return currency.getBalance(player) >= price * amount.toBigDecimal()
    }

    suspend fun tryTake(player: Player, amount: Int): Boolean {
        return currency.tryTake(player, price * amount.toBigDecimal())
    }

    suspend fun give(player: Player, amount: Int) {
        if (amount > 0) {
            currency.give(player, price * amount.toBigDecimal())
        }
    }

    suspend fun maxAffordable(player: Player): Long {
        if (price <= BigDecimal.ZERO) {
            return Long.MAX_VALUE
        }

        val balance = currency.getBalance(player)
        return balance.divide(price, 0, RoundingMode.DOWN).toLong()
    }
}
