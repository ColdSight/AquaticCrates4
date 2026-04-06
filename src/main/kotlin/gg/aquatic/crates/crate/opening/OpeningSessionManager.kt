package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.crate.Crate
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object OpeningSessionManager {
    private val sessions = ConcurrentHashMap<UUID, OpeningSession>()

    fun tryStart(player: Player, crate: Crate): OpeningSession? {
        val session = OpeningSession(player = player, crate = crate)
        return if (sessions.putIfAbsent(player.uniqueId, session) == null) session else null
    }

    fun current(player: Player): OpeningSession? {
        return sessions[player.uniqueId]
    }

    fun finish(session: OpeningSession, failed: Boolean = false) {
        session.stage = if (failed) OpeningStage.FAILED else OpeningStage.COMPLETED
        sessions.remove(session.player.uniqueId, session)
    }
}
