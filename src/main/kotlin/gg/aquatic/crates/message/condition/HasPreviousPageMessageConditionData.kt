package gg.aquatic.crates.message.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.HasPreviousPageCondition
import gg.aquatic.execute.condition.type.MessageConditionBinder
import gg.aquatic.klocale.impl.paper.ComponentVisibilityPayload
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("has-previous-page")
class HasPreviousPageMessageConditionData : MessageConditionData() {
    override fun toConditionHandle(): ConditionHandle<MessageConditionBinder> {
        return ConditionHandle(HasPreviousPageCondition, ObjectArguments(emptyMap()))
    }

    override fun toPayload(): ComponentVisibilityPayload {
        return ComponentVisibilityPayload("has-previous-page")
    }
}
