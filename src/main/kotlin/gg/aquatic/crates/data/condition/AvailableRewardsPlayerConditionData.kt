package gg.aquatic.crates.data.condition

import gg.aquatic.common.argument.ObjectArguments
import gg.aquatic.execute.Condition
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.waves.serialization.editor.meta.IntFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.IntFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.Material

@Serializable
@SerialName("available-rewards")
data class AvailableRewardsPlayerConditionData(
    val amount: Int = 1,
) : PlayerConditionData() {
    override fun toOpenConditionHandle(): CrateOpenConditionHandle {
        return CrateOpenConditionHandle.fromHandle(
            ConditionHandle(AvailableRewardsOpenCondition, ObjectArguments(mapOf("amount" to amount.coerceAtLeast(1))))
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<AvailableRewardsPlayerConditionData>.defineEditor() {
            field(
                AvailableRewardsPlayerConditionData::amount,
                IntFieldAdapter,
                IntFieldConfig(prompt = "Enter minimum available rewards:", min = 1),
                displayName = "Available Rewards",
                iconMaterial = Material.CHEST,
                description = listOf(
                    "Requires at least this many currently winnable rewards before the crate can open."
                )
            )
        }
    }
}

private object AvailableRewardsOpenCondition : Condition<CrateOpenConditionBinder> {
    override val binder: Class<out CrateOpenConditionBinder> = CrateOpenConditionBinder::class.java
    override val arguments = emptyList<gg.aquatic.common.argument.ObjectArgument<*>>()

    override suspend fun execute(
        binder: CrateOpenConditionBinder,
        args: gg.aquatic.common.argument.ArgumentContext<CrateOpenConditionBinder>,
    ): Boolean {
        val resolvedProvider = binder.crate.rewardProvider.resolve(binder.player)
        val available = resolvedProvider.rewards.count { it.canWin(binder.player) }
        val amount = args.int("amount")?.coerceAtLeast(1) ?: 1
        return available >= amount
    }
}
