package gg.aquatic.crates.data.processor

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import gg.aquatic.crates.data.editor.SwitchingSectionFieldAdapter
import gg.aquatic.crates.data.editor.stringContentOrNull
import gg.aquatic.crates.data.editor.withMapValue
import gg.aquatic.crates.data.editor.yamlScalar
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.FieldEditResult
import org.bukkit.Material
import org.bukkit.entity.Player

object RewardProcessorSectionFieldAdapter : SwitchingSectionFieldAdapter(
    sectionName = "Reward Processor",
    iconMaterial = Material.HOPPER_MINECART,
    defaultType = RewardProcessorType.BASIC.id,
    editHint = "Edit processor settings",
    changeHint = "Change processor type"
) {
    override suspend fun selectType(player: Player): String? = RewardProcessorTypeSelectionMenu.select(player)

    override fun updateType(context: EditorFieldContext, selected: String): FieldEditResult {
        return FieldEditResult.UpdatedRoot(updateRewardProcessorType(context.root, selected))
    }

    override fun currentType(context: EditorFieldContext): String {
        val root = context.root as? YamlMap ?: return RewardProcessorType.BASIC.id
        return root.get<YamlNode>("rewardProcessorType")?.stringContentOrNull ?: RewardProcessorType.BASIC.id
    }

    private fun updateRewardProcessorType(root: YamlNode, type: String): YamlNode {
        return root.withMapValue("rewardProcessorType", yamlScalar(type))
    }
}
