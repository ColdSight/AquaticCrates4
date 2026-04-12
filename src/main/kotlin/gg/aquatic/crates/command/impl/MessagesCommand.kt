package gg.aquatic.crates.command.impl

import gg.aquatic.crates.command.requirePlayerSender
import gg.aquatic.crates.message.editor.MessagesEditor
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

internal fun CommandBuilder<CommandSourceStack, CommandSender>.messagesCommand() =
    "messages" {
        hasPermission("aqcrates.admin")

        suspendExecute<CommandSender> {
            val player = sender.requirePlayerSender() ?: return@suspendExecute
            MessagesEditor.open(player)
        }
    }
