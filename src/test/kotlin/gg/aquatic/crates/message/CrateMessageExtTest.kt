package gg.aquatic.crates.message

import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.klocale.impl.paper.toPlain
import net.kyori.adventure.text.Component
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CrateMessageExtTest {
    @Test
    fun `replacePlaceholder replaces percent wrapped placeholder keys`() {
        val rendered = PaperMessage.of(
            listOf(Component.text("Opened %amount%x %crate_id%"))
        )
            .replacePlaceholder("%amount%", "100")
            .replacePlaceholder("%crate_id%", "test")
            .lines
            .joinToString("\n") { it.component.toPlain() }

        assertFalse(rendered.contains("%amount%"))
        assertFalse(rendered.contains("%crate_id%"))
        assertFalse(rendered.contains(" amountx"))
        assertFalse(rendered.contains(" crate_id"))
        assertTrue(rendered.contains("100x"))
        assertTrue(rendered.contains("test"))
    }
}
