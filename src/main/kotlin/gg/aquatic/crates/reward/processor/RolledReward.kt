package gg.aquatic.crates.reward.processor

import gg.aquatic.crates.reward.Reward
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.inventory.ItemStack

data class RolledReward(
    val reward: Reward,
    val amount: Int,
) {
    fun displayItem(
        selected: Boolean = false,
        hidden: Boolean = false,
        hiddenItem: ItemStack? = null,
        completed: Boolean = false,
    ): ItemStack {
        if (hidden) {
            val item = hiddenItem?.clone() ?: reward.previewItem().clone().apply {
                type = org.bukkit.Material.GRAY_STAINED_GLASS_PANE
            }

            val meta = item.itemMeta
            if (hiddenItem == null) {
                meta.displayName(Component.text("Hidden Reward", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
            }

            val lore = meta.lore()?.toMutableList() ?: mutableListOf()
            if (lore.isNotEmpty() && !completed) {
                lore += Component.empty()
            }

            if (selected) {
                lore += Component.text("Selected", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
                if (!completed) {
                    lore += Component.text("Click to unselect this hidden reward", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false)
                }
            } else if (!completed) {
                lore += Component.text("Click to choose this hidden reward", NamedTextColor.DARK_AQUA)
                    .decoration(TextDecoration.ITALIC, false)
            }

            meta.lore(lore)
            item.itemMeta = meta
            return item
        }

        val item = reward.previewItem().clone()
        val totalAmount = (item.amount.coerceAtLeast(1) * amount).coerceAtLeast(1)
        item.amount = totalAmount.coerceAtMost(item.maxStackSize.coerceAtLeast(1))

        val meta = item.itemMeta
        val lore = mutableListOf<Component>()
        lore += Component.text("Reward: ", NamedTextColor.DARK_AQUA)
            .append(reward.displayName.colorIfAbsent(NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false)
        lore += Component.text("Amount: $amount", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        lore += Component.text("Rarity: ", NamedTextColor.DARK_AQUA)
            .append(reward.rarity.displayName.colorIfAbsent(NamedTextColor.WHITE))
            .decoration(TextDecoration.ITALIC, false)
        if (selected) {
            lore += Component.text("Selected", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
        } else if (!completed) {
            lore += Component.text("Click to choose this reward", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
        }
        meta.lore(lore)
        item.itemMeta = meta
        return item
    }
}
