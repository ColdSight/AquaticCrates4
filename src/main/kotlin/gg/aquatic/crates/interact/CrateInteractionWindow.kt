package gg.aquatic.crates.interact

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class CrateInteractionWindow(
    private val windowMillis: Long,
) {
    private val lastInteractions = ConcurrentHashMap<UUID, Long>()
    private val lastCrateClaims = ConcurrentHashMap<UUID, Long>()

    fun tryMarkExecuted(playerId: UUID, now: Long): Boolean {
        val previous = lastInteractions[playerId]
        if (previous != null && now - previous < windowMillis) {
            return false
        }
        lastInteractions[playerId] = now
        return true
    }

    fun claimCrateInteraction(playerId: UUID, now: Long) {
        lastCrateClaims[playerId] = now
    }

    fun wasCrateInteractionClaimedSince(playerId: UUID, sinceMillis: Long): Boolean {
        val claimedAt = lastCrateClaims[playerId] ?: return false
        return claimedAt >= sinceMillis
    }
}
