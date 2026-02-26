package gg.aquatic.crates.reward

import gg.aquatic.crates.util.Weightable
import gg.aquatic.kmenu.inventory.ClickType
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class Reward(
    val id: String,
    displayName: Component?,
    val previewItem: () -> ItemStack,
    val fallbackItem: (() -> ItemStack)?,
    val winActions: suspend (player: Player) -> Unit,
    val condition: suspend (Player) -> Boolean,
    val purchaseManager: RewardPurchaseHandler?,
    val clickHandler: suspend (player: Player, clickType: ClickType) -> Unit,
    val rarity: RewardRarity,
    override var chance: Double
): Weightable {

    val isPurchasable: Boolean = purchaseManager != null

    suspend fun tryPurchase(player: Player): Boolean {
        val success = purchaseManager?.tryPurchase(player) ?: false
        if (success) winActions(player)
        return success
    }

    val displayName by lazy {
        displayName ?: previewItem().itemMeta.displayName() ?: Component.text(id)
    }
}