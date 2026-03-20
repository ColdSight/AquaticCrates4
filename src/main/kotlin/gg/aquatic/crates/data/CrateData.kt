package gg.aquatic.crates.data

import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.data.action.RewardActionTypes
import gg.aquatic.crates.data.condition.PlayerConditionData
import gg.aquatic.crates.data.condition.PlayerConditionSelectionMenu
import gg.aquatic.crates.data.condition.PlayerConditionTypes
import gg.aquatic.crates.data.condition.findConditionSubtypeId
import gg.aquatic.crates.data.condition.definePlayerConditionEditor
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.crates.data.hologram.CrateHologramLineTypes
import gg.aquatic.crates.data.hologram.findHologramLineSubtypeId
import gg.aquatic.crates.data.interactable.BlockCrateInteractableData
import gg.aquatic.crates.data.interactable.CrateInteractableData
import gg.aquatic.crates.data.interactable.CrateInteractableSelectionMenu
import gg.aquatic.crates.data.interactable.CrateInteractableTypes
import gg.aquatic.crates.data.interactable.EntityCrateInteractableData
import gg.aquatic.crates.data.interactable.MEGCrateInteractableData
import gg.aquatic.crates.data.interactable.MultiBlockCrateInteractableData
import gg.aquatic.crates.data.interactable.findInteractableSubtypeId
import gg.aquatic.crates.data.item.StackedItemData
import gg.aquatic.execute.checkConditions
import gg.aquatic.waves.serialization.editor.meta.EditableModel
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedEditorSchemaBuilder
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CrateData(
    val displayName: String = "<yellow>Crate",
    val keyItem: StackedItemData = StackedItemData(
        material = org.bukkit.Material.TRIPWIRE_HOOK.name,
        displayName = "<yellow>Crate Key"
    ),
    val interactables: List<@Polymorphic CrateInteractableData> = listOf(BlockCrateInteractableData()),
    val openConditions: List<@Polymorphic PlayerConditionData> = emptyList(),
    val hologram: CrateHologramData? = null,
    val preview: PreviewMenuData? = PreviewMenuData(),
    val rewards: Map<String, RewardData> = emptyMap(),
) {

    fun toCrate(id: String): Crate {
        return Crate(
            id = id,
            keyItem = keyItem.asStacked().getItem(),
            displayName = displayName.toMMComponent(),
            hologram = hologram?.toSettings(),
            priceGroups = emptyList(),
            openConditions = openConditions
                .map { it.toConditionHandle() }
                .takeIf { it.isNotEmpty() }
                ?.let { conditions ->
                    gg.aquatic.crates.open.OpenConditions { player, _, _ ->
                        conditions.checkConditions(player)
                    }
                }
                ?: gg.aquatic.crates.open.OpenConditions.DUMMY,
            interactables = interactables,
            rewards = rewards.entries.map { (rewardId, rewardData) ->
                rewardData.toReward(rewardId)
            },
            preview = preview?.toPreviewSettings()
        )
    }

    companion object {
        fun createDefault(displayName: String = "<yellow>Crate"): CrateData {
            return CrateData(
                displayName = displayName,
                interactables = listOf(BlockCrateInteractableData())
            )
        }
    }
}

object CrateDataEditorSchema : EditableModel<CrateData>(CrateData.serializer()) {
    private val schemaJson = Json { encodeDefaults = true }

    override fun resolveDescriptor(context: EditorFieldContext) = when {
        context.pathSegments.contains("hologram") ->
            context.findHologramLineSubtypeId()?.let(CrateHologramLineTypes::descriptor)
        context.pathSegments.contains("interactables") ->
            context.findInteractableSubtypeId()?.let(CrateInteractableTypes::descriptor)
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
            description = listOf("Main crate name shown in menus and other UI.")
        )
        list(
            CrateData::interactables,
            displayName = "Interactables",
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
            description = listOf("Conditions that must pass before the crate can be opened."),
            newValueFactory = PlayerConditionSelectionMenu.entryFactory
        ) {
            definePlayerConditionEditor()
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
                            rewardItem = StackedItemData(material = "DIAMOND", displayName = rewardId)
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
