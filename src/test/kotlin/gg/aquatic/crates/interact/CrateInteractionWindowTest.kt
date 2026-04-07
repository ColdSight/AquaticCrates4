package gg.aquatic.crates.interact

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrateInteractionWindowTest {
    @Test
    fun `tryMarkExecuted blocks duplicate interactions inside window`() {
        val window = CrateInteractionWindow(windowMillis = 75L)
        val player = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")

        assertTrue(window.tryMarkExecuted(player, now = 1_000L))
        assertFalse(window.tryMarkExecuted(player, now = 1_050L))
        assertTrue(window.tryMarkExecuted(player, now = 1_100L))
    }

    @Test
    fun `crate claim takes priority for later key interaction checks`() {
        val window = CrateInteractionWindow(windowMillis = 75L)
        val player = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")

        window.claimCrateInteraction(player, now = 2_000L)

        assertTrue(window.wasCrateInteractionClaimedSince(player, sinceMillis = 2_000L))
        assertTrue(window.wasCrateInteractionClaimedSince(player, sinceMillis = 1_999L))
        assertFalse(window.wasCrateInteractionClaimedSince(player, sinceMillis = 2_001L))
    }
}
