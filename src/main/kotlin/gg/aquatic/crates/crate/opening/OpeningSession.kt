package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.crate.Crate
import org.bukkit.entity.Player
import java.util.UUID

class OpeningSession(
    val id: UUID = UUID.randomUUID(),
    val player: Player,
    val crate: Crate,
    val startedAtMillis: Long = System.currentTimeMillis(),
) {
    @Volatile
    var stage: OpeningStage = OpeningStage.STARTING
        internal set
}
