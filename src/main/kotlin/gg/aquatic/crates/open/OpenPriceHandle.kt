package gg.aquatic.crates.open

import gg.aquatic.kurrency.Currency
import org.bukkit.entity.Player
import java.math.BigDecimal

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
}
