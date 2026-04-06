package gg.aquatic.crates.crate.preview

import gg.aquatic.crates.reward.Reward
import gg.aquatic.kmenu.inventory.event.AsyncPacketInventoryInteractEvent
import gg.aquatic.kmenu.menu.Menu
import gg.aquatic.kmenu.menu.MenuComponent
import gg.aquatic.kmenu.menu.util.ListMenu
import org.bukkit.inventory.ItemStack
import kotlin.random.Random

class RollingPreviewEntrySet(
    private val entries: List<ListMenu.Entry<Reward>>,
    private val unique: Boolean,
) {
    private val registeredIds = linkedSetOf<String>()
    private val currentEntries = linkedMapOf<String, ListMenu.Entry<Reward>?>()
    private var uniqueCycle = -1

    fun register(id: String): ListMenu.Entry<Reward>? {
        registeredIds += id
        val entry = if (unique) {
            rerollAll()
            currentEntries[id]
        } else {
            nextEntry(id)
        }
        currentEntries[id] = entry
        return entry
    }

    fun reroll(id: String, cycle: Int): ListMenu.Entry<Reward>? {
        val entry = if (unique) {
            if (uniqueCycle != cycle) {
                uniqueCycle = cycle
                rerollAll()
            }
            currentEntries[id]
        } else {
            nextEntry(id)
        }
        currentEntries[id] = entry
        return entry
    }

    private fun nextEntry(id: String): ListMenu.Entry<Reward>? {
        if (entries.isEmpty()) {
            return null
        }
        if (!unique) {
            return entries.random(Random.Default)
        }

        val used = currentEntries
            .filterKeys { it != id }
            .values
            .filterNotNull()
            .toSet()

        val available = entries.filter { it !in used }
        if (available.isEmpty()) {
            return currentEntries[id]
        }

        val previous = currentEntries[id]
        val different = available.filter { it != previous }
        return if (different.isNotEmpty()) different.random(Random.Default) else available.random(Random.Default)
    }

    private fun rerollAll() {
        if (registeredIds.isEmpty()) {
            return
        }

        val shuffled = entries.shuffled(Random.Default)
        registeredIds.forEachIndexed { index, id ->
            currentEntries[id] = shuffled.getOrNull(index)
        }
    }
}

class RollingPreviewEntryButton(
    override val id: String,
    override val slots: Collection<Int>,
    override val priority: Int,
    private val updateEvery: Int,
    private val entrySet: RollingPreviewEntrySet,
) : MenuComponent() {

    private var tick = 0
    private var cycle = 0
    private var currentEntry: ListMenu.Entry<Reward>? = entrySet.register(id)

    override val onClick: suspend (AsyncPacketInventoryInteractEvent) -> Unit = { }

    override suspend fun itemstack(menu: Menu): ItemStack? {
        return currentEntry?.itemVisual?.invoke()
    }

    override suspend fun tick(menu: Menu) {
        if (tick >= updateEvery) {
            tick = 0
            cycle++
            currentEntry = entrySet.reroll(id, cycle)
            menu.updateComponent(this)
        }

        tick++
    }
}
