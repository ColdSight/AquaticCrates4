package gg.aquatic.crates.data.editor

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.kmenu.KMenu
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

object RewardRaritySelectionMenu {

    private val entrySlots = (0..44).toList()

    sealed interface SelectionResult {
        data object Cancelled : SelectionResult
        data class Selected(val rarityId: String) : SelectionResult
        data object PreviousPage : SelectionResult
        data object NextPage : SelectionResult
    }

    private data class RarityOption(
        val id: String,
        val displayName: String,
        val chance: String?,
    )

    suspend fun select(player: Player, context: EditorFieldContext, currentValue: String?): SelectionResult {
        return openPage(player, parseRarities(context), currentValue, 0)
    }

    private suspend fun openPage(
        player: Player,
        rarities: List<RarityOption>,
        currentValue: String?,
        page: Int,
    ): SelectionResult {
        val safePage = page.coerceAtLeast(0)
        val start = safePage * entrySlots.size
        val pageEntries = rarities.drop(start).take(entrySlots.size)
        val hasPrevious = safePage > 0
        val hasNext = start + entrySlots.size < rarities.size

        return withContext(BukkitCtx.ofEntity(player)) {
            suspendCancellableCoroutine { continuation ->
                KMenu.scope.launch {
                    player.createMenu(Component.text("Select Reward Rarity"), InventoryType.GENERIC9X6) {
                        pageEntries.forEachIndexed { index, rarity ->
                            button("rarity_${rarity.id}_$index", entrySlots[index]) {
                                item = buildEntry(rarity, currentValue)
                                onClick {
                                    continuation.resumeOnce(SelectionResult.Selected(rarity.id))
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
                                lore += text("Keep the current rarity", NamedTextColor.GRAY)
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
                SelectionResult.PreviousPage -> openPage(player, rarities, currentValue, safePage - 1)
                SelectionResult.NextPage -> openPage(player, rarities, currentValue, safePage + 1)
                is SelectionResult.Selected -> result
            }
        }
    }

    private fun parseRarities(context: EditorFieldContext): List<RarityOption> {
        val root = context.root as? JsonObject ?: return emptyList()
        val rarityRoot = root["rarities"] as? JsonObject ?: return emptyList()
        return rarityRoot.entries.sortedBy { it.key }.map { (id, node) ->
            val objectNode = node as? JsonObject
            val displayName = (objectNode?.get("displayName") as? JsonPrimitive)?.contentOrNull
                ?: (objectNode?.get("display-name") as? JsonPrimitive)?.contentOrNull
                ?: id
            val chance = (objectNode?.get("chance") as? JsonPrimitive)?.contentOrNull
            RarityOption(id = id, displayName = displayName, chance = chance)
        }
    }

    private fun buildEntry(rarity: RarityOption, currentValue: String?) = stackedItem(Material.NETHER_STAR) {
        displayName = text(rarity.id, NamedTextColor.AQUA)
        lore += text("Display: ${rarity.displayName}", NamedTextColor.GRAY)
        rarity.chance?.let { lore += text("Weight: $it", NamedTextColor.GRAY) }
        if (currentValue == rarity.id) {
            lore += text(" ", NamedTextColor.DARK_GRAY)
            lore += text("Currently selected", NamedTextColor.GREEN)
        }
    }.getItem()

    private fun navItem(label: String) = stackedItem(Material.ARROW) {
        displayName = text(label, NamedTextColor.AQUA)
        lore += text("Open another page of rarities", NamedTextColor.GRAY)
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
