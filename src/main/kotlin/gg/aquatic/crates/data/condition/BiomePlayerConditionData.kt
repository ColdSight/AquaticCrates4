package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.BiomeCondition
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Biome
import java.util.Locale

@Serializable
@SerialName("biome")
data class BiomePlayerConditionData(
    val biomes: List<String> = listOf("plains"),
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        val normalizedBiomes = biomes
            .mapNotNull { raw -> resolveBiome(raw)?.key?.key }
            .distinct()
        return ConditionHandle(
            BiomeCondition,
            ObjectArguments(mapOf("biomes" to normalizedBiomes))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<BiomePlayerConditionData>.defineEditor() {
            list(
                BiomePlayerConditionData::biomes,
                displayName = "Biomes",
                iconMaterial = Material.MOSS_BLOCK,
                description = listOf("Allowed biome names for this condition."),
                newValueFactory = EditorEntryFactories.text(
                    prompt = "Enter biome:",
                    validator = {
                        if (resolveBiome(it) != null) null
                        else "Use a valid biome like plains."
                    }
                )
            )
        }

        private fun resolveBiome(raw: String): Biome? {
            val normalized = raw.trim()
                .lowercase(Locale.ROOT)
                .replace(' ', '_')
            if (normalized.isEmpty()) {
                return null
            }

            val key = NamespacedKey.fromString(normalized)
                ?: NamespacedKey.minecraft(normalized)
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(key)
        }
    }
}
