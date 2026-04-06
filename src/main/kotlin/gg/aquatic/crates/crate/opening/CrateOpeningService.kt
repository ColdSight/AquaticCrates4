package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.limit.LimitService
import gg.aquatic.crates.reward.processor.RolledReward
import gg.aquatic.crates.stats.CrateStats
import gg.aquatic.crates.stats.LoggedOpening
import gg.aquatic.crates.stats.LoggedRewardWin
import org.bukkit.entity.Player

object CrateOpeningService {
    fun reserveOpening(player: Player, crate: Crate): OpeningSession? {
        return OpeningSessionManager.tryStart(player, crate)
    }

    suspend fun tryOpen(player: Player, crate: Crate, crateHandle: CrateHandle? = null, amount: Int = 1): Boolean {
        val session = reserveOpening(player, crate) ?: return false
        return executeOpening(session, crateHandle, amount)
    }

    suspend fun executeOpening(session: OpeningSession, crateHandle: CrateHandle? = null, amount: Int = 1): Boolean {
        val crate = session.crate
        val player = session.player
        return try {
            var openedAny = false

            repeat(amount.coerceAtLeast(1)) {
                if (crateHandle != null && crate.keyMustBeHeld && !crate.isHoldingKey(player)) {
                    return@repeat
                }
                if (!crate.openConditions.check(player, crate, crateHandle)) {
                    return@repeat
                }
                if (!LimitService.canOpenCrate(player, crate.id, crate.limits)) {
                    return@repeat
                }
                if (crate.priceGroups.isNotEmpty() && crate.priceGroups.none { it.tryTake(player, 1) }) {
                    return@repeat
                }

                val resolvedProvider = crate.rewardProvider.resolve(player)
                if (resolvedProvider.rewards.none { it.canWin(player) }) {
                    return@repeat
                }

                session.stage = OpeningStage.PROCESSING_REWARDS
                openedAny = true

                val grantedRewards = crate.rewardProcessor.process(
                    player = player,
                    crate = crate,
                    crateHandle = crateHandle,
                    provider = resolvedProvider,
                )
                logGrantedRewards(player, crate, grantedRewards)
            }

            OpeningSessionManager.finish(session)
            openedAny
        } catch (throwable: Throwable) {
            OpeningSessionManager.finish(session, failed = true)
            throw throwable
        }
    }

    private suspend fun logGrantedRewards(player: Player, crate: Crate, rolledRewards: List<RolledReward>) {
        if (crate.disableOpenStats || rolledRewards.isEmpty()) {
            return
        }

        CrateStats.logOpening(
            LoggedOpening(
                playerUuid = player.uniqueId,
                crateId = crate.id,
                openedAtMillis = System.currentTimeMillis(),
                rewards = rolledRewards.map(LoggedRewardWin::from)
            )
        )
    }
}
