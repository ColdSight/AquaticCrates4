package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.data.action.RewardActionTypes
import gg.aquatic.crates.data.condition.*
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.crates.data.hologram.CrateHologramLineTypes
import gg.aquatic.crates.data.hologram.findHologramLineSubtypeId
import gg.aquatic.crates.data.interactable.*
import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.crates.data.price.OpenPriceGroupData
import gg.aquatic.crates.data.price.OpenPriceTypes
import gg.aquatic.crates.data.price.findOpenPriceSubtypeId
import gg.aquatic.crates.reward.RewardRarity
import gg.aquatic.execute.checkConditions
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
data class CrateData(
    val displayName: String = "<yellow>Crate",
    val keyItem: StackedItemData = StackedItemData(
        material = org.bukkit.Material.TRIPWIRE_HOOK.name,
        displayName = "<yellow>Crate Key"
    ),
    val interactables: List<@Polymorphic CrateInteractableData> = listOf(BlockCrateInteractableData()),
    val openConditions: List<@Polymorphic PlayerConditionData> = emptyList(),
    val priceGroups: List<OpenPriceGroupData> = listOf(OpenPriceGroupData()),
    val rarities: Map<String, RewardRarityData> = mapOf(DEFAULT_RARITY_ID to RewardRarityData(displayName = "<gray>Default")),
    val hologram: CrateHologramData? = null,
    val preview: PreviewMenuData? = PreviewMenuData(),
    val rewards: Map<String, RewardData> = emptyMap(),
) {

    fun normalized(crateId: String? = null, existingCrateIds: Set<String> = emptySet()): CrateData {
        val normalizedRarities = rarities
            .mapNotNull { (rarityId, data) ->
                rarityId.trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { it to data }
            }
            .toMap()
            .ifEmpty { mapOf(DEFAULT_RARITY_ID to RewardRarityData(displayName = "<gray>Default")) }

        val fallbackRarityId = normalizedRarities.keys.first()
        val availableRarityIds = normalizedRarities.keys

        return copy(
            priceGroups = priceGroups.map { it.normalized(crateId, existingCrateIds) },
            rarities = normalizedRarities,
            rewards = rewards.mapValues { (_, rewardData) ->
                rewardData.normalized(availableRarityIds, fallbackRarityId, crateId, existingCrateIds)
            }
        )
    }

    fun toCrate(id: String): Crate {
        val normalized = normalized(id)
        val crateKeyItem by lazy { normalized.keyItem.asStacked().getItem() }
        val resolvedRarities by lazy {
            normalized.rarities.mapValues { (rarityId, rarityData) ->
                rarityData.toRewardRarity(rarityId)
            }
        }
        val resolvedRewards by lazy {
            val fallbackRarity = resolvedRarities.values.first()
            normalized.rewards.entries.map { (rewardId, rewardData) ->
                val rewardRarity = resolvedRarities[rewardData.rarity] ?: fallbackRarity
                rewardData.toReward(rewardId, id, crateKeyItem, rewardRarity)
            }.toMutableList().also { rewards ->
                normalizeRewardChances(rewards, resolvedRarities)
            }
        }

        return Crate(
            id = id,
            keyItemSupplier = { crateKeyItem },
            displayName = normalized.displayName.toMMComponent(),
            hologramSupplier = { normalized.hologram?.toSettings() },
            priceGroupsSupplier = { normalized.priceGroups.map { it.toOpenPriceGroup(id, crateKeyItem) } },
            openConditionsSupplier = {
                normalized.openConditions
                    .map { it.toConditionHandle() }
                    .takeIf { it.isNotEmpty() }
                    ?.let { conditions ->
                        gg.aquatic.crates.open.OpenConditions { player, _, _ ->
                            conditions.checkConditions(player)
                        }
                    }
                    ?: gg.aquatic.crates.open.OpenConditions.DUMMY
            },
            interactables = normalized.interactables,
            rewardsSupplier = { resolvedRewards },
            previewSupplier = { normalized.preview?.toPreviewSettings() }
        )
    }

    companion object {
        const val DEFAULT_RARITY_ID = "default"

        fun createDefault(displayName: String = "<yellow>Crate"): CrateData {
            return CrateData(
                displayName = displayName,
                interactables = listOf(BlockCrateInteractableData()),
                priceGroups = listOf(OpenPriceGroupData()),
                rarities = mapOf(DEFAULT_RARITY_ID to RewardRarityData(displayName = "<gray>Default"))
            )
        }
    }
}

object CrateDataEditorSchema : EditableModel<CrateData>(CrateData.serializer()) {
    private val schemaJson = CrateDataFormats.json

    override fun resolveDescriptor(context: EditorFieldContext) = when {
        context.pathSegments.contains("hologram") ->
            context.findHologramLineSubtypeId()?.let(CrateHologramLineTypes::descriptor)
        context.pathSegments.contains("interactables") ->
            context.findInteractableSubtypeId()?.let(CrateInteractableTypes::descriptor)
        (context.pathSegments.contains("priceGroups") || context.pathSegments.contains("cost")) && context.pathSegments.contains("prices") ->
            context.findOpenPriceSubtypeId()?.let(OpenPriceTypes::descriptor)
        context.pathSegments.contains("winActions") || context.pathSegments.contains("clickActions") ->
            context.findRewardActionType()?.let(RewardActionTypes::descriptor)
        context.pathSegments.contains("openConditions") || context.pathSegments.contains("conditions") ->
            context.findConditionSubtypeId()?.let(PlayerConditionTypes::descriptor)
        else -> null
    }

    override fun TypedEditorSchemaBuilder<CrateData>.define() {
        field(
            CrateData::displayName,
            TextFieldAdapter,
            TextFieldConfig(prompt = "Enter crate display name:", showFormattedPreview = true),
            displayName = "Display Name",
            iconMaterial = Material.NAME_TAG,
            description = listOf("Main crate name shown in menus and other UI.")
        )
        list(
            CrateData::interactables,
            displayName = "Interactables",
            iconMaterial = Material.ARMOR_STAND,
            description = listOf("Clientside objects players can click to open this crate."),
            newValueFactory = CrateInteractableSelectionMenu.entryFactory
        ) {
            include<BlockCrateInteractableData> {
                with(BlockCrateInteractableData) {
                    defineEditor()
                }
            }
            include<EntityCrateInteractableData> {
                with(EntityCrateInteractableData) {
                    defineEditor()
                }
            }
            include<MEGCrateInteractableData> {
                with(MEGCrateInteractableData) {
                    defineEditor()
                }
            }
            include<MultiBlockCrateInteractableData> {
                with(MultiBlockCrateInteractableData) {
                    defineEditor()
                }
            }
        }
        list(
            CrateData::openConditions,
            displayName = "Open Conditions",
            iconMaterial = Material.TRIPWIRE_HOOK,
            description = listOf("Conditions that must pass before the crate can be opened."),
            newValueFactory = PlayerConditionSelectionMenu.entryFactory
        ) {
            definePlayerConditionEditor()
        }
        list(
            CrateData::priceGroups,
            displayName = "Price Groups",
            iconMaterial = Material.GOLD_INGOT,
            description = listOf(
                "Alternative price groups used to open this crate.",
                "If one group can be paid in full, the crate opens."
            ),
            newValueFactory = OpenPriceGroupData.defaultEntryFactory
        ) {
            with(OpenPriceGroupData) {
                defineEditor()
            }
        }
        map(
            CrateData::rarities,
            displayName = "Rarities",
            iconMaterial = Material.NETHER_STAR,
            description = listOf(
                "All rarity groups available in this crate.",
                "Each reward must belong to one of these rarities."
            ),
            mapKeyPrompt = "Enter rarity ID:",
            newMapEntryFactory = EditorEntryFactories.map(
                keyPrompt = "Enter rarity ID:",
                keyValidator = { if (CrateEditorValidators.crateIdRegex.matches(it)) null else "Use only letters, numbers, '_' or '-'." },
                valueFactory = { rarityId ->
                    schemaJson.encodeToJsonElement(
                        RewardRarityData.serializer(),
                        RewardRarityData(displayName = rarityId, chance = 1.0)
                    )
                }
            )
        ) {
            with(RewardRarityData) {
                defineEditor()
            }
        }

        group(CrateData::keyItem) {
            with(StackedItemData) {
                defineFullEditor(
                    materialLabel = "Key Material",
                    materialPrompt = "Enter material or Factory:ItemId:",
                    nameLabel = "Key Name",
                    namePrompt = "Enter key display name:",
                    loreLabel = "Key Lore"
                )
            }
        }

        optionalGroup(CrateData::hologram) {
            with(CrateHologramData) {
                defineEditor()
            }
        }

        optionalGroup(CrateData::preview) {
            with(PreviewMenuData) {
                defineEditor()
            }
        }

        map(
            CrateData::rewards,
            displayName = "Rewards",
            iconMaterial = Material.CHEST_MINECART,
            description = listOf("All rewards that can be won from this crate."),
            mapKeyPrompt = "Enter reward ID:",
            newMapEntryFactory = EditorEntryFactories.map(
                keyPrompt = "Enter reward ID:",
                keyValidator = { if (CrateEditorValidators.crateIdRegex.matches(it)) null else "Use only letters, numbers, '_' or '-'." },
                valueFactory = { rewardId ->
                    schemaJson.encodeToJsonElement(
                        RewardData.serializer(),
                        RewardData(
                            displayName = rewardId,
                            previewItem = StackedItemData(material = "CHEST", displayName = rewardId),
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

private fun normalizeRewardChances(
    rewards: MutableList<gg.aquatic.crates.reward.Reward>,
    rarities: Map<String, RewardRarity>
) {
    if (rewards.isEmpty() || rarities.isEmpty()) return

    val groupedRewards = rewards.groupBy { it.rarity.id }
    val activeRarities = rarities.filterKeys { groupedRewards.containsKey(it) }
    if (activeRarities.isEmpty()) return

    val totalRarityWeight = activeRarities.values.sumOf { it.chance.coerceAtLeast(0.0) }

    activeRarities.forEach { (rarityId, rarity) ->
        val rarityRewards = groupedRewards[rarityId].orEmpty()
        if (rarityRewards.isEmpty()) return@forEach

        val rarityFactor = when {
            totalRarityWeight > 0.0 -> rarity.chance.coerceAtLeast(0.0) / totalRarityWeight
            else -> 1.0 / activeRarities.size
        }

        val totalRewardWeight = rarityRewards.sumOf { it.chance.coerceAtLeast(0.0) }
        if (totalRewardWeight > 0.0) {
            rarityRewards.forEach { reward ->
                reward.chance = rarityFactor * (reward.chance.coerceAtLeast(0.0) / totalRewardWeight)
            }
        } else {
            val evenChance = rarityFactor / rarityRewards.size
            rarityRewards.forEach { reward ->
                reward.chance = evenChance
            }
        }
    }
}

private fun EditorFieldContext.findRewardActionType(): String? {
    if (!pathSegments.contains("winActions") && !pathSegments.contains("clickActions")) {
        return null
    }

    val direct = (value as? kotlinx.serialization.json.JsonObject)
        ?.get("type")
        ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
        ?.content
    if (direct != null) {
        return direct
    }

    val actionIndex = pathSegments.indexOfLast { it.toIntOrNull() != null }
    if (actionIndex == -1) {
        return null
    }

    var current: kotlinx.serialization.json.JsonElement = root
    for (segment in pathSegments.take(actionIndex + 1)) {
        current = when {
            segment.toIntOrNull() != null -> {
                val array = current as? kotlinx.serialization.json.JsonArray ?: return null
                array.getOrNull(segment.toInt()) ?: return null
            }

            else -> {
                val obj = current as? kotlinx.serialization.json.JsonObject ?: return null
                obj[segment] ?: return null
            }
        }
    }

    return (current as? kotlinx.serialization.json.JsonObject)
        ?.get("type")
        ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
        ?.content
}
