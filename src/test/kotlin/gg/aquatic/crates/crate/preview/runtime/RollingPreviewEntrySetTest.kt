package gg.aquatic.crates.crate.preview.runtime

import gg.aquatic.crates.reward.Reward
import gg.aquatic.kmenu.menu.util.ListMenu
import io.mockk.mockk
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RollingPreviewEntrySetTest {
    @Test
    fun `non-unique register and reroll returns entries from source list`() {
        val entries = entries("a", "b", "c")
        val set = RollingPreviewEntrySet(entries, unique = false)

        val first = set.register("slot1")
        val second = set.reroll("slot1", cycle = 1)

        assertTrue(first in entries)
        assertTrue(second in entries)
    }

    @Test
    fun `unique register assigns different entries to different slots when enough rewards exist`() {
        val entries = entries("a", "b", "c")
        val set = RollingPreviewEntrySet(entries, unique = true)

        set.register("slot1")
        set.register("slot2")
        set.register("slot3")
        val first = set.reroll("slot1", cycle = 1)
        val second = set.reroll("slot2", cycle = 1)
        val third = set.reroll("slot3", cycle = 1)

        assertEquals(3, setOf(first, second, third).size)
    }

    @Test
    fun `unique register leaves extra slots empty when there are not enough rewards`() {
        val entries = entries("a", "b")
        val set = RollingPreviewEntrySet(entries, unique = true)

        val first = set.register("slot1")
        val second = set.register("slot2")
        val third = set.register("slot3")

        assertTrue(first in entries)
        assertTrue(second in entries)
        assertNull(third)
    }

    @Test
    fun `unique reroll is stable inside one cycle and may change on next cycle`() {
        val entries = entries("a", "b", "c")
        val set = RollingPreviewEntrySet(entries, unique = true)

        set.register("slot1")
        val cycleOneA = set.reroll("slot1", cycle = 1)
        val cycleOneB = set.reroll("slot1", cycle = 1)

        assertEquals(cycleOneA, cycleOneB)

        val cycleTwo = set.reroll("slot1", cycle = 2)
        assertTrue(cycleTwo in entries)
    }

    private fun entries(vararg ids: String): List<ListMenu.Entry<Reward>> {
        return ids.map { id ->
            ListMenu.Entry(
                value = mockk<Reward>(name = id),
                itemVisual = { ItemStack(Material.STONE) },
                placeholderContext = mockk(relaxed = true),
                onClick = {}
            )
        }
    }
}
