package gg.aquatic.crates.crate

import gg.aquatic.clientside.serialize.ClientsideSettings
import gg.aquatic.crates.open.OpenConditions
import gg.aquatic.crates.open.OpenPriceGroup
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.util.randomItem
import gg.aquatic.kholograms.Hologram
import net.kyori.adventure.text.Component
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