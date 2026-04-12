package gg.aquatic.crates.reward

import gg.aquatic.crates.limit.LimitHandle
import gg.aquatic.execute.ActionHandle
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class RewardPlaceholderTest {
    @Test
    fun `normal reward placeholders include player and random amount`() {
        val reward = reward(id = "coins")
        val player = mockPlayer("Larkyy")

        val updated = reward.updatePlaceholders(
            str = "%reward-id% %random-amount% %reward-total-amount% %reward-chance% %reward-chance-formatted%",
            player = player,
            randomAmount = 7
        )

        assertEquals("coins 7 7 1 100", updated)
    }

    @Test
    fun `mass reward placeholders include aliases and player`() {
        val reward = reward(id = "gems")
        val player = mockPlayer("Tester")

        val updated = reward.updateMassPlaceholders(
            str = "%reward-win-count% %reward-drawn-count% %reward-total-amount% %reward-total-random-amount%",
            player = player,
            winCount = 12L,
            totalAmount = 48L
        )

        assertEquals("12 12 48 48", updated)
    }

    @Test
    fun `chance placeholders expose raw and formatted percentage values`() {
        val reward = reward(id = "legendary", chance = 0.1)
        val player = mockPlayer("Tester")

        val updated = reward.updateMassPlaceholders(
            str = "%reward-chance% %reward-chance-formatted% %reward-real-chance% %reward-real-chance-formatted%",
            player = player,
            winCount = 1L,
            totalAmount = 1L
        )

        assertEquals("0.1 10 10 10", updated)
    }

    private fun mockPlayer(name: String): Player = mockk {
        every { this@mockk.name } returns name
    }

    private fun reward(id: String, chance: Double = 1.0): Reward {
        return Reward(
            id = id,
            crateId = "test",
            displayName = Component.text(id),
            previewItem = { ItemStack(Material.STONE) },
            fallbackItem = null,
            winActions = emptyList(),
            massWinActions = emptyList(),
            conditions = emptyList(),
            purchaseManager = null,
            amountRanges = emptyList(),
            clickHandler = { _, _, _ -> },
            rarity = RewardRarity(id = "common", displayName = Component.text("Common"), chance = 1.0),
            limits = emptyList(),
            chance = chance
        )
    }
}
