package gg.aquatic.crates.message

import gg.aquatic.crates.data.editor.encodeToNode
import gg.aquatic.crates.message.condition.MessageConditionSelectionMenu
import gg.aquatic.crates.message.condition.defineMessageConditionEditor
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import org.bukkit.Material

fun TypedNestedSchemaBuilder<EditableMessageLineData>.defineMessageLineEditor() {
    fieldPattern(
        adapter = MessageLineEntryFieldAdapter,
        iconMaterial = Material.WRITABLE_BOOK
    )
    list(
        EditableMessageLineData::visibilityConditions,
        displayName = "Visibility Conditions",
        iconMaterial = Material.COMPARATOR,
        description = listOf(
            "Optional conditions that must all pass for the whole line to be visible.",
            "Use this when all components in the line share the same condition."
        ),
        newValueFactory = MessageConditionSelectionMenu.entryFactory
    ) {
        defineMessageConditionEditor()
    }
    list(
        EditableMessageLineData::components,
        displayName = "Components",
        iconMaterial = Material.BOOK,
        description = listOf(
            "Ordered components joined into a single rendered line."
        ),
        newValueFactory = gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories.text(
            prompt = "Enter default component text:",
            transform = {
                MessagesFormats.yaml.encodeToNode(
                    MessageComponentData.serializer(),
                    MessageComponentData(text = it)
                )
            }
        )
    ) {
        fieldPattern(
            adapter = MessageComponentEntryFieldAdapter,
            iconMaterial = Material.PAPER
        )
        with(MessageComponentData) {
            defineEditor()
        }
    }
}
