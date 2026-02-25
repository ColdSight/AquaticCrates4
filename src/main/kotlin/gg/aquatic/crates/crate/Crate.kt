package gg.aquatic.crates.crate

import gg.aquatic.clientside.serialize.ClientsideSettings
import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.open.OpenConditions
import gg.aquatic.crates.open.OpenPriceGroup
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.util.randomItem
import gg.aquatic.kholograms.Hologram
import gg.aquatic.kregistry.bootstrap.ContributionBuilder
import gg.aquatic.stacked.register
import gg.aquatic.stacked.stackedItem
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player

class Crate(
    val id: String,
    val displayName: Component,
    val hologram: Hologram.Settings?,
    val priceGroups: Collection<OpenPriceGroup>,
    val openConditions: OpenConditions = OpenConditions.DUMMY,
    val interactables: Collection<ClientsideSettings<*>>,
    val rewards: Collection<Reward>,
) {

    val crateItem by lazy {
        stackedItem(Material.CHEST) {
            displayName = Component.text("$id Crate (Admin Only)")
            lore += Component.text("Right click to spawn!")
        }
    }

    val crateItemStack by lazy { crateItem.getItem() }

    suspend fun tryOpen(player: Player) {
        for (group in priceGroups) {
            if (group.tryTake(player, 1)) {
                open(player)
                return
            }
        }
    }

    suspend fun open(player: Player, amount: Int = 1) {
        val filteredRewards = rewards.filter { it.condition(player) }

        repeat(amount) {
            filteredRewards.randomItem().winActions(player)
        }
    }
}