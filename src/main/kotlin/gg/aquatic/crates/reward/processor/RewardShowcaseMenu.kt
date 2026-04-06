package gg.aquatic.crates.reward.processor

import gg.aquatic.common.coroutine.BukkitCtx
import gg.aquatic.crates.data.processor.RewardDisplayMenuSettings
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.kmenu.menu.util.ListMenu
import gg.aquatic.replace.PlaceholderContext
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player

class RewardShowcaseMenu private constructor(
    player: Player,
    entries: List<ListMenu.Entry<RolledReward>>,
    private val settings: RewardDisplayMenuSettings,
) : ListMenu<RolledReward>(
    settings.invSettings.title,
    settings.invSettings.type,
    player,
    entries,
    Sorting.empty(),
    settings.rewardSlots,
) {
    companion object {
        suspend fun create(
            player: Player,
            rewards: List<RolledReward>,
            settings: RewardDisplayMenuSettings,
        ): RewardShowcaseMenu {
            val entries = rewards.map { rolled ->
                ListMenu.Entry(
                    rolled,
                    { rolled.displayItem(completed = true) },
                    PlaceholderContext.privateMenu(),
                    {}
                )
            }

            return RewardShowcaseMenu(player, entries, settings).apply {
                addButtons()
                settings.anvilSettings?.applyTo(this)
            }
        }
    }

    private val context = PlaceholderContext.privateMenu()

    private suspend fun addButtons() {
        for ((id, comp) in settings.invSettings.components) {
            if (id.lowercase() == "next-page") {
                injectNextButton(comp)
                continue
            }
            if (id.lowercase() == "prev-page") {
                injectPreviousButton(comp)
                continue
            }

            addComponent(comp.create(context))
        }
    }
}
