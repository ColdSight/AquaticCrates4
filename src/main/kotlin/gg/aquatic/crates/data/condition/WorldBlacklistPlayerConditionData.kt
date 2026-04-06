package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.Condition
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player

@Serializable
@SerialName("world-blacklist")
data class WorldBlacklistPlayerConditionData(
    val worlds: List<String> = listOf("world"),
) : PlayerConditionData() {
    override fun toOpenConditionHandle(): CrateOpenConditionHandle {
        val normalizedWorlds = worlds.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return CrateOpenConditionHandle.fromHandle(
            ConditionHandle(WorldBlacklistOpenCondition, ObjectArguments(mapOf("worlds" to normalizedWorlds)))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<WorldBlacklistPlayerConditionData>.defineEditor() {
            list(
                WorldBlacklistPlayerConditionData::worlds,
                displayName = "Worlds",
                iconMaterial = Material.BARRIER,
                description = listOf("Blocked world names for this crate open condition."),
                newValueFactory = EditorEntryFactories.text(
                    prompt = "Enter blacklisted world name:",
                    validator = {
                        if (Bukkit.getWorld(it.trim()) != null) null
                        else "Use an existing loaded world name."
                    }
                )
            )
        }
    }
}

private object WorldBlacklistOpenCondition : Condition<CrateOpenConditionBinder> {
    override val binder: Class<out CrateOpenConditionBinder> = CrateOpenConditionBinder::class.java
    override val arguments = emptyList<gg.aquatic.common.argument.ObjectArgument<*>>()

    override suspend fun execute(
        binder: CrateOpenConditionBinder,
        args: gg.aquatic.common.argument.ArgumentContext<CrateOpenConditionBinder>,
    ): Boolean {
        val worlds = args.stringCollection("worlds")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()

        if (worlds.isEmpty()) {
            return true
        }

        return binder.player.world.name !in worlds
    }
}
