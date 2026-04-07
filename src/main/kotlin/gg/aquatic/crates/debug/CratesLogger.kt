package gg.aquatic.crates.debug

import java.util.logging.Level
import java.util.logging.Logger

object CratesLogger {
    private val logger = Logger.getLogger("AquaticCrates")

    fun info(message: String) {
        logger.log(Level.INFO, message)
    }

    fun warning(message: String) {
        logger.log(Level.WARNING, message)
    }

    fun severe(message: String) {
        logger.log(Level.SEVERE, message)
    }

    fun info(category: CratesLogCategory, message: String) {
        info("[${category.name.lowercase()}] $message")
    }

    fun warning(category: CratesLogCategory, message: String) {
        warning("[${category.name.lowercase()}] $message")
    }

    fun severe(category: CratesLogCategory, message: String) {
        severe("[${category.name.lowercase()}] $message")
    }
}
