package gg.aquatic.crates.crate.opening

import gg.aquatic.crates.crate.Crate
import io.mockk.every
import io.mockk.mockk
import java.util.UUID
import org.bukkit.entity.Player
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OpeningReservationTest {
    @Test
    fun `tryStart allows only one active session per player`() {
        val reservation = OpeningReservation()
        val player = player(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"))
        val session1 = OpeningSession(player = player, crate = crate())
        val session2 = OpeningSession(player = player, crate = crate())

        val started = reservation.tryStart(session1)
        val rejected = reservation.tryStart(session2)

        assertEquals(session1, started)
        assertNull(rejected)
        assertEquals(session1, reservation.current(player.uniqueId))
    }

    @Test
    fun `finish removes session and updates final stage`() {
        val reservation = OpeningReservation()
        val player = player(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"))
        val session = OpeningSession(player = player, crate = crate())
        reservation.tryStart(session)

        reservation.finish(session, failed = true)

        assertEquals(OpeningStage.FAILED, session.stage)
        assertNull(reservation.current(player.uniqueId))
    }

    private fun player(uuid: UUID): Player = mockk {
        every { uniqueId } returns uuid
    }

    private fun crate(): Crate = mockk(relaxed = true)
}
