package gg.aquatic.crates.data.price

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.crates.crate.CrateHandler
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

object CrateReferenceSelectionMenu {

    private val entrySlots = (0..44).toList()

    sealed interface SelectionResult {
        data object Cancelled : SelectionResult
        data class Selected(val crateId: String?) : SelectionResult
        data object PreviousPage : SelectionResult
        data object NextPage : SelectionResult
    }

    suspend fun select(player: Player, currentValue: String?): SelectionResult {
        return openPage(player, currentValue, 0)
    }

    private suspend fun openPage(player: Player, currentValue: String?, page: Int): SelectionResult {
        val crateIds = CrateHandler.crates.keys.sorted()
        val options = listOf<String?>(null) + crateIds
        val safePage = page.coerceAtLeast(0)
        val start = safePage * entrySlots.size
        val pageEntries = options.drop(start).take(entrySlots.size)
        val hasPrevious = safePage > 0
        val hasNext = start + entrySlots.size < options.size

        return withContext(BukkitCtx.ofEntity(player)) {
            suspendCancellableCoroutine { continuation ->
                KMenu.scope.launch {
                    player.createMenu(Component.text("Select Source Crate"), InventoryType.GENERIC9X6) {
                        pageEntries.forEachIndexed { index, crateId ->
                            button("crate_ref_${crateId ?: "current"}_$index", entrySlots[index]) {
                                item = buildEntry(crateId, currentValue)
                                onClick {
                                    continuation.resumeOnce(SelectionResult.Selected(crateId))
                                }
                            }
                        }

                        if (hasPrevious) {
                            button("previous_page", 45) {
                                item = navItem("Previous Page")
                                onClick {
                                    continuation.resumeOnce(SelectionResult.PreviousPage)
                                }
                            }
                        }

                        button("cancel", 49) {
                            item = stackedItem(Material.BARRIER) {
                                displayName = text("Cancel", NamedTextColor.RED)
                                lore += text("Keep the current value", NamedTextColor.GRAY)
                            }.getItem()
                            onClick {
                                continuation.resumeOnce(SelectionResult.Cancelled)
                            }
                        }

                        if (hasNext) {
                            button("next_page", 53) {
                                item = navItem("Next Page")
                                onClick {
                                    continuation.resumeOnce(SelectionResult.NextPage)
                                }
                            }
                        }
                    }.open(player)
                }
            }
        }.let { result ->
            when (result) {
                SelectionResult.Cancelled -> SelectionResult.Cancelled
                SelectionResult.PreviousPage -> openPage(player, currentValue, safePage - 1)
                SelectionResult.NextPage -> openPage(player, currentValue, safePage + 1)
                is SelectionResult.Selected -> result
            }
        }
    }

    private fun buildEntry(crateId: String?, currentValue: String?) = stackedItem(
        if (crateId == null) Material.TRIPWIRE_HOOK else Material.CHEST
    ) {
        displayName = text(crateId ?: "Current Crate", NamedTextColor.AQUA)
        if (crateId == null) {
            lore += text("Uses the key of the crate", NamedTextColor.GRAY)
            lore += text("that owns this price group.", NamedTextColor.GRAY)
        } else {
            lore += text("Use the key from crate '$crateId'.", NamedTextColor.GRAY)
        }
        if (currentValue == crateId || (currentValue.isNullOrBlank() && crateId == null)) {
            lore += text(" ", NamedTextColor.DARK_GRAY)
            lore += text("Currently selected", NamedTextColor.GREEN)
        }
    }.getItem()

    private fun navItem(label: String) = stackedItem(Material.ARROW) {
        displayName = text(label, NamedTextColor.AQUA)
        lore += text("Open another page of crates", NamedTextColor.GRAY)
    }.getItem()

    private fun <T> Continuation<T>.resumeOnce(value: T) {
        runCatching {
            resume(value)
        }
    }

    private fun text(content: String, color: NamedTextColor): Component {
        return Component.text(content, color)
            .decoration(TextDecoration.ITALIC, false)
    }
}
