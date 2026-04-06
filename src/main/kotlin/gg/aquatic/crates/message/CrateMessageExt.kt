package gg.aquatic.crates.message

import gg.aquatic.klocale.impl.paper.PaperMessage
import net.kyori.adventure.text.Component

fun PaperMessage.replacePlaceholder(
    placeholder: String,
    replacement: String
): PaperMessage {
    return replace(normalizePlaceholderKey(placeholder), replacement)
}

fun PaperMessage.replacePlaceholder(
    placeholder: String,
    component: Component
): PaperMessage {
    return replace(normalizePlaceholderKey(placeholder), component)
}

private fun normalizePlaceholderKey(placeholder: String): String {
    return placeholder.removePrefix("%").removeSuffix("%")
}
