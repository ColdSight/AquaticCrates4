package gg.aquatic.crates.data.provider

import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import gg.aquatic.crates.data.editor.SwitchingSectionFieldAdapter
import gg.aquatic.crates.data.editor.stringContentOrNull
import gg.aquatic.crates.data.editor.withMapValue
import gg.aquatic.crates.data.editor.yamlScalar
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.FieldEditResult
import org.bukkit.Material
import org.bukkit.entity.Player

object RewardProviderSectionFieldAdapter : SwitchingSectionFieldAdapter(
    sectionName = "Rewards",
    iconMaterial = Material.CHEST_MINECART,
    defaultType = RewardProviderType.SIMPLE.id,
    editHint = "Edit reward provider settings",
    changeHint = "Change reward provider type"
) {
    override suspend fun selectType(player: Player): String? = RewardProviderTypeSelectionMenu.select(player)

    override fun updateType(context: EditorFieldContext, selected: String): FieldEditResult {
        return FieldEditResult.UpdatedRoot(updateRewardProviderType(context.root, selected))
    }

    override fun currentType(context: EditorFieldContext): String {
        val root = context.root as? YamlMap ?: return RewardProviderType.SIMPLE.id
        return root.get<YamlNode>("rewardProviderType")?.stringContentOrNull ?: RewardProviderType.SIMPLE.id
    }

    private fun updateRewardProviderType(root: YamlNode, type: String): YamlNode {
        return root.withMapValue("rewardProviderType", yamlScalar(type))
    }
}
