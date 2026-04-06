package gg.aquatic.crates.reward
import gg.aquatic.crates.limit.LimitHandle
import gg.aquatic.crates.limit.LimitService
import gg.aquatic.crates.util.Weightable
import gg.aquatic.crates.util.randomItem
import gg.aquatic.execute.ActionHandle
import gg.aquatic.execute.checkConditions
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.executeActions
import gg.aquatic.kmenu.inventory.ButtonType
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class Reward(
    val id: String,
    val crateId: String,
    displayName: Component?,
    val previewItem: () -> ItemStack,
    val fallbackItem: (() -> ItemStack)?,
    val winActions: Collection<ActionHandle<Player>>,
    val conditions: Collection<ConditionHandle<Player>>,
    val purchaseManager: RewardPurchaseHandler?,
    val amountRanges: Collection<RewardAmountRange>,
    val clickHandler: suspend (reward: Reward, player: Player, clickType: ButtonType) -> Unit,
    val rarity: RewardRarity,
    val limits: Collection<LimitHandle>,
    override var chance: Double
) : Weightable {

    val isPurchasable: Boolean = purchaseManager != null

    suspend fun canWin(player: Player): Boolean {
        return conditions.checkConditions(player) && LimitService.canWinReward(player, crateId, id, limits)
    }

    fun rollAmount(): Int {
        return if (amountRanges.isEmpty()) 1 else amountRanges.randomItem().roll()
    }

    suspend fun win(player: Player, randomAmount: Int = rollAmount()) {
        if (winActions.isEmpty()) {
            giveDefaultItem(player, randomAmount)
            return
        }

        winActions.executeActions(player) { _, str ->
            updatePlaceholders(str, randomAmount)
        }
    }

    suspend fun tryWin(player: Player, randomAmount: Int = rollAmount()): Boolean {
        if (!canWin(player)) {
            return false
        }

        win(player, randomAmount)
        return true
    }

    suspend fun tryPurchase(player: Player): Boolean {
        val success = purchaseManager?.tryPurchase(player) ?: false
        if (success) {
            win(player)
        }
        return success
    }

    val displayName by lazy {
        displayName ?: previewItem().itemMeta.displayName() ?: Component.text(id)
    }

    fun updatePlaceholders(str: String, randomAmount: Int): String {
        return str
            .replace("%random-amount%", randomAmount.toString())
            .replace("%reward-id%", id)
            .replace("%reward-name%", PlainTextComponentSerializer.plainText().serialize(displayName))
            .replace("%reward-rarity-id%", rarity.id)
            .replace("%reward-rarity-name%", PlainTextComponentSerializer.plainText().serialize(rarity.displayName))
    }

    private fun giveDefaultItem(player: Player, randomAmount: Int) {
        val item = previewItem()
        val originalAmount = item.amount.coerceAtLeast(1)
        var remaining = (originalAmount * randomAmount).coerceAtLeast(1)
        val maxStackSize = item.maxStackSize.coerceAtLeast(1)

        while (remaining > 0) {
            val stackAmount = minOf(remaining, maxStackSize)
            remaining -= stackAmount

            val stack = item.clone()
            stack.amount = stackAmount

            val leftovers = player.inventory.addItem(stack)
            leftovers.values.forEach { player.world.dropItemNaturally(player.location, it) }
        }
    }
}
