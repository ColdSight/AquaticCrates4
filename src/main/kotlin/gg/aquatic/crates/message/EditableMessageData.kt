package gg.aquatic.crates.message

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.common.coroutine.VirtualsCtx
import gg.aquatic.common.toMMComponent
import gg.aquatic.crates.data.action.RewardActionData
import gg.aquatic.crates.data.action.RewardActionSelectionMenu
import gg.aquatic.crates.data.action.defineRewardActionEditor
import gg.aquatic.execute.Condition
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.type.MessageConditionBinder
import gg.aquatic.execute.executeActions
import gg.aquatic.klocale.impl.paper.ComponentVisibilityPayload
import gg.aquatic.klocale.impl.paper.MessageContext
import gg.aquatic.klocale.impl.paper.PaginatedMessageContext
import gg.aquatic.klocale.impl.paper.PaperMessage
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Serializable
data class EditableMessageData(
    val lines: List<EditableMessageLineData> = listOf(EditableMessageLineData()),
    val pagination: EditableMessagePaginationData? = null,
    val actions: List<RewardActionData> = emptyList(),
) {
    fun toPaperMessage(): PaperMessage {
        return toPaperMessage(lines.map { it.toMiniMessage().toMMComponent() })
    }

    fun toPaperMessage(
        renderedLines: Iterable<Component>,
        paginationReplacements: Map<String, String> = emptyMap(),
    ): PaperMessage {
        val actionHandles = actions.map { it.toActionHandle() }
        val visibilityResolver: suspend (MessageContext, List<ComponentVisibilityPayload>) -> Boolean =
            visibility@{ context, payloads ->
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
            pagination = pagination?.toSettings(paginationReplacements),
            visibilityResolver = visibilityResolver,
            callbacks = listOf<(CommandSender, PaperMessage) -> Unit>({ sender, _ ->
                if (sender is Player && actionHandles.isNotEmpty()) {
                    VirtualsCtx {
                        actionHandles.executeActions(sender)
                    }
                }
            })
        )
    }

    companion object {
        fun lines(vararg value: String): EditableMessageData {
            return EditableMessageData(
                value.map { EditableMessageLineData(components = listOf(MessageComponentData(text = it))) }
            )
        }

        fun TypedNestedSchemaBuilder<EditableMessageData>.defineEditor() {
            list(
                EditableMessageData::lines,
                displayName = "Lines",
                iconMaterial = Material.WRITABLE_BOOK,
                description = listOf(
                    "Each entry is one rendered line of this message.",
                    "Each line is built from one or more components."
                ),
                newValueFactory = gg.aquatic.waves.serialization.editor.meta.EditorEntryFactories.text(
                    prompt = "Enter default line text:",
                    transform = {
                        MessagesFormats.json.encodeToJsonElement(
                            EditableMessageLineData.serializer(),
                            EditableMessageLineData(
                                components = listOf(MessageComponentData(text = it))
                            )
                        )
                    }
                )
            ) {
                defineMessageLineEditor()
            }
            list(
                EditableMessageData::actions,
                displayName = "Send Actions",
                iconMaterial = Material.BLAZE_POWDER,
                description = listOf(
                    "Actions executed when this message is sent to a player.",
                    "These do not run for console senders."
                ),
                newValueFactory = RewardActionSelectionMenu.entryFactory
            ) {
                defineRewardActionEditor()
            }
            field(
                EditableMessageData::pagination,
                displayName = "Pagination",
                iconMaterial = Material.BOOKSHELF,
                description = listOf(
                    "Optional pagination settings for this message.",
                    "When enabled, the message is split into pages with header/footer support."
                )
            )
            optionalGroup(EditableMessageData::pagination) {
                with(EditableMessagePaginationData) {
                    defineEditor()
                }
            }
        }
    }
}
