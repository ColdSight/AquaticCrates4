package gg.aquatic.crates.command.impl

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.Messages
import gg.aquatic.crates.command.crateArgument
import gg.aquatic.crates.command.onlinePlayerArgument
import gg.aquatic.crates.command.onlinePlayerArgumentIncludingSelf
import gg.aquatic.crates.command.onlinePlayerArgumentResult
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.message.storage.MessageStorage
import gg.aquatic.crates.message.replacePlaceholder
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import gg.aquatic.klocale.impl.paper.replacePlaceholders
import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.withContext
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal
import java.math.BigInteger

/**
 * aqcrates key give <crate-id> [player] [key]
 */
internal fun CommandBuilder<CommandSourceStack, CommandSender>.keyCommand() =
    "key" {
        hasPermission("aqcrates.admin")

        "give" {
            crateArgument("crate") {
                onlinePlayerArgumentIncludingSelf("player") {
                    bigIntegerArgument("amount", min = BigInteger.ONE) {
                        flagsArgument("options", listOf("-s", "-v"))
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

                    val player = playerArgument.player
                    val amount = getOrNull<BigInteger>("amount") ?: BigInteger.ONE
                    val silent = hasFlag("options", "-s")
                    val virtual = hasFlag("options", "-v")

                    if (player == null) {
                        if (sender !is Player) {
                            if (!silent) {
                                Messages.KEYS_SELF_REQUIRES_PLAYER.message().send(sender)
                            }
                            return@suspendExecute
                        }
                        val self = sender as Player
                        withContext(BukkitCtx.ofEntity(self)) {
                            giveKeys(crate, self, amount, virtual)
                            if (!silent) {
                                Messages.KEYS_GIVEN_SELF.message()
                                    .replacePlaceholder("%amount%", amount.toString())
                                    .replacePlaceholder("%key_type%", keyTypeName(virtual))
                                    .send(self)
                            }
                        }
                        return@suspendExecute
                    }

                    withContext(BukkitCtx.ofEntity(player)) {
                        giveKeys(crate, player, amount, virtual)
                        if (!silent) {
                            Messages.KEYS_GIVEN_TARGET.message()
                                .replacePlaceholder("%amount%", amount.toString())
                                .replacePlaceholder("%key_type%", keyTypeName(virtual))
                                .send(player)
                            if (sender != player) {
                                Messages.KEYS_GIVEN_SENDER.message()
                                    .replacePlaceholder("%player%", player.name)
                                    .replacePlaceholder("%amount%", amount.toString())
                                    .replacePlaceholder("%key_type%", keyTypeName(virtual))
                                    .send(sender)
                            }
                        }
                    }
                }
            }
        }

        "bank" {
            onlinePlayerArgument("player") {}

            suspendExecute<CommandSender> {
                val playerArgument = onlinePlayerArgumentResult("player")
                if (playerArgument.isInvalid) {
                    Messages.PLAYER_NOT_FOUND.message()
                        .replacePlaceholder("%player%", playerArgument.rawName ?: "unknown")
                        .send(sender)
                    return@suspendExecute
                }

                val target = playerArgument.player ?: run {
                    if (sender !is Player) {
                        Messages.KEYS_SELF_REQUIRES_PLAYER.message().send(sender)
                        return@suspendExecute
                    }
                    sender as Player
                }

                if (target != sender && !sender.hasPermission("aqcrates.admin.keybank.others")) {
                    Messages.NO_PERMISSION.message()
                        .replacePlaceholder("%permission%", "aqcrates.admin.keybank.others")
                        .send(sender)
                    return@suspendExecute
                }

                sendKeyBank(sender, target)
            }
        }
    }

private suspend fun giveKeys(crate: Crate, player: Player, amount: BigInteger, virtual: Boolean) {
    if (virtual) {
        crate.keyCurrency.give(player, amount.toBigDecimal())
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

private suspend fun sendKeyBank(sender: CommandSender, target: Player) {
    val data = MessageStorage.loadData()
    val entries = CrateHandler.crates.values
        .sortedBy { it.id }
        .mapNotNull { crate ->
            val balance = crate.keyVirtualCurrency.getBalance(target)
            if (balance <= BigDecimal.ZERO) return@mapNotNull null
            KeyBankEntry(
                crateId = crate.id,
                crateName = PlainTextComponentSerializer.plainText().serialize(crate.displayName),
                amount = balance.stripTrailingZeros().toPlainString()
            )
        }

    if (entries.isEmpty()) {
        Messages.KEY_BANK_EMPTY.message()
            .replacePlaceholder("%player%", target.name)
            .send(sender)
        return
    }

    val renderedLines = entries.flatMap { entry ->
        data.keyBank.lines.map { line ->
            line.toMiniMessage().toMMComponent().replacePlaceholders(
                mapOf(
                    "player" to target.name,
                    "crate_id" to entry.crateId,
                    "crate_name" to entry.crateName,
                    "amount" to entry.amount,
                )
            )
        }
    }

    data.keyBank.toPaperMessage(
        renderedLines,
        paginationReplacements = mapOf("player" to target.name)
    ).send(sender)
}

private data class KeyBankEntry(
    val crateId: String,
    val crateName: String,
    val amount: String,
)
