package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.condition.impl.HasEmptyInventorySlotCondition
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
@SerialName("has-empty-inventory-slot")
data class HasEmptyInventorySlotPlayerConditionData(
    val amount: Int = 1,
) : PlayerConditionData() {
    override fun toConditionHandle(): ConditionHandle<org.bukkit.entity.Player> {
        return ConditionHandle(
            HasEmptyInventorySlotCondition,
            ObjectArguments(mapOf("amount" to amount.coerceAtLeast(1)))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<HasEmptyInventorySlotPlayerConditionData>.defineEditor() {
            field(
                HasEmptyInventorySlotPlayerConditionData::amount,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter required empty slot amount:", min = 1),
                displayName = "Amount",
                iconMaterial = Material.HOPPER,
                description = listOf(
                    "Minimum number of empty inventory slots required.",
                    "Use 1 to only require at least one free slot."
                )
            )
        }
    }
}
