package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.data.processor.RewardDisplayMenuSettings
import gg.aquatic.crates.reward.provider.ResolvedRewardProvider
import org.bukkit.entity.Player

class BasicRewardProcessor(
    private val resultMenu: RewardDisplayMenuSettings?,
) : RewardProcessor {
    override suspend fun process(player: Player, crate: Crate, crateHandle: CrateHandle?, provider: ResolvedRewardProvider) {
        val rolledRewards = provider.rollRewards(player)
        if (rolledRewards.isEmpty()) {
            return
        }

        rolledRewards.forEach { rolled ->
            rolled.reward.win(player, rolled.amount)
        }

        resultMenu?.let { menuSettings ->
            RewardShowcaseMenu.create(player, rolledRewards, menuSettings).open()
        }
    }
}
