package gg.aquatic.crates.crate.preview

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.crate.preview.runtime.PreviewRewardEntries
import gg.aquatic.kmenu.menu.util.ListMenu
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.replace.PlaceholderContext
import org.bukkit.entity.Player
import gg.aquatic.crates.reward.Reward

class PreviewListMenu private constructor(
    player: Player,
    val crate: Crate,
    val crateHandle: CrateHandle?,
    entries: List<Entry<Reward>>,
    private val settings: PreviewMenuSettings.Basic,
) : ListMenu<Reward>(
    settings.invSettings.title,
    settings.invSettings.type,
    player,
    entries,
    Sorting.empty(),
    settings.rewardSlots,
) {

    companion object {
        suspend fun mappedEntries(crate: Crate, player: Player, rewardLore: List<String>): List<Entry<Reward>> =
            PreviewRewardEntries.mappedEntries(crate, player, rewardLore)

        suspend fun create(
            player: Player,
            crate: Crate,
            crateHandle: CrateHandle?,
            settings: PreviewMenuSettings.Basic
        ): PreviewListMenu {
            val menu = PreviewListMenu(player, crate, crateHandle, mappedEntries(crate, player, settings.rewardLore), settings)
            menu.addButtons()
            menu.addRandomRewards()
            settings.anvilSettings?.applyTo(menu)
            return menu
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

    private suspend fun addRandomRewards() {
        PreviewRewardEntries.addRollingRandomRewards(
            menu = this,
            crate = crate,
            player = player,
            rewardLore = settings.rewardLore,
            randomRewardSlots = settings.randomRewardSlots,
            randomRewardSwitchTicks = settings.randomRewardSwitchTicks,
            randomRewardUnique = settings.randomRewardUnique,
        )
    }
}
