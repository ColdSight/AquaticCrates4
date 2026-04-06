package gg.aquatic.crates.crate

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CrateInteractionGuard {
    private const val WINDOW_MILLIS = 75L
    private val lastInteractions = ConcurrentHashMap<UUID, Long>()
    private val lastCrateClaims = ConcurrentHashMap<UUID, Long>()

    fun tryMarkExecuted(playerId: UUID): Boolean {
        val now = System.currentTimeMillis()
        val previous = lastInteractions[playerId]
        if (previous != null && now - previous < WINDOW_MILLIS) {
            return false
        }
        lastInteractions[playerId] = now
        return true
    }

    fun claimCrateInteraction(playerId: UUID) {
        lastCrateClaims[playerId] = System.currentTimeMillis()
    }

    fun wasCrateInteractionClaimedSince(playerId: UUID, sinceMillis: Long): Boolean {
        val claimedAt = lastCrateClaims[playerId] ?: return false
        return claimedAt >= sinceMillis
    }
}
