package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.WorldCondition
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Material

@Serializable
@SerialName("world")
data class WorldPlayerConditionData(
    val worlds: List<String> = listOf("world"),
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        val normalizedWorlds = worlds
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        return ConditionHandle(
            WorldCondition,
            ObjectArguments(mapOf("worlds" to normalizedWorlds))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<WorldPlayerConditionData>.defineEditor() {
            list(
                WorldPlayerConditionData::worlds,
                displayName = "Worlds",
                iconMaterial = Material.GRASS_BLOCK,
                description = listOf("Allowed world names for this condition."),
                newValueFactory = EditorEntryFactories.text(
                    prompt = "Enter world name:",
                    validator = {
                        if (Bukkit.getWorld(it.trim()) != null) null
                        else "Use an existing loaded world name."
                    }
                )
            )
        }
    }
}
