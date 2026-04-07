package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.crate.Crate
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object OpeningSessionManager {
    private val reservation = OpeningReservation()

    fun tryStart(player: Player, crate: Crate): OpeningSession? {
        val session = OpeningSession(player = player, crate = crate)
        return reservation.tryStart(session)
    }

    fun current(player: Player): OpeningSession? {
        return reservation.current(player.uniqueId)
    }

    fun finish(session: OpeningSession, failed: Boolean = false) {
        reservation.finish(session, failed)
    }
}
