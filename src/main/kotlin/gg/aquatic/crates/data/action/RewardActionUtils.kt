package gg.aquatic.crates.data.action

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.common.argument.ObjectArgument
import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.Action
import gg.aquatic.execute.ActionHandle
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.bukkit.entity.Player

internal fun inlinePlayerAction(executor: suspend (Player) -> Unit): ActionHandle<Player> {
    return ActionHandle(
        object : Action<Player> {
            override val binder: Class<out Player> = Player::class.java
            override val arguments: List<ObjectArgument<*>> = emptyList()

            override suspend fun execute(binder: Player, args: ArgumentContext<Player>) {
                executor(binder)
            }
        },
        ObjectArguments(emptyMap())
    )
}

internal fun EditorFieldContext.matchesSubtype(id: String): Boolean {
    val currentType = findSubtypeId() ?: return false
    return currentType.equals(id, ignoreCase = true)
}

private fun EditorFieldContext.findSubtypeId(): String? {
    val direct = (value as? JsonObject)
        ?.get("type")
        ?.let { it as? JsonPrimitive }
        ?.contentOrNull
    if (direct != null) {
        return direct
    }

    val numericIndex = pathSegments.indexOfLast { it.toIntOrNull() != null }
    if (numericIndex == -1) {
        return null
    }

    val actionElement = root.findByPath(pathSegments.take(numericIndex + 1)) as? JsonObject ?: return null
    return actionElement["type"]
        ?.let { it as? JsonPrimitive }
        ?.contentOrNull
}

private fun JsonElement.findByPath(path: List<String>): JsonElement? {
    var current: JsonElement = this
    for (segment in path) {
        current = when {
            segment.toIntOrNull() != null -> {
                if (current !is kotlinx.serialization.json.JsonArray) return null
                current.getOrNull(segment.toInt()) ?: return null
            }

            else -> {
                if (current !is JsonObject) return null
                current[segment] ?: return null
            }
        }
    }
    return current
}
