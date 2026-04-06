package gg.aquatic.crates.message.condition

import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.type.MessageConditionBinder
import gg.aquatic.klocale.impl.paper.ComponentVisibilityPayload
import kotlinx.serialization.Serializable

@Serializable
abstract class MessageConditionData {
    abstract fun toConditionHandle(): ConditionHandle<MessageConditionBinder>
    abstract fun toPayload(): ComponentVisibilityPayload
}
