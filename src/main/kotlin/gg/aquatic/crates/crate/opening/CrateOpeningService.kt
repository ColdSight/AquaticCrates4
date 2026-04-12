package gg.aquatic.crates.crate.opening

import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.debug.CratesDebug
import gg.aquatic.crates.debug.CratesLogCategory
import gg.aquatic.crates.limit.LimitService
import gg.aquatic.crates.milestone.CrateMilestoneService
import gg.aquatic.crates.open.OpenPriceGroup
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.processor.BasicRewardProcessor
import gg.aquatic.crates.reward.processor.ChooseRewardProcessor
import gg.aquatic.crates.reward.processor.CompiledMassOpeningSampler
import gg.aquatic.crates.reward.processor.MassRandom
import gg.aquatic.crates.reward.processor.MassRewardGrant
import gg.aquatic.crates.reward.processor.RolledReward
import gg.aquatic.crates.reward.provider.ResolvedRewardProvider
import gg.aquatic.crates.stats.CrateStats
import gg.aquatic.crates.stats.CrateStatsTimeframe
import gg.aquatic.crates.stats.LoggedOpening
import gg.aquatic.crates.stats.LoggedRewardWin
import gg.aquatic.crates.util.randomItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.math.BigInteger
import java.util.concurrent.ThreadLocalRandom
import org.bukkit.entity.Player

object CrateOpeningService {
    fun reserveOpening(player: Player, crate: Crate): OpeningSession? {
        return OpeningSessionManager.tryStart(player, crate)
    }

    suspend fun tryOpenResult(
        player: Player,
        crate: Crate,
        crateHandle: CrateHandle? = null,
        amount: BigInteger = BigInteger.ONE,
        ignoreKeyRequirement: Boolean = false,
    ): OpeningExecutionResult {
        val session = reserveOpening(player, crate) ?: return OpeningExecutionResult(success = false, openedCount = BigInteger.ZERO)
        return executeOpeningResult(session, crateHandle, amount, ignoreKeyRequirement)
    }

    suspend fun tryOpen(
        player: Player,
        crate: Crate,
        crateHandle: CrateHandle? = null,
        amount: BigInteger = BigInteger.ONE,
        ignoreKeyRequirement: Boolean = false,
    ): Boolean {
        return tryOpenResult(player, crate, crateHandle, amount, ignoreKeyRequirement).success
    }

    suspend fun executeOpening(
        session: OpeningSession,
        crateHandle: CrateHandle? = null,
        amount: BigInteger = BigInteger.ONE,
        ignoreKeyRequirement: Boolean = false,
    ): Boolean {
        return executeOpeningResult(session, crateHandle, amount, ignoreKeyRequirement).success
    }


    suspend fun executeOpeningResult(
        session: OpeningSession,
        crateHandle: CrateHandle? = null,
        amount: BigInteger = BigInteger.ONE,
        ignoreKeyRequirement: Boolean = false,
    ): OpeningExecutionResult {
        val startedAtNanos = System.nanoTime()
        val requestedAmount = amount.max(BigInteger.ONE)
        var debugStrategy = "EXACT"
        var openedCount = BigInteger.ZERO
        var openedAny = false

        return try {
            if (requestedAmount > BigInteger.ONE && (session.crate.rewardProcessor is BasicRewardProcessor || session.crate.rewardProcessor is ChooseRewardProcessor)) {
                var remaining = requestedAmount
                val chunkGrants = ArrayList<MassRewardGrant>()
                // Keep the outer API on BigInteger, but execute the optimized mass pipeline in Int-sized chunks.
                // This avoids rewriting the hot path to arbitrary-precision math while still supporting huge requests.
                var milestoneBaseOpenCount = if (session.crate.milestoneManager.hasAnyMilestones && CrateStats.ready) {
                    CrateStats.getPlayerCrateOpens(session.player.uniqueId, session.crate.id, CrateStatsTimeframe.ALL_TIME)
                } else {
                    0L
                }
                while (remaining > BigInteger.ZERO) {
                    val chunk = remaining.min(Int.MAX_VALUE.toBigInteger()).toInt()
                    val chunkResult = processMassOpeningChunk(session, crateHandle, chunk, ignoreKeyRequirement)
                    debugStrategy = chunkResult.strategy?.name ?: debugStrategy
                    openedCount += chunkResult.aggregation.openedCount.toBigInteger()
                    openedAny = openedAny || chunkResult.success
                    val chunkOpenedCount = chunkResult.aggregation.openedCount.toLong()
                    val chunkMilestoneGrants = aggregateMassMilestones(
                        player = session.player,
                        crate = session.crate,
                        baseOpenCount = milestoneBaseOpenCount,
                        openedCount = chunkOpenedCount
                    )
                    val mergedChunkGrants = MassOpeningSupport.mergeMassRewardGrants(
                        chunkResult.aggregation.grants + chunkMilestoneGrants
                    )
                    chunkGrants += mergedChunkGrants
                    if (chunkOpenedCount > 0L) {
                        logGrantedRewards(
                            player = session.player,
                            crate = session.crate,
                            openCount = chunkOpenedCount,
                            grantedRewards = mergedChunkGrants
                        )
                        milestoneBaseOpenCount = milestoneBaseOpenCount.saturatedAdd(chunkOpenedCount)
                    }
                    if (!chunkResult.success || chunkResult.aggregation.openedCount < chunk) {
                        break
                    }
                    remaining -= chunk.toBigInteger()
                }

                if (chunkGrants.isNotEmpty()) {
                    val mergedGrants = MassOpeningSupport.mergeMassRewardGrants(chunkGrants)
                    mergedGrants.forEach { grant ->
                        grant.reward.massWin(session.player, grant.winCount, grant.totalAmount)
                    }
                }

                OpeningSessionManager.finish(session)
                return OpeningExecutionResult(success = openedAny, openedCount = openedCount)
            }

            var remaining = requestedAmount
            while (remaining > BigInteger.ZERO) {
                val chunk = remaining.min(Int.MAX_VALUE.toBigInteger()).toInt()
                repeat(chunk) {
                    val grantedRewards = processSingleOpening(session, crateHandle, ignoreKeyRequirement) ?: return@repeat
                    openedAny = true
                    openedCount += BigInteger.ONE
                    logGrantedRewards(session.player, session.crate, grantedRewards)
                }
                remaining -= chunk.toBigInteger()
            }

            OpeningSessionManager.finish(session)
            OpeningExecutionResult(success = openedAny, openedCount = openedCount)
        } catch (throwable: Throwable) {
            OpeningSessionManager.finish(session, failed = true)
            throw throwable
        } finally {
            val elapsedMillis = (System.nanoTime() - startedAtNanos) / 1_000_000.0
            CratesDebug.message(
                session.player,
                1,
                "Crate opening computed in ${"%.3f".format(java.util.Locale.US, elapsedMillis)} ms " +
                    "(strategy=$debugStrategy, opened=$openedCount/$requestedAmount, success=$openedAny)"
            )
        }
    }

    private suspend fun processMassOpeningChunk(
        session: OpeningSession,
        crateHandle: CrateHandle?,
        requestedAmount: Int,
        ignoreKeyRequirement: Boolean,
    ): MassOpenChunkResult {
        if (!canAttemptOpen(session, crateHandle, ignoreKeyRequirement)) {
            return emptyMassChunkResult()
        }

        val priceSelection = selectMassPriceSelection(session, requestedAmount, ignoreKeyRequirement)
        val effectiveAmount = selectEffectiveMassOpenAmount(session, requestedAmount, priceSelection)
        if (effectiveAmount < 1) {
            priceSelection.group?.onFail?.invoke(session.player)
            return emptyMassChunkResult()
        }

        val selectedPriceGroup = priceSelection.group
        if (!takeMassOpeningPrice(session.player, selectedPriceGroup, effectiveAmount, ignoreKeyRequirement)) {
            return emptyMassChunkResult()
        }

        val massContext = createMassOpeningContext(session)
            ?: return emptyMassChunkResult()
        session.stage = OpeningStage.PROCESSING_REWARDS

        val strategy = selectMassChunkStrategy(massContext, effectiveAmount)
        CratesDebug.log(
            CratesLogCategory.INTERACTION,
            2,
            "Mass opening crate '${session.crate.id}' using strategy $strategy for amount=$effectiveAmount " +
                "(statsReady=${CrateStats.ready}, randomRewardCount=${massContext.compiledSampler.hasRandomRewardCount}, " +
                "randomRewardAmounts=${massContext.compiledSampler.hasRandomRewardAmounts}, rewardLimits=${!massContext.rewardTracker.hasNoLimits})"
        )

        val rewardGrants = aggregateMassChunkRewards(effectiveAmount, massContext, strategy)
        if (rewardGrants.openedCount < 1) {
            if (!ignoreKeyRequirement) {
                selectedPriceGroup?.refund(session.player, effectiveAmount)
            }
            return MassOpenChunkResult(success = false, aggregation = MassOpenAggregation(0, emptyList()), strategy = strategy)
        }
        if (rewardGrants.openedCount < effectiveAmount) {
            if (!ignoreKeyRequirement) {
                selectedPriceGroup?.refund(session.player, effectiveAmount - rewardGrants.openedCount)
            }
        }

        return MassOpenChunkResult(
            success = true,
            aggregation = rewardGrants,
            strategy = strategy
        )
    }

    private suspend fun processSingleOpening(
        session: OpeningSession,
        crateHandle: CrateHandle?,
        ignoreKeyRequirement: Boolean,
    ): List<RolledReward>? {
        if (!canAttemptOpen(session, crateHandle, ignoreKeyRequirement)) {
            return null
        }
        if (!takeOpeningPrice(session.player, session.crate.priceGroups, ignoreKeyRequirement)) {
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

    private suspend fun canAttemptOpen(
        session: OpeningSession,
        crateHandle: CrateHandle?,
        ignoreKeyRequirement: Boolean,
    ): Boolean {
        val crate = session.crate
        val player = session.player
        if (!ignoreKeyRequirement && crate.keyMustBeHeld && !crate.isHoldingKey(player)) {
            return false
        }
        if (!crate.openConditions.check(player, crate, crateHandle)) {
            return false
        }
        return LimitService.canOpenCrate(player, crate.id, crate.limits)
    }

    private suspend fun takeOpeningPrice(
        player: Player,
        priceGroups: Collection<OpenPriceGroup>,
        ignoreKeyRequirement: Boolean,
    ): Boolean {
        if (ignoreKeyRequirement) {
            return true
        }
        return priceGroups.isEmpty() || priceGroups.any { it.tryTake(player, 1) }
    }

    private suspend fun resolveWinnableProvider(session: OpeningSession): ResolvedRewardProvider? {
        val resolvedProvider = session.crate.rewardProvider.resolve(session.player)
        return resolvedProvider.takeIf { provider -> provider.rewards.any { it.canWin(session.player) } }
    }

    private suspend fun logGrantedRewards(player: Player, crate: Crate, rolledRewards: List<RolledReward>) {
        if (crate.disableOpenStats || rolledRewards.isEmpty()) {
            return
        }

        logGrantedRewards(
            player,
            crate,
            1L,
            MassOpeningSupport.mergeMassRewardGrants(rolledRewards.map { MassRewardGrant(it.reward, 1L, it.amount.toLong()) })
        )
    }

    private suspend fun logGrantedRewards(player: Player, crate: Crate, openCount: Long, grantedRewards: List<MassRewardGrant>) {
        if (crate.disableOpenStats || openCount <= 0L) {
            return
        }

        CrateStats.logOpening(
            LoggedOpening(
                playerUuid = player.uniqueId,
                crateId = crate.id,
                openedAtMillis = System.currentTimeMillis(),
                rewards = grantedRewards.map {
                    LoggedRewardWin(
                        rewardId = it.reward.id,
                        rarityId = it.reward.rarity.id,
                        amount = it.totalAmount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                        winCount = it.winCount
                    )
                },
                openCount = openCount
            )
        )
    }

    private suspend fun maxOpenCountFromCrateLimits(session: OpeningSession): Long {
        val limits = session.crate.limits
        if (limits.isEmpty() || !CrateStats.ready) {
            return Long.MAX_VALUE
        }

        var maxOpenCount = Long.MAX_VALUE
        for (limit in limits) {
            val current = CrateStats.getPlayerCrateOpens(session.player.uniqueId, session.crate.id, limit.timeframe)
            val remaining = limit.limit.toLong() - current
            maxOpenCount = minOf(maxOpenCount, remaining.coerceAtLeast(0L))
        }

        return maxOpenCount
    }

    private suspend fun selectMassPriceGroup(player: Player, groups: Collection<OpenPriceGroup>, requestedAmount: Long): MassPriceSelection {
        if (groups.isEmpty()) {
            return MassPriceSelection(null, Long.MAX_VALUE)
        }

        var bestGroup: OpenPriceGroup? = null
        var bestAffordable = 0L
        for (group in groups) {
            val affordable = group.maxAffordable(player)
            if (affordable >= requestedAmount) {
                return MassPriceSelection(group, affordable)
            }
            if (affordable > bestAffordable) {
                bestAffordable = affordable
                bestGroup = group
            }
        }

        return MassPriceSelection(bestGroup, bestAffordable)
    }

    private suspend fun aggregateMassRewards(
        amount: Int,
        tracker: MassRewardLimitTracker,
        compiledSampler: CompiledMassOpeningSampler,
        strategy: MassAggregationStrategy,
    ): MassOpenAggregation {
        val rewards = compiledSampler.rewards
        if (rewards.isEmpty()) {
            return MassOpenAggregation(0, emptyList())
        }

        // Pick the cheapest correct strategy first. Each branch below is a specialization of the same
        // reward-generation problem under stricter assumptions about determinism, limits and randomness.
        if (strategy == MassAggregationStrategy.DETERMINISTIC) {
            tryAggregateDeterministicSingleReward(compiledSampler, amount, tracker)?.let { return it }
        }
        if (strategy == MassAggregationStrategy.LIMIT_AWARE) {
            return aggregateMassRewardsLimitAware(compiledSampler, amount, tracker)
        }
        if (strategy == MassAggregationStrategy.MULTINOMIAL) {
            return aggregateMassRewardsMultinomial(compiledSampler, amount)
        }
        if (strategy == MassAggregationStrategy.VECTORIZED) {
            return aggregateMassRewardsVectorized(compiledSampler, amount)
        }

        val aggregates = LinkedHashMap<Reward, MutableMassRewardGrant>()
        val staticPool = rewards.takeIf { tracker.hasNoLimits }
        val countRandom = createMassRandom(amount.toLong(), rewards.size.toLong(), tracker.hasNoLimits.hashCode().toLong())
        var openedCount = 0

        for (@Suppress("unused") openIndex in 0 until amount) {
            val availableRewards = staticPool ?: rewards.filter(tracker::canGrant)
            if (availableRewards.isEmpty()) {
                break
            }

            val targetCount = compiledSampler.sampleRewardCount(countRandom)
            repeat(targetCount.coerceAtLeast(1)) {
                val currentRewards = staticPool ?: availableRewards.filter(tracker::canGrant)
                if (currentRewards.isEmpty()) {
                    return@repeat
                }

                val reward = currentRewards.randomItem()
                val rolledAmount = reward.rollAmount()
                tracker.recordGrant(reward)
                val aggregate = aggregates.getOrPut(reward) { MutableMassRewardGrant(reward) }
                aggregate.winCount += 1L
                aggregate.totalAmount += rolledAmount.toLong()
            }

            openedCount += 1
        }

        return MassOpenAggregation(
            openedCount = openedCount,
            grants = aggregates.values.map { it.toImmutable() }
        )
    }

    private fun aggregateMassUniqueChooseRewards(
        amount: Int,
        tracker: MassRewardLimitTracker,
        compiledSampler: CompiledMassOpeningSampler,
    ): MassOpenAggregation {
        val rewards = compiledSampler.rewards
        if (rewards.isEmpty() || amount < 1) {
            return MassOpenAggregation(0, emptyList())
        }

        val random = createMassRandom(amount.toLong(), rewards.size.toLong(), 0xC001CE42L)
        val aggregates = LinkedHashMap<Reward, MutableMassRewardGrant>()
        var openedCount = 0

        repeat(amount) {
            val availableRewards = rewards.filter(tracker::canGrant)
            if (availableRewards.isEmpty()) {
                return@repeat
            }

            val targetCount = compiledSampler.sampleRewardCount(random)
                .coerceAtLeast(1)
                .coerceAtMost(availableRewards.size)
            if (targetCount < 1) {
                return@repeat
            }

            val openPool = availableRewards.toMutableList()
            repeat(targetCount) {
                if (openPool.isEmpty()) {
                    return@repeat
                }

                val reward = openPool.removeAt(weightedRewardIndex(openPool, random))
                val rolledAmount = compiledSampler.sampleRewardAmount(rewards.indexOf(reward), random)
                tracker.recordGrant(reward)
                val aggregate = aggregates.getOrPut(reward) { MutableMassRewardGrant(reward) }
                aggregate.winCount += 1L
                aggregate.totalAmount += rolledAmount.toLong()
            }

            openedCount += 1
        }

        return MassOpenAggregation(
            openedCount = openedCount,
            grants = aggregates.values.map { it.toImmutable() }
        )
    }

    private fun aggregateMassRewardsMultinomial(
        compiledSampler: CompiledMassOpeningSampler,
        amount: Int,
    ): MassOpenAggregation {
        // Every open contributes the same number of draws, so the whole batch can be collapsed into one multinomial
        // experiment over totalDraws = opens * drawsPerOpen.
        val rewardCountPerOpen = compiledSampler.fixedRewardCount ?: return aggregateMassRewardsVectorizedSingleWorker(compiledSampler, amount)
        compiledSampler.allFixedRewardAmounts() ?: return aggregateMassRewardsVectorizedSingleWorker(compiledSampler, amount)
        val totalDraws = rewardCountPerOpen.toLong() * amount.toLong()
        val random = createMassRandom(amount.toLong(), compiledSampler.rewards.size.toLong(), totalDraws)
        val counts = gg.aquatic.crates.reward.processor.MassSampling.sampleMultinomialCounts(
            totalDraws = totalDraws,
            probabilities = compiledSampler.normalizedRewardProbabilities,
            random = random
        )

        return MassOpenAggregation(
            openedCount = amount,
            grants = buildList {
                for (index in compiledSampler.rewards.indices) {
                    val winCount = counts[index]
                    if (winCount > 0L) {
                        add(
                            MassRewardGrant(
                                reward = compiledSampler.rewards[index],
                                winCount = winCount,
                                totalAmount = compiledSampler.sampleRewardAmountTotal(index, winCount, random)
                            )
                        )
                    }
                }
            }
        )
    }

    private fun aggregateMassRewardsLimitAware(
        compiledSampler: CompiledMassOpeningSampler,
        amount: Int,
        tracker: MassRewardLimitTracker,
    ): MassOpenAggregation {
        // This path is only used when one open produces at most one reward draw. That lets us map the assigned
        // draw count back to openedCount without replaying each open individually.
        val rewards = compiledSampler.rewards
        if (rewards.isEmpty() || amount < 1) {
            return MassOpenAggregation(0, emptyList())
        }

        val random = createMassRandom(amount.toLong(), rewards.size.toLong(), 0x1CEB00DAL)
        val totalDraws = compiledSampler.sampleTotalRewardDraws(amount.toLong(), random)
        if (totalDraws <= 0L) {
            return MassOpenAggregation(0, emptyList())
        }

        val capacities = LongArray(rewards.size) { index -> tracker.maxGrantableWins(rewards[index]) }
        // Sample the full multinomial draw count first, then redistribute overflow away from capped rewards.
        // This keeps the fast aggregated path while still respecting per-reward win limits.
        val sampledCounts = MassOpeningSupport.sampleCappedRewardCounts(
            totalDraws,
            compiledSampler.normalizedRewardProbabilities,
            capacities,
            random
        )
        val winCounts = MassOpeningSupport.enforceRewardCap(sampledCounts, capacities)
        if (!sampledCounts.contentEquals(winCounts)) {
            CratesDebug.log(
                CratesLogCategory.INTERACTION,
                1,
                "Mass opening reward limit overflow detected for crate '${compiledSampler.rewards.firstOrNull()?.crateId ?: "unknown"}'; counts were clamped defensively."
            )
        }
        val assignedDraws = winCounts.sum()
        if (assignedDraws <= 0L) {
            return MassOpenAggregation(0, emptyList())
        }

        return MassOpenAggregation(
            openedCount = minOf(amount.toLong(), assignedDraws).toInt(),
            grants = buildList {
                for (index in rewards.indices) {
                    val winCount = winCounts[index]
                    if (winCount > 0L) {
                        add(
                            MassRewardGrant(
                                reward = rewards[index],
                                winCount = winCount,
                                totalAmount = compiledSampler.sampleRewardAmountTotal(index, winCount, random)
                            )
                        )
                    }
                }
            }
        )
    }

    private suspend fun aggregateMassMilestones(
        player: Player,
        crate: Crate,
        baseOpenCount: Long,
        openedCount: Long,
    ): List<MassRewardGrant> {
        // Milestones depend on the cumulative open-count range, so they are evaluated after reward aggregation
        // against [baseOpenCount, baseOpenCount + openedCount] instead of being mixed into the reward sampler.
        if (openedCount < 1L || !crate.milestoneManager.hasAnyMilestones || !CrateStats.ready) {
            return emptyList()
        }

        tryAggregateDeterministicMilestones(player, crate, baseOpenCount, openedCount)?.let { return it }

        val endOpenCount = baseOpenCount + openedCount
        val aggregates = LinkedHashMap<Reward, MutableMassRewardGrant>()

        crate.milestoneManager.milestoneHitsInRange(baseOpenCount, endOpenCount).forEach { hit ->
            hit.milestone.rewards.forEach { reward ->
                if (!reward.canWin(player)) {
                    return@forEach
                }

                val aggregate = aggregates.getOrPut(reward) { MutableMassRewardGrant(reward) }
                repeat(hit.hitCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
                    aggregate.winCount += 1L
                    aggregate.totalAmount += reward.rollAmount().toLong()
                }

                var remaining = hit.hitCount - Int.MAX_VALUE.toLong().coerceAtMost(hit.hitCount)
                while (remaining > 0L) {
                    val batch = remaining.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    repeat(batch) {
                        aggregate.winCount += 1L
                        aggregate.totalAmount += reward.rollAmount().toLong()
                    }
                    remaining -= batch.toLong()
                }
            }
        }

        return aggregates.values.map { it.toImmutable() }
    }

    private fun tryAggregateDeterministicSingleReward(
        compiledSampler: CompiledMassOpeningSampler,
        amount: Int,
        tracker: MassRewardLimitTracker,
    ): MassOpenAggregation? {
        if (compiledSampler.rewards.size != 1) {
            return null
        }

        val reward = compiledSampler.rewards.first()
        val rewardCountPerOpen = compiledSampler.fixedRewardCount ?: return null
        val rewardAmount = compiledSampler.fixedRewardAmount(0) ?: return null
        if (rewardCountPerOpen < 1 || rewardAmount < 1) {
            return MassOpenAggregation(0, emptyList())
        }

        val maxWins = tracker.maxGrantableWins(reward)
        val openedCount = minOf(amount.toLong(), maxWins.floorDiv(rewardCountPerOpen.toLong()))
        if (openedCount < 1L) {
            return MassOpenAggregation(0, emptyList())
        }

        val winCount = openedCount * rewardCountPerOpen.toLong()
        return MassOpenAggregation(
            openedCount = openedCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            grants = listOf(
                MassRewardGrant(
                    reward = reward,
                    winCount = winCount,
                    totalAmount = winCount * rewardAmount.toLong()
                )
            )
        )
    }

    private suspend fun aggregateMassRewardsVectorized(
        compiledSampler: CompiledMassOpeningSampler,
        amount: Int,
    ): MassOpenAggregation = coroutineScope {
        // Generic fast path when we still need actual sampling work but can safely split independent opens across
        // workers and merge only aggregated win/amount totals afterwards.
        if (amount < 1) {
            return@coroutineScope MassOpenAggregation(0, emptyList())
        }

        val rewards = compiledSampler.rewards
        val rewardCountPerOpen = compiledSampler.fixedRewardCount
        val rewardCount = rewards.size
        val expectedDraws = rewardCountPerOpen?.toLong()?.times(amount.toLong()) ?: amount.toLong()
        val workerCount = MassOpeningSupport.selectWorkerCount(expectedDraws)
        val rootSeed = createSamplingSeed(amount.toLong(), rewardCount.toLong(), expectedDraws)
        if (workerCount == 1) {
            return@coroutineScope aggregateMassRewardsVectorizedSingleWorker(compiledSampler, amount, rootSeed)
        }

        val openChunkSize = MassOpeningSupport.openChunkSize(amount, workerCount)
        val partials = (0 until workerCount).mapNotNull { workerIndex ->
            val startOpen = workerIndex * openChunkSize
            val endOpen = minOf(amount, startOpen + openChunkSize)
            if (startOpen >= endOpen) {
                null
            } else {
                async(MassOpeningSupport.workerDispatcher(workerCount)) {
                    aggregateMassRewardsChunk(
                        compiledSampler = compiledSampler,
                        openCount = endOpen - startOpen,
                        seed = MassRandom.mixSeed(rootSeed + workerIndex.toLong())
                    )
                }
            }
        }.awaitAll()

        val mergedWins = LongArray(rewardCount)
        val mergedAmounts = LongArray(rewardCount)
        partials.forEach { partial ->
            for (index in 0 until rewardCount) {
                mergedWins[index] += partial.winCounts[index]
                mergedAmounts[index] += partial.amountSums[index]
            }
        }

        return@coroutineScope MassOpenAggregation(
            openedCount = amount,
            grants = buildList {
                for (index in 0 until rewardCount) {
                    val winCount = mergedWins[index]
                    if (winCount > 0L) {
                        add(
                            MassRewardGrant(
                                reward = rewards[index],
                                winCount = winCount,
                                totalAmount = mergedAmounts[index]
                            )
                        )
                    }
                }
            }
        )
    }

    private fun aggregateMassRewardsVectorizedSingleWorker(
        compiledSampler: CompiledMassOpeningSampler,
        amount: Int,
        rootSeed: Long = createSamplingSeed(amount.toLong(), compiledSampler.rewards.size.toLong(), amount.toLong()),
    ): MassOpenAggregation {
        val partial = aggregateMassRewardsChunk(compiledSampler, amount, rootSeed)
        return MassOpenAggregation(
            openedCount = amount,
            grants = buildList {
                for (index in compiledSampler.rewards.indices) {
                    val winCount = partial.winCounts[index]
                    if (winCount > 0L) {
                        add(
                            MassRewardGrant(
                                reward = compiledSampler.rewards[index],
                                winCount = winCount,
                                totalAmount = partial.amountSums[index]
                            )
                        )
                    }
                }
            }
        )
    }

    private fun aggregateMassRewardsChunk(
        compiledSampler: CompiledMassOpeningSampler,
        openCount: Int,
        seed: Long,
    ): MassRewardChunkAggregation {
        val random = MassRandom(seed)
        val rewardCount = compiledSampler.rewards.size
        val winCounts = LongArray(rewardCount)
        val amountSums = LongArray(rewardCount)
        val deterministicRewardCount = compiledSampler.fixedRewardCount
        val fixedAmounts = compiledSampler.allFixedRewardAmounts()

        if (deterministicRewardCount != null) {
            val totalDraws = deterministicRewardCount.toLong() * openCount.toLong()
            var processed = 0L
            if (fixedAmounts != null) {
                while (processed < totalDraws) {
                    val rewardIndex = compiledSampler.sampleRewardIndex(random)
                    winCounts[rewardIndex]++
                    amountSums[rewardIndex] += fixedAmounts[rewardIndex].toLong()
                    processed++
                }
            } else {
                while (processed < totalDraws) {
                    val rewardIndex = compiledSampler.sampleRewardIndex(random)
                    winCounts[rewardIndex]++
                    amountSums[rewardIndex] += compiledSampler.sampleRewardAmount(rewardIndex, random).toLong()
                    processed++
                }
            }
            return MassRewardChunkAggregation(winCounts, amountSums)
        }

        repeat(openCount) {
            val rewardsPerOpen = compiledSampler.sampleRewardCount(random).coerceAtLeast(1)
            if (fixedAmounts != null) {
                repeat(rewardsPerOpen) {
                    val rewardIndex = compiledSampler.sampleRewardIndex(random)
                    winCounts[rewardIndex]++
                    amountSums[rewardIndex] += fixedAmounts[rewardIndex].toLong()
                }
            } else {
                repeat(rewardsPerOpen) {
                    val rewardIndex = compiledSampler.sampleRewardIndex(random)
                    winCounts[rewardIndex]++
                    amountSums[rewardIndex] += compiledSampler.sampleRewardAmount(rewardIndex, random).toLong()
                }
            }
        }

        return MassRewardChunkAggregation(winCounts, amountSums)
    }

    private suspend fun tryAggregateDeterministicMilestones(
        player: Player,
        crate: Crate,
        baseOpenCount: Long,
        openedCount: Long,
    ): List<MassRewardGrant>? {
        val endOpenCount = baseOpenCount + openedCount
        val aggregates = LinkedHashMap<Reward, MutableMassRewardGrant>()

        suspend fun addMilestoneRewards(rewardCount: Long, rewards: Collection<Reward>): Boolean {
            if (rewardCount <= 0L) {
                return true
            }

            for (reward in rewards) {
                if (reward.limits.isNotEmpty() || !reward.canWinIgnoringLimits(player)) {
                    return false
                }

                val rewardAmount = reward.amountRanges.randomItem().roll()
                val aggregate = aggregates.getOrPut(reward) { MutableMassRewardGrant(reward) }
                aggregate.winCount += rewardCount
                aggregate.totalAmount += rewardCount * rewardAmount.toLong()
            }
            return true
        }

        for (hit in crate.milestoneManager.milestoneHitsInRange(baseOpenCount, endOpenCount)) {
            if (!addMilestoneRewards(hit.hitCount, hit.milestone.rewards)) {
                return null
            }
        }

        return aggregates.values.map { it.toImmutable() }
    }

    private fun selectMassAggregationStrategy(
        compiledSampler: CompiledMassOpeningSampler,
        tracker: MassRewardLimitTracker,
        amount: Int,
    ): MassAggregationStrategy {
        if (!tracker.hasNoLimits) {
            // The limit-aware sampler can only infer openedCount safely when each open contributes at most
            // one reward draw. Multi-draw opens fall back to the exact path to keep charging/refunds correct.
            if (compiledSampler.fixedRewardCount == 1 && !compiledSampler.hasRandomRewardCount) {
                return MassAggregationStrategy.LIMIT_AWARE
            }
            return MassAggregationStrategy.EXACT
        }
        val deterministic = compiledSampler.rewards.size == 1 &&
            compiledSampler.fixedRewardCount != null &&
            compiledSampler.fixedRewardAmount(0) != null
        if (deterministic) {
            return MassAggregationStrategy.DETERMINISTIC
        }
        if (
            amount >= MULTINOMIAL_OPEN_THRESHOLD &&
            compiledSampler.fixedRewardCount != null &&
            !compiledSampler.hasRandomRewardAmounts
        ) {
            return MassAggregationStrategy.MULTINOMIAL
        }
        if (amount >= VECTORIZE_OPEN_THRESHOLD) {
            return MassAggregationStrategy.VECTORIZED
        }
        return MassAggregationStrategy.EXACT
    }

    private fun createSamplingSeed(vararg components: Long): Long {
        var seed = ThreadLocalRandom.current().nextLong() xor System.nanoTime()
        components.forEachIndexed { index, component ->
            seed = MassRandom.mixSeed(seed xor component + index.toLong() * SAMPLING_SEED_INDEX_STEP)
        }
        return seed
    }

    private suspend fun selectMassPriceSelection(
        session: OpeningSession,
        requestedAmount: Int,
        ignoreKeyRequirement: Boolean,
    ): MassPriceSelection {
        return if (ignoreKeyRequirement) {
            MassPriceSelection(null, Long.MAX_VALUE)
        } else {
            selectMassPriceGroup(session.player, session.crate.priceGroups, requestedAmount.toLong())
        }
    }

    private suspend fun selectEffectiveMassOpenAmount(
        session: OpeningSession,
        requestedAmount: Int,
        priceSelection: MassPriceSelection,
    ): Int {
        val crateLimitCap = maxOpenCountFromCrateLimits(session)
        return minOf(requestedAmount.toLong(), priceSelection.maxAffordable, crateLimitCap)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private suspend fun takeMassOpeningPrice(
        player: Player,
        selectedPriceGroup: OpenPriceGroup?,
        effectiveAmount: Int,
        ignoreKeyRequirement: Boolean,
    ): Boolean {
        if (ignoreKeyRequirement || selectedPriceGroup == null) {
            return true
        }
        return selectedPriceGroup.tryTake(player, effectiveAmount)
    }

    private suspend fun createMassOpeningContext(session: OpeningSession): MassOpeningContext? {
        val resolvedProvider = resolveWinnableProvider(session) ?: return null
        val rewardProcessor = session.crate.rewardProcessor
        val rewardTracker = MassRewardLimitTracker.create(session.player, session.crate.id, resolvedProvider.rewards)
        val massRewardCountRanges = when (rewardProcessor) {
            is ChooseRewardProcessor -> rewardProcessor.chooseCountRanges
            else -> resolvedProvider.rewardCountRanges
        }
        val compiledSampler = CompiledMassOpeningSampler.compile(
            massRewardCountRanges,
            resolvedProvider.rewards.filter { it.canWinIgnoringLimits(session.player) }
        )
        val chooseNeedsUniqueExact = rewardProcessor is ChooseRewardProcessor &&
            rewardProcessor.uniqueRewards &&
            compiledSampler.maxRewardCount > 1

        return MassOpeningContext(
            compiledSampler = compiledSampler,
            rewardTracker = rewardTracker,
            chooseNeedsUniqueExact = chooseNeedsUniqueExact
        )
    }

    private fun selectMassChunkStrategy(
        context: MassOpeningContext,
        effectiveAmount: Int,
    ): MassAggregationStrategy {
        return if (context.chooseNeedsUniqueExact) {
            MassAggregationStrategy.EXACT
        } else {
            selectMassAggregationStrategy(context.compiledSampler, context.rewardTracker, effectiveAmount)
        }
    }

    private suspend fun aggregateMassChunkRewards(
        effectiveAmount: Int,
        context: MassOpeningContext,
        strategy: MassAggregationStrategy,
    ): MassOpenAggregation {
        return withContext(VirtualsCtx) {
            if (context.chooseNeedsUniqueExact) {
                aggregateMassUniqueChooseRewards(effectiveAmount, context.rewardTracker, context.compiledSampler)
            } else {
                aggregateMassRewards(effectiveAmount, context.rewardTracker, context.compiledSampler, strategy)
            }
        }
    }

    private fun emptyMassChunkResult(): MassOpenChunkResult {
        return MassOpenChunkResult(success = false, aggregation = MassOpenAggregation(0, emptyList()), strategy = null)
    }

    private fun createMassRandom(vararg components: Long): MassRandom {
        return MassRandom(createSamplingSeed(*components))
    }

    private fun Long.saturatedAdd(other: Long): Long {
        return if (Long.MAX_VALUE - this < other) Long.MAX_VALUE else this + other
    }

    private fun weightedRewardIndex(rewards: List<Reward>, random: MassRandom): Int {
        if (rewards.size == 1) {
            return 0
        }

        var totalWeight = 0.0
        for (reward in rewards) {
            totalWeight += reward.chance.coerceAtLeast(0.0)
        }
        if (totalWeight <= 0.0) {
            return random.nextInt(rewards.size)
        }

        var remaining = random.nextDouble() * totalWeight
        for (index in rewards.indices) {
            val chance = rewards[index].chance.coerceAtLeast(0.0)
            if (chance <= 0.0) {
                continue
            }
            remaining -= chance
            if (remaining <= 0.0) {
                return index
            }
        }
        return rewards.lastIndex
    }

    private val MULTINOMIAL_OPEN_THRESHOLD = MassOpeningSupport.MULTINOMIAL_OPEN_THRESHOLD
    private val VECTORIZE_OPEN_THRESHOLD = MassOpeningSupport.VECTORIZE_OPEN_THRESHOLD
    private val SAMPLING_SEED_INDEX_STEP = 0x9E3779B97F4A7C15uL.toLong()
}

private data class MassOpeningContext(
    val compiledSampler: CompiledMassOpeningSampler,
    val rewardTracker: MassRewardLimitTracker,
    val chooseNeedsUniqueExact: Boolean,
)
