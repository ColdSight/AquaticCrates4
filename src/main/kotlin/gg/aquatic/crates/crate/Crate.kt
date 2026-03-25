package gg.aquatic.crates.crate

import gg.aquatic.crates.crate.preview.PreviewMenuSettings
import gg.aquatic.crates.data.interactable.CrateInteractableData
import gg.aquatic.crates.open.OpenConditions
import gg.aquatic.crates.open.OpenPriceGroup
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.util.randomItem
import gg.aquatic.execute.executeActions
import gg.aquatic.kholograms.Hologram
import gg.aquatic.stacked.event.StackedItemInteractEvent
import gg.aquatic.stacked.stackedItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

class Crate(
    val id: String,
    keyItemSupplier: () -> ItemStack,
    val displayName: Component,
    hologramSupplier: () -> Hologram.Settings?,
    priceGroupsSupplier: () -> Collection<OpenPriceGroup>,
    openConditionsSupplier: () -> OpenConditions = { OpenConditions.DUMMY },
    val interactables: Collection<CrateInteractableData>,
    rewardsSupplier: () -> Collection<Reward>,
    previewSupplier: () -> PreviewMenuSettings?,
) {

    val keyItem: ItemStack by lazy(keyItemSupplier)
    val hologram: Hologram.Settings? by lazy(hologramSupplier)
    val priceGroups: Collection<OpenPriceGroup> by lazy(priceGroupsSupplier)
    val openConditions: OpenConditions by lazy(openConditionsSupplier)
    val rewards: Collection<Reward> by lazy(rewardsSupplier)
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
        val filteredRewards = rewards.filter { it.canWin(player) }
        if (filteredRewards.isEmpty()) {
            return
        }

        repeat(amount) {
            filteredRewards.randomItem().winActions.executeActions(player)
        }
    }
}
