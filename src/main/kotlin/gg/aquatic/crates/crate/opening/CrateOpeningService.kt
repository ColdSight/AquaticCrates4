package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.limit.LimitService
import gg.aquatic.crates.milestone.CrateMilestoneService
import gg.aquatic.crates.reward.processor.RolledReward
import gg.aquatic.crates.reward.provider.ResolvedRewardProvider
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
        return try {
            var openedAny = false

            repeat(amount.coerceAtLeast(1)) {
                val grantedRewards = processSingleOpening(session, crateHandle) ?: return@repeat
                openedAny = true
                logGrantedRewards(session.player, session.crate, grantedRewards)
            }

            OpeningSessionManager.finish(session)
            openedAny
        } catch (throwable: Throwable) {
            OpeningSessionManager.finish(session, failed = true)
            throw throwable
        }
    }

    private suspend fun processSingleOpening(
        session: OpeningSession,
        crateHandle: CrateHandle?,
    ): List<RolledReward>? {
        if (!canAttemptOpen(session, crateHandle)) {
            return null
        }
        if (!takeOpeningPrice(session)) {
            return null
        }

        val resolvedProvider = resolveWinnableProvider(session) ?: return null
        session.stage = OpeningStage.PROCESSING_REWARDS

        val grantedRewards = session.crate.rewardProcessor.process(
            player = session.player,
            crate = session.crate,
            crateHandle = crateHandle,
            provider = resolvedProvider,
        )

        if (grantedRewards.isEmpty()) {
            return emptyList()
        }

        val milestoneRewards = CrateMilestoneService.grantReachedMilestones(session.player, session.crate)
        return grantedRewards + milestoneRewards
    }

    private suspend fun canAttemptOpen(session: OpeningSession, crateHandle: CrateHandle?): Boolean {
        val crate = session.crate
        val player = session.player
        if (crateHandle != null && crate.keyMustBeHeld && !crate.isHoldingKey(player)) {
            return false
        }
        if (!crate.openConditions.check(player, crate, crateHandle)) {
            return false
        }
        return LimitService.canOpenCrate(player, crate.id, crate.limits)
    }

    private suspend fun takeOpeningPrice(session: OpeningSession): Boolean {
        val crate = session.crate
        val player = session.player
        return crate.priceGroups.isEmpty() || crate.priceGroups.any { it.tryTake(player, 1) }
    }

    private suspend fun resolveWinnableProvider(session: OpeningSession): ResolvedRewardProvider? {
        val resolvedProvider = session.crate.rewardProvider.resolve(session.player)
        return resolvedProvider.takeIf { provider -> provider.rewards.any { it.canWin(session.player) } }
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
