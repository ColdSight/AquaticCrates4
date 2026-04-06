package gg.aquatic.crates.command.impl

import gg.aquatic.crates.Messages
import gg.aquatic.crates.crate.CrateHandler
import gg.aquatic.crates.message.replacePlaceholder
import gg.aquatic.crates.stats.CrateStats
import gg.aquatic.kommand.CommandBuilder
import gg.aquatic.kommand.hasPermission
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.command.CommandSender

internal fun CommandBuilder<CommandSourceStack, CommandSender>.statsCommand() =
    "stats" {
        hasPermission("aqcrates.admin")

        "invalidate" {
            suspendExecute<CommandSender> {
                val result = CrateStats.invalidate(CrateHandler.crates.keys)

                Messages.STATS_INVALIDATED.message()
                    .replacePlaceholder("%total_deleted%", result.totalDeletedRows.toString())
                    .replacePlaceholder("%deleted_openings%", result.deletedOpenings.toString())
                    .replacePlaceholder("%deleted_opening_rewards%", result.deletedOpeningRewards.toString())
                    .replacePlaceholder("%deleted_hourly_crate_buckets%", result.deletedHourlyCrateBuckets.toString())
                    .replacePlaceholder("%deleted_hourly_reward_buckets%", result.deletedHourlyRewardBuckets.toString())
                    .replacePlaceholder("%deleted_alltime_crate_rows%", result.deletedAllTimeCrateRows.toString())
                    .replacePlaceholder("%deleted_alltime_reward_rows%", result.deletedAllTimeRewardRows.toString())
                    .replacePlaceholder("%deleted_expired_hourly_crate_buckets%", result.deletedExpiredHourlyCrateBuckets.toString())
                    .replacePlaceholder("%deleted_expired_hourly_reward_buckets%", result.deletedExpiredHourlyRewardBuckets.toString())
                    .send(sender)
            }
        }
    }
