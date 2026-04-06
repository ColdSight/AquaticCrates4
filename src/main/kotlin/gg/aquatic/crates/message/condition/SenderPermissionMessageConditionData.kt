package gg.aquatic.crates.message.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.SenderPermissionCondition
import gg.aquatic.execute.condition.type.MessageConditionBinder
import gg.aquatic.klocale.impl.paper.ComponentVisibilityPayload
import gg.aquatic.waves.serialization.editor.meta.TextFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.TextFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("sender-permission")
data class SenderPermissionMessageConditionData(
    val permission: String = "example.permission"
) : MessageConditionData() {
    override fun toConditionHandle(): ConditionHandle<MessageConditionBinder> {
        return ConditionHandle(SenderPermissionCondition, ObjectArguments(mapOf("permission" to permission)))
    }

    override fun toPayload(): ComponentVisibilityPayload {
        return ComponentVisibilityPayload("sender-permission", mapOf("permission" to permission))
    }

    companion object {
        fun TypedNestedSchemaBuilder<SenderPermissionMessageConditionData>.defineEditor() {
            field(
                SenderPermissionMessageConditionData::permission,
                TextFieldAdapter,
                TextFieldConfig(prompt = "Enter permission node:"),
                displayName = "Permission",
                description = listOf("Sender must have this permission for the component to be visible.")
            )
        }
    }
}
