package gg.aquatic.crates.command

import gg.aquatic.crates.Messages
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.message.replacePlaceholder
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.ExecutionContext
import gg.aquatic.kommand.playerArgument
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

internal fun CommandBuilder<CommandSourceStack, CommandSender>.crateArgument(
    id: String,
    block: CommandBuilder<CommandSourceStack, CommandSender>.() -> Unit
) {
    mappedWordArgument(
        id = id,
        onInvalid = {
            Messages.CRATE_NOT_FOUND.message()
                .replacePlaceholder("%crate_id%", string(id))
                .send(sender)
        },
        parser = { raw -> CrateHandler.crates[raw] }
    ) {
        suggest({ CrateHandler.crates.keys })
        block()
    }
}

internal fun CommandBuilder<CommandSourceStack, CommandSender>.onlinePlayerArgumentIncludingSelf(
    id: String,
    block: CommandBuilder<CommandSourceStack, CommandSender>.() -> Unit
) {
    playerArgument(
        id,
        true,
        onInvalid = {
            Messages.PLAYER_NOT_FOUND.message()
                .replacePlaceholder("%player%", string(id))
                .send(sender)
        }
    ) {
        block()
    }
}

internal fun CommandBuilder<CommandSourceStack, CommandSender>.onlinePlayerArgument(
    id: String,
    block: CommandBuilder<CommandSourceStack, CommandSender>.() -> Unit
) {
    playerArgument(
        id,
        onInvalid = {
            Messages.PLAYER_NOT_FOUND.message()
                .replacePlaceholder("%player%", string(id))
                .send(sender)
        }
    ) {
        block()
    }
}

internal fun CommandSender.requirePlayerSender(): Player? {
    return this as? Player ?: run {
        Messages.PLAYER_ONLY_COMMAND.message().send(this)
        null
    }
}

internal data class OnlinePlayerArgumentResult(
    val rawName: String?,
    val player: Player?,
) {
    val wasSpecified: Boolean
        get() = rawName != null

    val isInvalid: Boolean
        get() = rawName != null && player == null
}

internal fun ExecutionContext<CommandSourceStack, CommandSender>.onlinePlayerArgumentResult(id: String): OnlinePlayerArgumentResult {
    val rawName = runCatching { string(id) }.getOrNull()
    val player = getOrNull<Player>(id)
    return OnlinePlayerArgumentResult(rawName, player)
}
