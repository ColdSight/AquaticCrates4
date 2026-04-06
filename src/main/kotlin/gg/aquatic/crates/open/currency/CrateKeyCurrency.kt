package gg.aquatic.crates.open.currency

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.crates.crate.Crate
import gg.aquatic.kurrency.Currency
import gg.aquatic.kurrency.impl.VirtualCurrency
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.math.RoundingMode

class CrateKeyCurrency(
    @Suppress("unused")
    private val crateId: String,
    private val crateResolver: () -> Crate?,
    private val backingCurrency: VirtualCurrency,
) : Currency {
    override val id: String = "aqcrates:key:$crateId"
    override val prefix: String = ""
    override val suffix: String = ""

    private fun resolveKeyItem(): ItemStack? {
        return crateResolver()?.keyItem?.clone()
    }

    private fun countPhysicalKeys(player: Player, keyItem: ItemStack): Int {
        var total = 0
        for (content in player.inventory.contents) {
            if (content?.isSimilar(keyItem) == true) {
                total += content.amount
            }
        }
        return total
    }

    private fun removePhysicalKeys(player: Player, keyItem: ItemStack, amount: Int) {
        if (amount <= 0) return

        var remaining = amount
        for (slot in player.inventory.contents.indices) {
            val content = player.inventory.getItem(slot) ?: continue
            if (!content.isSimilar(keyItem)) continue

            if (content.amount <= remaining) {
                remaining -= content.amount
                player.inventory.setItem(slot, null)
            } else {
                content.amount -= remaining
                player.inventory.setItem(slot, content)
                return
            }

            if (remaining == 0) {
                return
            }
        }
    }

    override suspend fun give(player: Player, amount: BigDecimal) = withContext(BukkitCtx.ofEntity(player)) {
        val toAddCount = amount.setScale(0, RoundingMode.DOWN).toInt()
        if (toAddCount < 1) return@withContext

        backingCurrency.give(player, BigDecimal.valueOf(toAddCount.toLong()).setScale(2, RoundingMode.HALF_DOWN))
    }

    override suspend fun take(player: Player, amount: BigDecimal) = withContext(BukkitCtx.ofEntity(player)) {
        val toRemoveCount = amount.setScale(0, RoundingMode.DOWN).toInt()
        if (toRemoveCount < 1) return@withContext

        val keyItem = resolveKeyItem()
        val physicalToTake = keyItem?.let { countPhysicalKeys(player, it).coerceAtMost(toRemoveCount) } ?: 0
        if (physicalToTake > 0 && keyItem != null) {
            removePhysicalKeys(player, keyItem, physicalToTake)
        }

        val remaining = toRemoveCount - physicalToTake
        if (remaining > 0) {
            backingCurrency.take(
                player,
                BigDecimal.valueOf(remaining.toLong()).setScale(2, RoundingMode.HALF_DOWN)
            )
        }
    }

    override suspend fun set(player: Player, amount: BigDecimal) = withContext(BukkitCtx.ofEntity(player)) {
        val balance = getBalance(player)
        val comparison = balance.compareTo(amount)

        if (comparison < 0) {
            give(player, amount.subtract(balance))
        } else if (comparison > 0) {
            take(player, balance.subtract(amount))
        }
    }

    override suspend fun getBalance(player: Player): BigDecimal = withContext(BukkitCtx.ofEntity(player)) {
        val physical = resolveKeyItem()?.let { countPhysicalKeys(player, it).toLong() } ?: 0L
        val virtual = backingCurrency.getBalance(player).setScale(0, RoundingMode.DOWN).toLong()
        BigDecimal.valueOf(physical + virtual).setScale(2, RoundingMode.HALF_DOWN)
    }

    override suspend fun tryTake(player: Player, amount: BigDecimal): Boolean = withContext(BukkitCtx.ofEntity(player)) {
        if (getBalance(player) < amount) {
            return@withContext false
        }
        val toTake = amount.setScale(0, RoundingMode.DOWN).toInt()
        if (toTake < 1) return@withContext true

        val keyItem = resolveKeyItem()
        val physicalAvailable = keyItem?.let { countPhysicalKeys(player, it) } ?: 0
        val physicalToTake = physicalAvailable.coerceAtMost(toTake)
        if (physicalToTake > 0 && keyItem != null) {
            removePhysicalKeys(player, keyItem, physicalToTake)
        }

        val remaining = toTake - physicalToTake
        if (remaining > 0) {
            if (!backingCurrency.tryTake(
                    player,
                    BigDecimal.valueOf(remaining.toLong()).setScale(2, RoundingMode.HALF_DOWN)
                )) {
                if (physicalToTake > 0) {
                    val refundItem = keyItem ?: return@withContext false
                    val stack = refundItem.clone()
                    stack.amount = physicalToTake
                    player.inventory.addItem(stack)
                }
                return@withContext false
            }
        }

        true
    }
}
