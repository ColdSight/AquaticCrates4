package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.data.processor.RewardDisplayMenuSettings
import gg.aquatic.crates.reward.provider.ResolvedRewardProvider
import gg.aquatic.execute.ActionHandle
import gg.aquatic.crates.util.randomItem
import org.bukkit.entity.Player

class ChooseRewardProcessor(
    private val chooseCountRanges: Collection<gg.aquatic.crates.reward.RewardAmountRange>,
    private val uniqueRewards: Boolean,
    private val hiddenRewards: Boolean,
    private val onSelectActions: Collection<ActionHandle<Player>>,
    private val hiddenItem: org.bukkit.inventory.ItemStack?,
    private val menu: RewardDisplayMenuSettings,
) : RewardProcessor {
    override suspend fun process(player: Player, crate: Crate, crateHandle: CrateHandle?, provider: ResolvedRewardProvider) {
        val offerCount = if (provider.rewardCountRanges.isEmpty()) 1 else provider.rewardCountRanges.randomItem().roll()
        val offeredRewards = provider.rollRewards(player, countOverride = offerCount, unique = uniqueRewards)
            .take(menu.rewardSlots.size)
        if (offeredRewards.isEmpty()) {
            return
        }

        val chooseCount = (if (chooseCountRanges.isEmpty()) 1 else chooseCountRanges.randomItem().roll())
            .coerceAtLeast(1)
            .coerceAtMost(offeredRewards.size)

        ChooseRewardMenu.create(
            player = player,
            rewards = offeredRewards,
            chooseCount = chooseCount,
            hiddenRewards = hiddenRewards,
            onSelectActions = onSelectActions,
            hiddenItem = hiddenItem,
            settings = menu
        ).open()
    }
}
