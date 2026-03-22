package gg.aquatic.crates.command.impl

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.data.CrateStorage
import gg.aquatic.crates.data.editor.CrateEditor
import gg.aquatic.crates.data.editor.CrateManagementMenu
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * aqcrates crate <give> <crate-id>
 */
internal fun CommandBuilder<CommandSourceStack, CommandSender>.crateCommand() =
    "crate" {
        hasPermission("aqcrates.admin")

        "give" {
            listArgument("crate", { CrateHandler.crates.values }, { it.id }) {
                suspendExecute<Player> {
                    withContext(BukkitCtx.ofEntity(sender)) {
                        val crate = get<Crate>("crate")
                        sender.sendMessage("You have been given the crate!")
                        sender.inventory.addItem(crate.crateItemStack)
                    }
                }
            }
        }

        "edit" {
            listArgument("crate-id", { CrateStorage.availableIds() }, { it }) {
                suspendExecute<Player> {
                    withContext(BukkitCtx.ofEntity(sender)) {
                        val crateId = get<String>("crate-id")
                        CrateEditor.open(sender, crateId)
                    }
                }
            }
        }

        "menu" {
            suspendExecute<Player> {
                withContext(BukkitCtx.ofEntity(sender)) {
                    CrateManagementMenu.open(sender)
                }
            }
        }
    }
