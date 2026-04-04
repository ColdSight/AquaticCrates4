package gg.aquatic.crates.crate

import gg.aquatic.crates.crate.preview.PreviewMenuSettings
import gg.aquatic.crates.data.interactable.CrateInteractableData
import gg.aquatic.crates.open.OpenConditions
import gg.aquatic.crates.open.OpenPriceGroup
import gg.aquatic.crates.reward.processor.RewardProcessor
import gg.aquatic.crates.reward.provider.RewardProvider
import gg.aquatic.kholograms.Hologram
import gg.aquatic.stacked.event.StackedItemInteractEvent
import gg.aquatic.stacked.stackedItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

/**
 * Rewards Table
 * ID | Name (crate:name)
 *
 * Openings
 * ID | Crate Name - indexed | Timestamp | PlayerUUID | Amount
 *
 * Opened Rewards
 * Name (crate:name - Indexed) | Timestamp | PlayerUUID - indexed | cratename - indexed | Amount | Opening ID
 */
class Crate(
    val id: String,
    keyItemSupplier: () -> ItemStack,
    val displayName: Component,
    hologramSupplier: () -> Hologram.Settings?,
    priceGroupsSupplier: () -> Collection<OpenPriceGroup>,
    openConditionsSupplier: () -> OpenConditions = { OpenConditions.DUMMY },
    val interactables: Collection<CrateInteractableData>,
    rewardProviderSupplier: () -> RewardProvider,
    rewardProcessorSupplier: () -> RewardProcessor,
    previewSupplier: () -> PreviewMenuSettings?,
) {

    val keyItem: ItemStack by lazy(keyItemSupplier)
    val hologram: Hologram.Settings? by lazy(hologramSupplier)
    val priceGroups: Collection<OpenPriceGroup> by lazy(priceGroupsSupplier)
    val openConditions: OpenConditions by lazy(openConditionsSupplier)
    val rewardProvider: RewardProvider by lazy(rewardProviderSupplier)
    val rewardProcessor: RewardProcessor by lazy(rewardProcessorSupplier)
    val preview: PreviewMenuSettings? by lazy(previewSupplier)

    val crateItem by lazy {
        stackedItem(Material.CHEST) {
            displayName = Component.text("$id Crate (Admin Only)")
            lore += Component.text("Right click to spawn!")
        }
    }

    internal fun handleCrateItemInteractions(event: StackedItemInteractEvent) {
        val player = event.player
        event.cancelled = true
        if (!player.hasPermission("aqcrates.admin")) {
            player.inventory.itemInMainHand.amount = 0
            return
        }

        val originalEvent = event.originalEvent
        if (originalEvent !is PlayerInteractEvent) {
            return
        }

        if (originalEvent.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        player.sendMessage("You have placed the crate!")

        val rotation = player.yaw - 180
        val location = originalEvent.clickedBlock!!.location.add(originalEvent.blockFace.direction).apply { yaw = rotation }

        CrateHandler.spawnCrate(location, this, true)
    }

    val crateItemStack by lazy { crateItem.getItem() }

    suspend fun tryOpen(player: Player, crateHandle: CrateHandle? = null): Boolean {
        if (!openConditions.check(player, this, crateHandle)) {
            return false
        }

        if (priceGroups.isEmpty()) {
            open(player)
            return true
        }

        for (group in priceGroups) {
            if (group.tryTake(player, 1)) {
                open(player)
                return true
            }
        }

        return false
    }

    suspend fun open(player: Player, amount: Int = 1) {
        val resolvedProvider = rewardProvider.resolve(player)
        if (resolvedProvider.rewards.none { it.canWin(player) }) {
            return
        }

        repeat(amount) {
            rewardProcessor.process(player, this, null, resolvedProvider)
        }
    }
}
