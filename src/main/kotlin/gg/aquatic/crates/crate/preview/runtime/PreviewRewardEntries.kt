package gg.aquatic.crates.crate.preview.runtime

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.reward.Reward
import gg.aquatic.kmenu.menu.Menu
import gg.aquatic.kmenu.menu.util.ListMenu
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.replace.PlaceholderContext
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

object PreviewRewardEntries {
    suspend fun buildPreviewItem(reward: Reward, player: Player, rewardLore: List<String>): ItemStack? {
        val item = if (reward.canWin(player)) reward.previewItem() else reward.fallbackItem?.invoke() ?: return null

        if (rewardLore.isEmpty()) {
            return item
        }

        val meta = item.itemMeta ?: return item
        val originalLore = meta.lore().orEmpty()
        val appendedLore = rewardLore.map { reward.updatePlaceholders(it, 1).toMMComponent() }
        meta.lore(originalLore + appendedLore)
        item.itemMeta = meta
        return item
    }

    suspend fun createEntry(
        reward: Reward,
        player: Player,
        rewardLore: List<String>,
    ): ListMenu.Entry<Reward>? {
        val context = PlaceholderContext.privateMenu()
        val item = buildPreviewItem(reward, player, rewardLore) ?: return null

        return ListMenu.Entry(
            reward,
            { item.clone() },
            context,
            { e ->
                if (reward.isPurchasable) {
                    reward.tryPurchase(player)
                    withContext(BukkitCtx.ofEntity(player)) {
                        player.updateInventory()
                    }
                } else {
                    reward.clickHandler(reward, player, e.buttonType)
                }
            }
        )
    }

    suspend fun mappedEntries(crate: Crate, player: Player, rewardLore: List<String>): List<ListMenu.Entry<Reward>> {
        return crate.rewardProvider.getRewards(player).mapNotNull { createEntry(it, player, rewardLore) }
    }

    suspend fun addRollingRandomRewards(
        menu: Menu,
        crate: Crate,
        player: Player,
        rewardLore: List<String>,
        randomRewardSlots: Collection<Int>,
        randomRewardSwitchTicks: Int,
        randomRewardUnique: Boolean,
    ) {
        if (randomRewardSlots.isEmpty()) {
            return
        }

        val randomEntries = mappedEntries(crate, player, rewardLore)
        val slots = if (randomRewardUnique) randomRewardSlots.take(randomEntries.size) else randomRewardSlots.toList()
        val entrySet = RollingPreviewEntrySet(randomEntries, randomRewardUnique)

        slots.forEach { slot ->
            menu.addComponent(
                RollingPreviewEntryButton(
                    id = "random-preview:$slot",
                    slots = listOf(slot),
                    priority = 1,
                    updateEvery = randomRewardSwitchTicks,
                    entrySet = entrySet,
                )
            )
        }
    }
}
