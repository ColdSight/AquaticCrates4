package gg.aquatic.crates.command.impl

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import gg.aquatic.kommand.playerArgument
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * aqcrates key give <crate-id> [player] [key]
 */
internal fun CommandBuilder<CommandSourceStack, CommandSender>.keyCommand() =
    "key" {
        hasPermission("aqcrates.admin")

        "give" {
            listArgument("crate", { CrateHandler.crates.values }, { it.id }) {
                playerArgument("player", true) {
                    intArgument("amount")
                }
                suspendExecute<CommandSender> {
                    val crate = get<Crate>("crate")
                    val player = getOrNull<Player>("player")
                    val amount = getOrNull<Int>("amount") ?: 1

                    if (player == null) {
                        if (sender !is Player) {
                            sender.sendMessage("You must be a player to give yourself keys!")
                            return@suspendExecute
                        }
                        withContext(BukkitCtx.ofEntity(sender as Player)) {
                            (sender as Player).inventory.addItem(crate.keyItem)
                            sender.sendMessage("You have been given a key!")
                        }
                        return@suspendExecute
                    }

                    withContext(BukkitCtx.ofEntity(player)) {
                        player.inventory.addItem(crate.keyItem.asQuantity(amount))
                        player.sendMessage("You have been given ${amount}x key!")
                    }
                }
            }
        }
    }
