package gg.aquatic.crates.data.editor

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

object InventoryTypeSelectionMenu {

    private val entrySlots = (0..44).toList()
    private val options = listOf(
        "GENERIC9X1",
        "GENERIC9X2",
        "GENERIC9X3",
        "GENERIC9X4",
        "GENERIC9X5",
        "GENERIC9X6",
        "GENERIC3X3",
        "CRAFTER3X3",
        "BEACON",
        "BLAST_FURNACE",
        "BREWING_STAND",
        "CRAFTING_TABLE",
        "ENCHANTMENT_TABLE",
        "FURNACE",
        "GRINDSTONE",
        "HOPPER",
        "LECTERN",
        "LOOM",
        "VILLAGER",
        "SHULKER_BOX",
        "SMITHING_TABLE",
        "SMOKER",
        "CARTOGRAPHY_TABLE",
        "STONECUTTER",
        "ANVIL"
    )

    sealed interface SelectionResult {
        data object Cancelled : SelectionResult
        data class Selected(val inventoryType: String) : SelectionResult
        data object PreviousPage : SelectionResult
        data object NextPage : SelectionResult
    }

    suspend fun select(player: Player, currentValue: String?): SelectionResult {
        return openPage(player, currentValue, 0)
    }

    private suspend fun openPage(player: Player, currentValue: String?, page: Int): SelectionResult {
        val safePage = page.coerceAtLeast(0)
        val start = safePage * entrySlots.size
        val pageEntries = options.drop(start).take(entrySlots.size)
        val hasPrevious = safePage > 0
        val hasNext = start + entrySlots.size < options.size

        return withContext(BukkitCtx.ofEntity(player)) {
            suspendCancellableCoroutine { continuation ->
                KMenu.scope.launch {
                    player.createMenu(Component.text("Select Inventory Type"), InventoryType.GENERIC9X6) {
                        pageEntries.forEachIndexed { index, inventoryTypeName ->
                            button("inventory_type_${inventoryTypeName}_$index", entrySlots[index]) {
                                item = buildEntry(inventoryTypeName, currentValue)
                                onClick {
                                    continuation.resumeOnce(SelectionResult.Selected(inventoryTypeName))
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
                                lore += text("Keep the current UI type", NamedTextColor.GRAY)
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

    private fun buildEntry(inventoryTypeName: String, currentValue: String?) = stackedItem(Material.CHEST) {
        displayName = text(inventoryTypeName, NamedTextColor.AQUA)
        lore += text("Use this inventory layout", NamedTextColor.GRAY)
        lore += text("for the preview menu UI.", NamedTextColor.GRAY)
        if (currentValue == inventoryTypeName) {
            lore += text(" ", NamedTextColor.DARK_GRAY)
            lore += text("Currently selected", NamedTextColor.GREEN)
        }
    }.getItem()

    private fun navItem(label: String) = stackedItem(Material.ARROW) {
        displayName = text(label, NamedTextColor.AQUA)
        lore += text("Open another page of UI types", NamedTextColor.GRAY)
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
