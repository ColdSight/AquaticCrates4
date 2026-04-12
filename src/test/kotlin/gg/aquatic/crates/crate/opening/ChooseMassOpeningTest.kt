package gg.aquatic.crates.crate.opening

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.common.argument.ObjectArgument
import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.data.interaction.CrateClickMappingData
import gg.aquatic.crates.limit.LimitHandle
import gg.aquatic.crates.milestone.CrateMilestoneManager
import gg.aquatic.crates.open.OpenConditions
import gg.aquatic.crates.reward.Reward
import gg.aquatic.crates.reward.RewardAmountRange
import gg.aquatic.crates.reward.RewardRarity
import gg.aquatic.crates.reward.processor.ChooseRewardProcessor
import gg.aquatic.crates.reward.provider.ResolvedRewardProvider
import gg.aquatic.crates.reward.provider.RewardProvider
import gg.aquatic.crates.stats.CrateStats
import gg.aquatic.execute.Action
import gg.aquatic.execute.ActionHandle
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class ChooseMassOpeningTest {
    @AfterTest
    fun shutdownStats() {
        CrateStats.shutdown()
    }

    @Test
    fun `mass opening with choose processor does not open selection menu`() = runBlocking {
        val player = mockk<Player> {
            every { uniqueId } returns java.util.UUID.randomUUID()
            every { name } returns "Tester"
        }
        val rewards = listOf(
            reward("one", 50.0),
            reward("two", 30.0),
            reward("three", 20.0),
        )
        val crate = crate(
            rewards = rewards,
            processor = ChooseRewardProcessor(
                chooseCountRanges = listOf(RewardAmountRange(min = 1, max = 2, chance = 1.0)),
                uniqueRewards = true,
                hiddenRewards = false,
                onSelectActions = emptyList(),
                hiddenItem = null,
                menu = mockk(relaxed = true)
            )
        )

        val success = CrateOpeningService.tryOpen(player, crate, amount = BigInteger.valueOf(100))
        assertTrue(success)
    }

    @Test
    fun `big integer chunked mass opening executes mass actions only once per reward`() = runBlocking {
        val player = mockk<Player> {
            every { uniqueId } returns java.util.UUID.randomUUID()
            every { name } returns "Tester"
        }
        val executions = AtomicInteger(0)
        val crate = crate(
            rewards = listOf(reward("only", 100.0, executions)),
            processor = ChooseRewardProcessor(
                chooseCountRanges = listOf(RewardAmountRange(min = 1, max = 1, chance = 1.0)),
                uniqueRewards = true,
                hiddenRewards = false,
                onSelectActions = emptyList(),
                hiddenItem = null,
                menu = mockk(relaxed = true)
            )
        )

        val success = CrateOpeningService.tryOpen(
            player,
            crate,
            amount = BigInteger.valueOf(Int.MAX_VALUE.toLong()).add(BigInteger.ONE)
        )

        assertTrue(success)
        assertEquals(1, executions.get())
    }

    private fun crate(
        rewards: List<Reward>,
        processor: ChooseRewardProcessor,
    ): Crate {
        val provider = object : RewardProvider {
            override fun allRewards(): Collection<Reward> = rewards

            override suspend fun resolve(player: Player): ResolvedRewardProvider {
                return ResolvedRewardProvider(
                    rewards = rewards,
                    rewardCountRanges = listOf(RewardAmountRange(min = 2, max = 3, chance = 1.0))
                )
            }
        }

        return Crate(
            id = "choose-test",
            keyItemSupplier = { mockk(relaxed = true) },
            keyMustBeHeld = false,
            crateClickMapping = CrateClickMappingData(),
            keyClickMapping = CrateClickMappingData(),
            displayName = Component.text("choose-test"),
            hologramSupplier = { null },
            hologramYOffset = 0.0,
            priceGroupsSupplier = { emptyList() },
            openConditionsSupplier = { OpenConditions.DUMMY },
            interactables = emptyList(),
            disableOpenStats = true,
            limits = emptyList(),
            milestoneManagerSupplier = { CrateMilestoneManager(emptyList(), emptyList()) },
            rewardProviderSupplier = { provider },
            rewardProcessorSupplier = { processor },
            previewSupplier = { null },
        )
    }

    private fun reward(id: String, chance: Double, executionCounter: AtomicInteger? = null): Reward {
        val noOpAction = ActionHandle(
            if (executionCounter == null) NoOpPlayerAction else CountingPlayerAction(executionCounter),
            ObjectArguments(emptyMap())
        )
        return Reward(
            id = id,
            crateId = "choose-test",
            displayName = Component.text(id),
            previewItem = { ItemStack(Material.STONE) },
            fallbackItem = null,
            winActions = emptyList(),
            massWinActions = listOf(noOpAction),
            conditions = emptyList(),
            purchaseManager = null,
            amountRanges = listOf(RewardAmountRange(min = 1, max = 2, chance = 1.0)),
            clickHandler = { _, _, _ -> },
            rarity = RewardRarity(id = "common", displayName = Component.text("Common"), chance = 1.0),
            limits = emptyList<LimitHandle>(),
            chance = chance
        )
    }

    private object NoOpPlayerAction : Action<Player> {
        override val binder: Class<out Player> = Player::class.java
        override val arguments: List<ObjectArgument<*>> = emptyList()

        override suspend fun execute(binder: Player, args: ArgumentContext<Player>) {
        }
    }

    private class CountingPlayerAction(
        private val counter: AtomicInteger,
    ) : Action<Player> {
        override val binder: Class<out Player> = Player::class.java
        override val arguments: List<ObjectArgument<*>> = emptyList()

        override suspend fun execute(binder: Player, args: ArgumentContext<Player>) {
            counter.incrementAndGet()
        }
    }
}
