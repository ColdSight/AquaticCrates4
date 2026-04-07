package gg.aquatic.crates.crate.opening

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class OpeningReservation {
    private val sessions = ConcurrentHashMap<UUID, OpeningSession>()

    fun tryStart(session: OpeningSession): OpeningSession? {
        return if (sessions.putIfAbsent(session.player.uniqueId, session) == null) session else null
    }

    fun current(playerId: UUID): OpeningSession? {
        return sessions[playerId]
    }

    fun finish(session: OpeningSession, failed: Boolean = false) {
        session.stage = if (failed) OpeningStage.FAILED else OpeningStage.COMPLETED
        sessions.remove(session.player.uniqueId, session)
    }
}
