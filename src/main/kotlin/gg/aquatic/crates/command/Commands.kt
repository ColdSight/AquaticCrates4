package gg.aquatic.crates.command

import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.command.impl.crateCommand
import gg.aquatic.crates.command.impl.keyCommand
import gg.aquatic.crates.command.impl.reloadCommand
import gg.aquatic.kommand.command

internal fun CratesPlugin.initializeCommands() {
    command("aqcrates", "acrates", "crates") {
        crateCommand()
        keyCommand()
        reloadCommand()
    }
}