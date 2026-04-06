package gg.aquatic.crates.message

import kotlinx.serialization.Serializable

@Serializable
enum class MessageClickActionType {
    OPEN_URL,
    RUN_COMMAND,
    SUGGEST_COMMAND,
    COPY_TO_CLIPBOARD,
    PAGE,
}
