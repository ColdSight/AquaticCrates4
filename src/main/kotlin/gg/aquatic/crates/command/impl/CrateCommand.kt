package gg.aquatic.crates.command.impl

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.crates.Messages
import gg.aquatic.crates.command.crateArgument
import gg.aquatic.crates.command.onlinePlayerArgumentIncludingSelf
import gg.aquatic.crates.command.onlinePlayerArgumentResult
import gg.aquatic.crates.command.requirePlayerSender
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.data.CrateStorage
import gg.aquatic.crates.data.editor.CrateEditor
import gg.aquatic.crates.data.editor.CrateManagementMenu
import gg.aquatic.crates.message.replacePlaceholder
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.withContext
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.math.BigInteger

/**
 * aqcrates crate <give> <crate-id>
 */
internal fun CommandBuilder<CommandSourceStack, CommandSender>.crateCommand() =
    "crate" {
        hasPermission("aqcrates.admin")

        "give" {
            crateArgument("crate") {
                suspendExecute<CommandSender> {
                    val player = sender.requirePlayerSender() ?: return@suspendExecute

                    withContext(BukkitCtx.ofEntity(player)) {
                        val crate = get<Crate>("crate")
                        Messages.CRATE_GIVEN.message().send(player)
                        player.inventory.addItem(crate.crateItemStack)
                    }
                }
            }
        }

        "open" {
            crateArgument("crate") {
                onlinePlayerArgumentIncludingSelf("player") {
                    bigIntegerArgument("amount", min = BigInteger.ONE) {
                        flagsArgument("options", listOf("-nokey"))
                    }
                }

                suspendExecute<CommandSender> {
                    val crate = get<Crate>("crate")
                    val playerArgument = onlinePlayerArgumentResult("player")
                    if (playerArgument.isInvalid) {
                        Messages.PLAYER_NOT_FOUND.message()
                            .replacePlaceholder("%player%", playerArgument.rawName ?: "unknown")
                            .send(sender)
                        return@suspendExecute
                    }

                    val target = playerArgument.player ?: run {
                        if (sender !is Player) {
                            Messages.CRATE_OPEN_SELF_REQUIRES_PLAYER.message().send(sender)
                            return@suspendExecute
                        }
                        sender as Player
                    }
                    val amount = (getOrNull<BigInteger>("amount") ?: BigInteger.ONE).max(BigInteger.ONE)
                    val noKey = hasFlag("options", "-nokey")

                    withContext(BukkitCtx.ofEntity(target)) {
                        if (!noKey && crate.keyMustBeHeld && !crate.isHoldingKey(target)) {
                            Messages.CRATE_OPEN_KEY_REQUIRED.message()
                                .replacePlaceholder("%crate_id%", crate.id)
                                .send(sender)
                            return@withContext
                        }

                        val openResult = crate.openResult(target, amount, ignoreKeyRequirement = noKey)
                        if (!openResult.success) {
                            Messages.CRATE_OPEN_FAILED.message()
                                .replacePlaceholder("%crate_id%", crate.id)
                                .replacePlaceholder("%amount%", amount.toString())
                                .send(sender)
                            return@withContext
                        }

                        val openedAmount = openResult.openedCount.max(BigInteger.ONE)

                        if (sender == target) {
                            Messages.CRATE_OPENED_SELF.message()
                                .replacePlaceholder("%crate_id%", crate.id)
                                .replacePlaceholder("%amount%", openedAmount.toString())
                                .send(target)
                            return@withContext
                        }

                        Messages.CRATE_OPENED_TARGET.message()
                            .replacePlaceholder("%crate_id%", crate.id)
                            .replacePlaceholder("%amount%", openedAmount.toString())
                            .send(target)
                        Messages.CRATE_OPENED_SENDER.message()
                            .replacePlaceholder("%player%", target.name)
                            .replacePlaceholder("%crate_id%", crate.id)
                            .replacePlaceholder("%amount%", openedAmount.toString())
                            .send(sender)
                    }
                }
            }
        }

        "edit" {
            mappedWordArgument(
                "crate-id",
                onInvalid = {
                    Messages.CRATE_NOT_FOUND.message()
                        .replacePlaceholder("%crate_id%", string("crate-id"))
                        .send(sender)
                },
                parser = { raw -> CrateStorage.availableIds().find { it == raw } }
            ) {
                suggest({ CrateStorage.availableIds() })
                suspendExecute<CommandSender> {
                    val player = sender.requirePlayerSender() ?: return@suspendExecute

                    withContext(BukkitCtx.ofEntity(player)) {
                        val crateId = get<String>("crate-id")
                        CrateEditor.open(player, crateId)
                    }
                }
            }
        }

        "menu" {
            suspendExecute<CommandSender> {
                val player = sender.requirePlayerSender() ?: return@suspendExecute

                withContext(BukkitCtx.ofEntity(player)) {
                    CrateManagementMenu.open(player)
                }
            }
        }
    }
