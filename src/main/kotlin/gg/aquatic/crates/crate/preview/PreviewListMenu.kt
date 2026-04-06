package gg.aquatic.crates.crate.preview

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.reward.Reward
import gg.aquatic.kmenu.menu.util.ListMenu
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.replace.PlaceholderContext
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class PreviewListMenu private constructor(
    player: Player,
    val crate: Crate,
    val crateHandle: CrateHandle?,
    entries: List<Entry<Reward>>,
    private val settings: PreviewMenuSettings.Basic,
) : ListMenu<Reward>(
    settings.invSettings.title,
    settings.invSettings.type,
    player,
    entries,
    Sorting.empty(),
    settings.rewardSlots,
) {

    companion object {
        private suspend fun buildPreviewItem(reward: Reward, player: Player, rewardLore: List<String>): ItemStack? {
            val item = if (reward.canWin(player)) {
                reward.previewItem()
            } else reward.fallbackItem?.invoke() ?: return null

            if (rewardLore.isEmpty()) {
                return item
            }

            val meta = item.itemMeta ?: return item
            val originalLore = meta.lore().orEmpty()
            val appendedLore = rewardLore.map {
                reward.updatePlaceholders(it, 1).toMMComponent()
            }
            meta.lore(originalLore + appendedLore)
            item.itemMeta = meta
            return item
        }

        private suspend fun createEntry(
            reward: Reward,
            player: Player,
            rewardLore: List<String>,
        ): Entry<Reward>? {
            val context = PlaceholderContext.privateMenu()
            val item = buildPreviewItem(reward, player, rewardLore) ?: return null

            return Entry(
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

        suspend fun mappedEntries(crate: Crate, player: Player, rewardLore: List<String>): List<Entry<Reward>> {
            return crate.rewardProvider.getRewards(player).mapNotNull {
                createEntry(it, player, rewardLore)
            }
        }

        suspend fun create(
            player: Player,
            crate: Crate,
            crateHandle: CrateHandle?,
            settings: PreviewMenuSettings.Basic
        ): PreviewListMenu {
            val menu = PreviewListMenu(player, crate, crateHandle, mappedEntries(crate, player, settings.rewardLore), settings)
            menu.addButtons()
            menu.addRandomRewards()
            settings.anvilSettings?.applyTo(menu)
            return menu
        }
    }

    private val context = PlaceholderContext.privateMenu()

    private suspend fun addButtons() {
        for ((id, comp) in settings.invSettings.components) {
            if (id.lowercase() == "next-page") {
                injectNextButton(comp)
                continue
            }
            if (id.lowercase() == "prev-page") {
                injectPreviousButton(comp)
                continue
            }

            addComponent(comp.create(context))
        }
    }

    private suspend fun addRandomRewards() {
        if (settings.randomRewardSlots.isEmpty()) {
            return
        }

        val randomEntries = mappedEntries(crate, player, settings.rewardLore)
        val slots = if (settings.randomRewardUnique) {
            settings.randomRewardSlots.take(randomEntries.size)
        } else settings.randomRewardSlots.toList()
        val coordinator = RollingRandomRewardCoordinator(randomEntries, settings.randomRewardUnique)

        slots.forEach { slot ->
            addComponent(
                RollingRandomRewardButton(
                    id = "random-preview:$slot",
                    slots = listOf(slot),
                    priority = 1,
                    updateEvery = settings.randomRewardSwitchTicks,
                    coordinator = coordinator,
                )
            )
        }
    }
}
