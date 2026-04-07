package gg.aquatic.crates.crate.preview

import gg.aquatic.crates.crate.Crate
import gg.aquatic.crates.crate.CrateHandle
import gg.aquatic.crates.crate.preview.runtime.PreviewRewardEntries
import gg.aquatic.kmenu.privateMenu
import gg.aquatic.kmenu.menu.PrivateMenu
import gg.aquatic.replace.PlaceholderContext
import org.bukkit.entity.Player

class PreviewCustomPagesMenu private constructor(
    player: Player,
    private val crate: Crate,
    private val crateHandle: CrateHandle?,
    private val pageSettings: PreviewMenuSettings.Basic,
    private val allPages: List<PreviewMenuSettings.Basic>,
    private val entries: List<gg.aquatic.kmenu.menu.util.ListMenu.Entry<gg.aquatic.crates.reward.Reward>>,
    private val page: Int,
) : PrivateMenu(
    pageSettings.invSettings.title,
    pageSettings.invSettings.type,
    player,
    true
) {

    companion object {
        suspend fun create(
            player: Player,
            crate: Crate,
            crateHandle: CrateHandle?,
            settings: PreviewMenuSettings.CustomPages,
            page: Int,
        ): PreviewCustomPagesMenu {
            val pages = settings.pages.toList()
            val safePage = page.coerceIn(0, pages.lastIndex)
            return PreviewCustomPagesMenu(
                player = player,
                crate = crate,
                crateHandle = crateHandle,
                pageSettings = pages[safePage],
                allPages = pages,
                entries = PreviewListMenu.mappedEntries(crate, player, pages[safePage].rewardLore),
                page = safePage
            ).apply {
                addButtons()
                addRewards()
                addRandomRewards()
                pageSettings.anvilSettings?.applyTo(this)
            }
        }
    }

    private val context = PlaceholderContext.privateMenu()

    private suspend fun addButtons() {
        for ((id, comp) in pageSettings.invSettings.components) {
            when (id.lowercase()) {
                "next-page" -> {
                    addComponent(comp.create(context) { event ->
                        if (event.buttonType != gg.aquatic.kmenu.inventory.ButtonType.LEFT) return@create
                        if (page + 1 >= allPages.size) return@create
                        create(player, crate, crateHandle, PreviewMenuSettings.CustomPages(allPages), page + 1).open()
                    })
                }

                "prev-page" -> {
                    addComponent(comp.create(context) { event ->
                        if (event.buttonType != gg.aquatic.kmenu.inventory.ButtonType.LEFT) return@create
                        if (page <= 0) return@create
                        create(player, crate, crateHandle, PreviewMenuSettings.CustomPages(allPages), page - 1).open()
                    })
                }

                else -> addComponent(comp.create(context))
            }
        }
    }

    private suspend fun addRewards() {
        val lowerIndex = allPages
            .take(page)
            .sumOf { it.rewardSlots.size }

        pageSettings.rewardSlots.forEachIndexed { index, slot ->
            val entry = entries.getOrNull(lowerIndex + index) ?: return@forEachIndexed
            addComponent(entry.createButton(slot))
        }
    }

    private suspend fun addRandomRewards() {
        PreviewRewardEntries.addRollingRandomRewards(
            menu = this,
            crate = crate,
            player = player,
            rewardLore = pageSettings.rewardLore,
            randomRewardSlots = pageSettings.randomRewardSlots,
            randomRewardSwitchTicks = pageSettings.randomRewardSwitchTicks,
            randomRewardUnique = pageSettings.randomRewardUnique,
        )
    }
}
