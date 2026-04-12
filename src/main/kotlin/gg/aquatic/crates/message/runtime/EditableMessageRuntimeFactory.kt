package gg.aquatic.crates.message.runtime

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.message.EditableMessageData
import gg.aquatic.crates.util.withPlayerPlaceholder
import gg.aquatic.execute.Condition
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.type.MessageConditionBinder
import gg.aquatic.execute.executeActions
import gg.aquatic.klocale.impl.paper.ComponentVisibilityPayload
import gg.aquatic.klocale.impl.paper.MessageContext
import gg.aquatic.klocale.impl.paper.PaginatedMessageContext
import gg.aquatic.klocale.impl.paper.PaperMessage
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object EditableMessageRuntimeFactory {
    fun create(
        data: EditableMessageData,
        renderedLines: Iterable<Component>,
        paginationReplacements: Map<String, String> = emptyMap(),
    ): PaperMessage {
        val actionHandles = data.actions.map { it.toActionHandle() }
        val visibilityResolver: suspend (MessageContext, List<ComponentVisibilityPayload>) -> Boolean =
            { context, payloads ->
                val binder = when (context) {
                    is PaginatedMessageContext -> MessageConditionBinder(
                        sender = context.sender,
                        page = context.page,
                        totalPages = context.totalPages,
                    )
                    else -> MessageConditionBinder(
                        sender = context.sender,
                        page = 0,
                        totalPages = 1,
                    )
                }
                payloads.all { payload ->
                    @Suppress("UNCHECKED_CAST")
                    val condition = (Condition.REGISTRY as Condition.Companion.ConditionRegistry<MessageConditionBinder>)
                        .getHierarchicalByClass(payload.id, MessageConditionBinder::class.java)
                        as? Condition<MessageConditionBinder>
                        ?: return@all false

                    val args = ObjectArguments(
                        payload.args + mapOf(
                            "page" to binder.page,
                            "total-pages" to binder.totalPages
                        )
                    )
                    ConditionHandle(condition, args).execute(binder) { _, str -> str }
                }
            }

        return PaperMessage.of(
            renderedLines,
            pagination = data.pagination?.toSettings(paginationReplacements),
            visibilityResolver = visibilityResolver,
            callbacks = listOf<(CommandSender, PaperMessage) -> Unit>({ sender, _ ->
                if (sender is Player && actionHandles.isNotEmpty()) {
                    VirtualsCtx {
                        actionHandles.executeActions(sender, withPlayerPlaceholder(sender))
                    }
                }
            })
        )
    }

    fun create(data: EditableMessageData): PaperMessage {
        return create(
            data = data,
            renderedLines = data.lines.map { it.toMiniMessage().toMMComponent() }
        )
    }
}
