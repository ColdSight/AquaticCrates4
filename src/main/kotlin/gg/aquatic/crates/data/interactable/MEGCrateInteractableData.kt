package gg.aquatic.crates.data.interactable

import gg.aquatic.clientside.serialize.ClientsideMEGSettings
import gg.aquatic.crates.data.editor.CrateEditorValidators
import gg.aquatic.waves.serialization.editor.meta.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("meg")
data class MEGCrateInteractableData(
    val modelId: String = "crate_model",
    val viewRange: Int = 50,
) : CrateInteractableData() {

    override fun toSettings() = ClientsideMEGSettings(
        modelId = modelId,
        viewRange = viewRange
    )

    companion object {
        fun TypedNestedSchemaBuilder<MEGCrateInteractableData>.defineEditor() {
            field(
                MEGCrateInteractableData::modelId,
                TextFieldAdapter,
                TextFieldConfig(
                    prompt = "Enter MEG model id:",
                    validator = CrateEditorValidators::validateModelEngineModel
                ),
                displayName = "Model Id",
                description = listOf("ModelEngine model id used for this clientside interactable.")
            )
            field(
                MEGCrateInteractableData::viewRange,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter interactable view range:", min = 1),
                displayName = "View Range",
                description = listOf("Maximum distance where this clientside interactable stays visible.")
            )
        }
    }
}
