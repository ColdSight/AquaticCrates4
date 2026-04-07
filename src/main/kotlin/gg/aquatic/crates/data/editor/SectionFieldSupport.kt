package gg.aquatic.crates.data.editor

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlNode
import gg.aquatic.kmenu.inventory.ButtonType
import gg.aquatic.waves.serialization.editor.SerializableEditor
import gg.aquatic.waves.serialization.editor.meta.EditorFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.EditorFieldContext
import gg.aquatic.waves.serialization.editor.meta.EditorSchema
import gg.aquatic.waves.serialization.editor.meta.FieldEditResult
import kotlinx.serialization.KSerializer
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

abstract class RootSectionFieldAdapter<T : Any>(
    private val serializer: KSerializer<T>,
    private val yaml: Yaml,
    private val schema: EditorSchema<T>,
    private val title: Component,
) : EditorFieldAdapter {

    protected abstract fun loadSection(context: EditorFieldContext): T?
    protected abstract fun defaultSectionValue(): T
    protected abstract fun updateRoot(root: YamlNode, edited: T): YamlNode

    protected open fun acceptsButton(buttonType: ButtonType): Boolean = buttonType == ButtonType.LEFT

    override suspend fun edit(
        player: Player,
        context: EditorFieldContext,
        buttonType: ButtonType
    ): FieldEditResult {
        if (!acceptsButton(buttonType)) {
            return FieldEditResult.NoChange
        }

        val edited = SerializableEditor.editValue(
            player = player,
            title = title,
            serializer = serializer,
            yaml = yaml,
            schema = schema,
            loadFresh = { loadSection(context) ?: defaultSectionValue() }
        ) ?: return FieldEditResult.NoChange

        return FieldEditResult.UpdatedRoot(updateRoot(context.root, edited))
    }
}

abstract class ValueSectionFieldAdapter<T : Any>(
    private val serializer: KSerializer<T>,
    private val yaml: Yaml,
    private val schema: EditorSchema<T>,
    private val title: Component,
) : EditorFieldAdapter {

    protected abstract fun loadSection(context: EditorFieldContext): T?
    protected abstract fun defaultSectionValue(): T
    protected abstract fun updateValue(edited: T): YamlNode

    protected open fun acceptsButton(buttonType: ButtonType): Boolean = buttonType == ButtonType.LEFT

    protected suspend fun editSectionValue(player: Player, context: EditorFieldContext): FieldEditResult {
        val edited = SerializableEditor.editValue(
            player = player,
            title = title,
            serializer = serializer,
            yaml = yaml,
            schema = schema,
            loadFresh = { loadSection(context) ?: defaultSectionValue() }
        ) ?: return FieldEditResult.NoChange

        return FieldEditResult.Updated(updateValue(edited))
    }

    override suspend fun edit(
        player: Player,
        context: EditorFieldContext,
        buttonType: ButtonType
    ): FieldEditResult {
        if (!acceptsButton(buttonType)) {
            return FieldEditResult.NoChange
        }

        return editSectionValue(player, context)
    }
}
