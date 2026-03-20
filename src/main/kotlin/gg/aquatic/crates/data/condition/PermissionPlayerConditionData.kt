package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.impl.PermissionCondition
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("permission")
data class PermissionPlayerConditionData(
    val permission: String = "example.permission"
) : PlayerConditionData() {
    override fun toConditionHandle() = conditionHandle(
        PermissionCondition,
        ObjectArguments(mapOf("permission" to permission))
    )

    companion object {
        fun TypedNestedSchemaBuilder<PermissionPlayerConditionData>.defineEditor() {
            field(
                PermissionPlayerConditionData::permission,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter permission node:"),
                displayName = "Permission",
                description = listOf("Player must have this permission for the condition to pass.")
            )
        }
    }
}
