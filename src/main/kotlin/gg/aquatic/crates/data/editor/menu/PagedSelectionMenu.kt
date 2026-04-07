package gg.aquatic.crates.data.editor.menu

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.stacked.stackedItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object PagedSelectionMenu {
    private val defaultEntrySlots = (0..44).toList()

    sealed interface Result<out T> {
        data object Cancelled : Result<Nothing>
        data class Selected<T>(val value: T) : Result<T>
        data object PreviousPage : Result<Nothing>
        data object NextPage : Result<Nothing>
    }

    suspend fun <T> select(
        player: Player,
        title: String,
        options: List<T>,
        currentValue: T? = null,
        inventoryType: InventoryType = InventoryType.GENERIC9X6,
        entrySlots: List<Int> = defaultEntrySlots,
        cancelSlot: Int = 49,
        previousSlot: Int = 45,
        nextSlot: Int = 53,
        buildEntry: (T, T?) -> org.bukkit.inventory.ItemStack,
        cancelDescription: String = "Keep the current value",
        navigationDescription: String = "Open another page",
    ): Result<T> {
        return openPage(
            player = player,
            title = title,
            options = options,
            currentValue = currentValue,
            inventoryType = inventoryType,
            entrySlots = entrySlots,
            cancelSlot = cancelSlot,
            previousSlot = previousSlot,
            nextSlot = nextSlot,
            page = 0,
            buildEntry = buildEntry,
            cancelDescription = cancelDescription,
            navigationDescription = navigationDescription
        )
    }

    private suspend fun <T> openPage(
        player: Player,
        title: String,
        options: List<T>,
        currentValue: T?,
        inventoryType: InventoryType,
        entrySlots: List<Int>,
        cancelSlot: Int,
        previousSlot: Int,
        nextSlot: Int,
        page: Int,
        buildEntry: (T, T?) -> org.bukkit.inventory.ItemStack,
        cancelDescription: String,
        navigationDescription: String,
    ): Result<T> {
        val safePage = page.coerceAtLeast(0)
        val start = safePage * entrySlots.size
        val pageEntries = options.drop(start).take(entrySlots.size)
        val hasPrevious = safePage > 0
        val hasNext = start + entrySlots.size < options.size

        return withContext(BukkitCtx.ofEntity(player)) {
            suspendCancellableCoroutine { continuation ->
                KMenu.scope.launch {
                    player.createMenu(Component.text(title), inventoryType) {
                        pageEntries.forEachIndexed { index, option ->
                            button("entry_${index}", entrySlots[index]) {
                                item = buildEntry(option, currentValue)
                                onClick {
                                    continuation.resumeOnce(Result.Selected(option))
                                }
                            }
                        }

                        if (hasPrevious) {
                            button("previous_page", previousSlot) {
                                item = navItem("Previous Page", navigationDescription)
                                onClick { continuation.resumeOnce(Result.PreviousPage) }
                            }
                        }

                        button("cancel", cancelSlot) {
                            item = stackedItem(Material.BARRIER) {
                                displayName = text("Cancel", NamedTextColor.RED)
                                lore += text(cancelDescription, NamedTextColor.GRAY)
                            }.getItem()
                            onClick { continuation.resumeOnce(Result.Cancelled) }
                        }

                        if (hasNext) {
                            button("next_page", nextSlot) {
                                item = navItem("Next Page", navigationDescription)
                                onClick { continuation.resumeOnce(Result.NextPage) }
                            }
                        }
                    }.open(player)
                }
            }
        }.let { result ->
            when (result) {
                Result.Cancelled -> Result.Cancelled
                Result.PreviousPage -> openPage(
                    player, title, options, currentValue, inventoryType, entrySlots,
                    cancelSlot, previousSlot, nextSlot, safePage - 1, buildEntry,
                    cancelDescription, navigationDescription
                )
                Result.NextPage -> openPage(
                    player, title, options, currentValue, inventoryType, entrySlots,
                    cancelSlot, previousSlot, nextSlot, safePage + 1, buildEntry,
                    cancelDescription, navigationDescription
                )
                is Result.Selected -> result
            }
        }
    }

    private fun navItem(label: String, description: String) = stackedItem(Material.ARROW) {
        displayName = text(label, NamedTextColor.AQUA)
        lore += text(description, NamedTextColor.GRAY)
    }.getItem()

    private fun text(content: String, color: NamedTextColor): Component {
        return Component.text(content, color)
            .decoration(TextDecoration.ITALIC, false)
    }

    private fun <T> Continuation<T>.resumeOnce(value: T) {
        runCatching { resume(value) }
    }
}
