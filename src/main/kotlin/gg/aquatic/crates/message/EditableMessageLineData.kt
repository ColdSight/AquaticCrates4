package gg.aquatic.crates.message

import gg.aquatic.klocale.impl.paper.ComponentVisibilityCodec
import kotlinx.serialization.Serializable

@Serializable
data class EditableMessageLineData(
    val components: List<MessageComponentData> = listOf(MessageComponentData()),
    val visibilityConditions: List<gg.aquatic.crates.message.condition.MessageConditionData> = emptyList(),
) {
    fun toMiniMessage(): String {
        var renderedLine = components.joinToString("<reset>") { component ->
            var rendered = component.text
            val click = component.click
            if (click != null && click.value.isNotBlank()) {
                val attribute = click.value.escapeMiniMessageAttribute()
                rendered = when (click.type) {
                MessageClickActionType.OPEN_URL -> "<click:open_url:'$attribute'>$rendered</click>"
                MessageClickActionType.RUN_COMMAND -> "<click:run_command:'$attribute'>$rendered</click>"
                MessageClickActionType.SUGGEST_COMMAND -> "<click:suggest_command:'$attribute'>$rendered</click>"
                MessageClickActionType.COPY_TO_CLIPBOARD -> "<click:copy_to_clipboard:'$attribute'>$rendered</click>"
                MessageClickActionType.PAGE -> "<click:suggest_command:'__aq_page__:$attribute'>$rendered</click>"
            }
        }
            if (component.hover.isNotEmpty()) {
                val hover = component.hover.joinToString("<newline>") { it.escapeMiniMessageAttribute() }
                rendered = "<hover:show_text:'$hover'>$rendered</hover>"
            }
            if (component.visibilityConditions.isNotEmpty()) {
                val payload = ComponentVisibilityCodec.encode(component.visibilityConditions.map { it.toPayload() })
                rendered = "<insert:'__aq_visible__:$payload'>$rendered"
            }
            rendered
        }
        if (visibilityConditions.isNotEmpty()) {
            val payload = ComponentVisibilityCodec.encode(visibilityConditions.map { it.toPayload() })
            renderedLine = "<insert:'__aq_visible__:$payload'>$renderedLine"
        }
        return renderedLine
    }
}

private fun String.escapeMiniMessageAttribute(): String {
    return replace("\\", "\\\\").replace("'", "\\'")
}
