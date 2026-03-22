package gg.aquatic.crates.command.impl

import gg.aquatic.crates.CratesPlugin
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender

internal fun CommandBuilder<CommandSourceStack, CommandSender>.reloadCommand() =
    "reload" {
        hasPermission("aqcrates.admin")

        suspendExecute<CommandSender> {
            sender.sendMessage("Reloading...")
            CratesPlugin.reload()
            sender.sendMessage("Reloaded!")
        }
    }
