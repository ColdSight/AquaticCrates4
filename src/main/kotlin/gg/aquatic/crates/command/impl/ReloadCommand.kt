package gg.aquatic.crates.command.impl

import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.Messages
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender

internal fun CommandBuilder<CommandSourceStack, CommandSender>.reloadCommand() =
    "reload" {
        hasPermission("aqcrates.admin")

        suspendExecute<CommandSender> {
            Messages.PLUGIN_RELOADING.message().send(sender)
            CratesPlugin.reload()
            Messages.PLUGIN_RELOADED.message().send(sender)
        }
    }
