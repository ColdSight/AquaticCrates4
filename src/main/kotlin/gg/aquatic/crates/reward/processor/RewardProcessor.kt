package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.reward.provider.ResolvedRewardProvider
import org.bukkit.entity.Player

interface RewardProcessor {
    suspend fun process(
        player: Player,
        crate: Crate,
        crateHandle: CrateHandle?,
        provider: ResolvedRewardProvider,
    )
}
