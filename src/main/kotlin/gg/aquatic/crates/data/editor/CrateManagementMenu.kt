package gg.aquatic.crates.data.editor

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.data.CrateData
import gg.aquatic.crates.data.CrateStorage
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.kmenu.inventory.InventoryType
import gg.aquatic.kmenu.menu.createMenu
import gg.aquatic.stacked.stackedItem
import gg.aquatic.waves.input.impl.ChatInput
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.entity.Player

object CrateManagementMenu {

    private val entrySlots = (0..44).toList()
    private val idPattern = Regex("[a-zA-Z0-9_-]+")

    suspend fun open(player: Player, page: Int = 0) {
        val crateIds = CrateStorage.availableIds()
        val safePage = page.coerceAtLeast(0)
        val start = safePage * entrySlots.size
        val pageEntries = crateIds.drop(start).take(entrySlots.size)
        val hasPrevious = safePage > 0
        val hasNext = start + entrySlots.size < crateIds.size

        player.createMenu(Component.text("Crates (${crateIds.size})"), InventoryType.Companion.GENERIC9X6) {
            pageEntries.forEachIndexed { index, crateId ->
                button("crate_$crateId", entrySlots[index]) {
                    item = buildCrateIcon(crateId)
                    onClick { event ->
                        when (event.buttonType) {
                            ButtonType.DROP -> {
                                CrateStorage.delete(crateId)
                                VirtualsCtx {
                                    CratesPlugin.reload()
                                }
                                open(player, if (hasNext || pageEntries.size > 1) safePage else (safePage - 1).coerceAtLeast(0))
                            }

                            else -> {
                                openEditor(player, crateId)
                            }
                        }
                    }
                }
            }

            button("create", 49) {
                item = stackedItem(Material.NETHER_STAR) {
                    displayName = title("Create Crate")
                    lore += hint("Create a new default crate")
                }.getItem()
                onClick {
                    createCrate(player, safePage)
                }
            }

            if (hasPrevious) {
                button("previous_page", 45) {
                    item = stackedItem(Material.ARROW) {
                        displayName = title("Previous Page")
                        lore += hint("Open the previous crate page")
                    }.getItem()
                    onClick {
                        open(player, safePage - 1)
                    }
                }
            }

            if (hasNext) {
                button("next_page", 53) {
                    item = stackedItem(Material.ARROW) {
                        displayName = title("Next Page")
                        lore += hint("Open the next crate page")
                    }.getItem()
                    onClick {
                        open(player, safePage + 1)
                    }
                }
            }
        }.open(player)
    }

    private suspend fun createCrate(player: Player, returnPage: Int) {
        withContext(BukkitCtx.ofEntity(player)) {
            player.closeInventory()
        }

        player.sendMessage("Enter new crate ID:")
        val id = ChatInput.createHandle(listOf("cancel")).await(player)?.trim().orEmpty()
        if (id.isEmpty()) {
            open(player, returnPage)
            return
        }

        if (!idPattern.matches(id)) {
            player.sendMessage("Invalid crate ID. Use only letters, numbers, '_' or '-'.")
            open(player, returnPage)
            return
        }

        if (CrateStorage.exists(id)) {
            player.sendMessage("Crate '$id' already exists.")
            open(player, returnPage)
            return
        }

        CrateStorage.save(id, CrateData.createDefault(displayName = "<yellow>$id Crate"))
        VirtualsCtx {
            CratesPlugin.reload()
        }
        openEditor(player, id)
    }

    private suspend fun openEditor(player: Player, crateId: String) {
        withContext(BukkitCtx.ofEntity(player)) {
            player.closeInventory()
        }

        CratesPlugin.server.scheduler.runTask(CratesPlugin, Runnable {
            runCatching {
                CrateEditor.open(player, crateId)
            }.onFailure { throwable ->
                player.sendMessage("Failed to open crate editor: ${throwable.message ?: throwable.javaClass.simpleName}")
                throwable.printStackTrace()
            }
        })
    }

    private fun buildCrateIcon(crateId: String) = CrateHandler.crates[crateId]?.keyItem?.clone()?.apply {
        editMeta { meta ->
            meta.displayName(title(crateId))
            meta.lore(
                listOf(
                    section("Actions"),
                    action("Left click: ", "Open editor", NamedTextColor.GREEN),
                    action("Q: ", "Delete crate", NamedTextColor.RED)
                )
            )
        }
    } ?: stackedItem(Material.CHEST) {
        displayName = title(crateId)
        lore += section("Actions")
        lore += action("Left click: ", "Open editor", NamedTextColor.GREEN)
        lore += action("Q: ", "Delete crate", NamedTextColor.RED)
    }.getItem()

    private fun title(text: String): Component {
        return Component.text(text, NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false)
    }

    private fun section(text: String): Component {
        return Component.text(text, NamedTextColor.DARK_AQUA)
            .decoration(TextDecoration.ITALIC, false)
    }

    private fun hint(text: String): Component {
        return Component.text(text, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
    }

    private fun action(prefix: String, value: String, valueColor: NamedTextColor): Component {
        return Component.text(prefix, NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
            .append(
                Component.text(value, valueColor)
                    .decoration(TextDecoration.ITALIC, false)
            )
    }
}
