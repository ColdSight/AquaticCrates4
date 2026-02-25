package gg.aquatic.crates.command.impl

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.kommand.CommandBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player

/**
 * aqcrates crate <give> <crate-id>
 */
internal fun CommandBuilder<CommandSourceStack>.crateCommand() =
    "crate" {
        requires {
            it.sender.hasPermission("aqcrates.admin")
        }

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