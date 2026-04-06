package gg.aquatic.crates.message.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.HasNextPageCondition
import gg.aquatic.execute.condition.type.MessageConditionBinder
import gg.aquatic.klocale.impl.paper.ComponentVisibilityPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("has-next-page")
class HasNextPageMessageConditionData : MessageConditionData() {
    override fun toConditionHandle(): ConditionHandle<MessageConditionBinder> {
        return ConditionHandle(HasNextPageCondition, ObjectArguments(emptyMap()))
    }

    override fun toPayload(): ComponentVisibilityPayload {
        return ComponentVisibilityPayload("has-next-page")
    }
}
