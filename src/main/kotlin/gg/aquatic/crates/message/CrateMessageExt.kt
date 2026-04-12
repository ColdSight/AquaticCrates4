package gg.aquatic.crates.message

import gg.aquatic.klocale.impl.paper.PaperMessage
import net.kyori.adventure.text.Component

fun PaperMessage.replacePlaceholder(
    placeholder: String,
    replacement: String
): PaperMessage {
    val normalized = normalizePlaceholderKey(placeholder)
    val replacements = lines
        .flatMap { it.placeholders }
        .toSet()
        .associateWith { key ->
            if (key == normalized) replacement else "%$key%"
        }

    return replace(replacements)
}

fun PaperMessage.replacePlaceholder(
    placeholder: String,
    component: Component
): PaperMessage {
    val normalized = normalizePlaceholderKey(placeholder)
    return replace(normalized, component)
}

private fun normalizePlaceholderKey(placeholder: String): String {
    return placeholder.removePrefix("%").removeSuffix("%")
}
