package gg.aquatic.crates.command.impl

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import gg.aquatic.kommand.playerArgument
import io.papermc.paper.command.brigadier.CommandSourceStack
import java.math.BigDecimal
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.math.BigInteger

/**
 * aqcrates key give <crate-id> [player] [key]
 */
internal fun CommandBuilder<CommandSourceStack, CommandSender>.keyCommand() =
    "key" {
        hasPermission("aqcrates.admin")

        "give" {
            listArgument("crate", { CrateHandler.crates.values }, { it.id }) {
                playerArgument("player", true) {
                    bigIntegerArgument("amount")
                    flagsArgument("options", listOf("-s", "-v"))
                }
                suspendExecute<CommandSender> {
                    val crate = get<Crate>("crate")
                    val player = getOrNull<Player>("player")
                    val amount = getOrNull<BigInteger>("amount") ?: BigInteger.ONE
                    val silent = hasFlag("options", "-s")
                    val virtual = hasFlag("options", "-v")

                    if (player == null) {
                        if (sender !is Player) {
                            if (!silent) {
                                sender.sendMessage("You must be a player to give yourself keys!")
                            }
                            return@suspendExecute
                        }
                        val self = sender as Player
                        withContext(BukkitCtx.ofEntity(self)) {
                            giveKeys(crate, self, amount, virtual)
                            if (!silent) {
                                self.sendMessage("You have been given ${amount}x ${keyTypeName(virtual)} key!")
                            }
                        }
                        return@suspendExecute
                    }

                    withContext(BukkitCtx.ofEntity(player)) {
                        giveKeys(crate, player, amount, virtual)
                        if (!silent) {
                            player.sendMessage("You have been given ${amount}x ${keyTypeName(virtual)} key!")
                            if (sender != player) {
                                sender.sendMessage("You have given ${player.name} ${amount}x ${keyTypeName(virtual)} key!")
                            }
                        }
                    }
                }
            }
        }
    }

private suspend fun giveKeys(crate: Crate, player: Player, amount: BigInteger, virtual: Boolean) {
    if (virtual) {
        CratesPlugin.crateKeyCurrency(crate.id).give(player, amount.toBigDecimal())
        return
    }

    givePhysicalKeys(crate.keyItem, player, amount)
}

private fun givePhysicalKeys(keyItem: ItemStack, player: Player, amount: BigInteger) {
    val maxStackSize = keyItem.maxStackSize.coerceAtLeast(1)
    var remaining = amount

    while (remaining > BigInteger.ZERO) {
        val stackAmount = remaining.min(maxStackSize.toBigInteger()).toInt()
        player.inventory.addItem(
            keyItem.clone().apply {
                this.amount = stackAmount
            }
        )
        remaining -= stackAmount.toBigInteger()
    }
}

private fun keyTypeName(virtual: Boolean): String = if (virtual) "virtual" else "physical"
