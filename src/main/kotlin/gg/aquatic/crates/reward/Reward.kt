package gg.aquatic.crates.reward

import gg.aquatic.crates.util.Weightable
import gg.aquatic.execute.ActionHandle
import gg.aquatic.execute.checkConditions
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.executeActions
import gg.aquatic.kmenu.inventory.ClickType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class Reward(
    val id: String,
    displayName: Component?,
    val previewItem: () -> ItemStack,
    val fallbackItem: (() -> ItemStack)?,
    val winActions: Collection<ActionHandle<Player>>,
    val conditions: Collection<ConditionHandle<Player>>,
    val purchaseManager: RewardPurchaseHandler?,
    val clickHandler: suspend (player: Player, clickType: ClickType) -> Unit,
    val rarity: RewardRarity,
    override var chance: Double
): Weightable {

    val isPurchasable: Boolean = purchaseManager != null

    suspend fun canWin(player: Player): Boolean {
        return conditions.checkConditions(player)
    }

    suspend fun tryPurchase(player: Player): Boolean {
        val success = purchaseManager?.tryPurchase(player) ?: false
        if (success) winActions.executeActions(player)
        return success
    }

    val displayName by lazy {
        displayName ?: previewItem().itemMeta.displayName() ?: Component.text(id)
    }
}
