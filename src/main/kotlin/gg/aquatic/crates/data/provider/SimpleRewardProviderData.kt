package gg.aquatic.crates.data.provider

import gg.aquatic.crates.data.CrateData
import gg.aquatic.crates.data.CrateDataFormats
import gg.aquatic.crates.data.RewardData
import gg.aquatic.crates.data.range.RewardAmountRangeData
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class SimpleRewardProviderData(
    val rewardCountRanges: List<RewardAmountRangeData> = emptyList(),
    val rewards: Map<String, RewardData> = emptyMap(),
) {
    fun normalized(
        availableRarityIds: Set<String>,
        fallbackRarityId: String,
        currentCrateId: String? = null,
        existingCrateIds: Set<String> = emptySet(),
    ): SimpleRewardProviderData {
        return copy(
            rewardCountRanges = rewardCountRanges.map { it.normalized() },
            rewards = rewards.mapValues { (_, rewardData) ->
                rewardData.normalized(availableRarityIds, fallbackRarityId, currentCrateId, existingCrateIds)
            }
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<SimpleRewardProviderData>.defineEditor() {
            list(
                SimpleRewardProviderData::rewardCountRanges,
                displayName = "Reward Count Ranges",
                iconMaterial = Material.HOPPER,
                description = listOf(
                    "Controls how many rewards can be rolled from one crate open.",
                    "If empty, one reward is rolled by default."
                )
            ) {
                with(RewardAmountRangeData) {
                    defineEditor(
                        minLabel = "Min Rewards",
                        maxLabel = "Max Rewards",
                        chanceLabel = "Range Weight"
                    )
                }
            }
            map(
                SimpleRewardProviderData::rewards,
                displayName = "Rewards",
                iconMaterial = Material.CHEST_MINECART,
                description = listOf("All rewards that can be won from this crate."),
                mapKeyPrompt = "Enter reward ID:",
                newMapEntryFactory = EditorEntryFactories.map(
                    keyPrompt = "Enter reward ID:",
                    keyValidator = { if (CrateEditorValidators.crateIdRegex.matches(it)) null else "Use only letters, numbers, '_' or '-'." },
                    valueFactory = { rewardId ->
                        CrateDataFormats.json.encodeToJsonElement(
                            RewardData.serializer(),
                            RewardData(
                                displayName = rewardId,
                                previewItem = gg.aquatic.crates.data.item.StackedItemData(
                                    material = Material.CHEST.name,
                                    displayName = rewardId
                                ),
                                rarity = CrateData.DEFAULT_RARITY_ID
                            )
                        )
                    }
                )
            ) {
                with(RewardData) {
                    defineEditor()
                }
            }
        }
    }
}
