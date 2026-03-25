package gg.aquatic.crates.reward.provider

import gg.aquatic.crates.reward.Reward
import gg.aquatic.execute.condition.ConditionHandle
import gg.aquatic.execute.checkConditions
import org.bukkit.entity.Player

class RewardPool(
    val id: String,
    val rewards: Collection<Reward>,
    val conditions: Collection<ConditionHandle<Player>>,
) {
    suspend fun matches(player: Player): Boolean {
        return conditions.checkConditions(player)
    }
}
