package gg.aquatic.crates.interact

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.execute.Action
import org.bukkit.entity.Player

class PlayerProxyCrateClickAction(
    private val delegate: Action<Player>,
) : Action<CrateClickBinder> {
    override val binder: Class<out CrateClickBinder> = CrateClickBinder::class.java
    override val arguments = delegate.arguments

    override suspend fun execute(binder: CrateClickBinder, args: ArgumentContext<CrateClickBinder>) {
        delegate.execute(
            binder.player,
            ArgumentContext(
                binder = binder.player,
                arguments = args.arguments,
                updater = { _, str -> args.updater(binder, str) }
            )
        )
    }
}