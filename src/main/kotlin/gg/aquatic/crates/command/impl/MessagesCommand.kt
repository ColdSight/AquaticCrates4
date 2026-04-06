package gg.aquatic.crates.command.impl

import gg.aquatic.crates.message.MessagesEditor
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

internal fun CommandBuilder<CommandSourceStack, CommandSender>.messagesCommand() =
    "messages" {
        hasPermission("aqcrates.admin")

        suspendExecute<CommandSender> {
            val player = sender as? Player ?: return@suspendExecute
            MessagesEditor.open(player)
        }
    }
