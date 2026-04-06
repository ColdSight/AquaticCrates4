package gg.aquatic.crates.interact

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.crates.Messages
import gg.aquatic.execute.Action

object DestroyCrateClickAction : Action<CrateClickBinder> {
    override val binder: Class<out CrateClickBinder> = CrateClickBinder::class.java
    override val arguments = emptyList<gg.aquatic.common.argument.ObjectArgument<*>>()

    override suspend fun execute(binder: CrateClickBinder, args: ArgumentContext<CrateClickBinder>) {
        if (binder.usingKeyMapping || !binder.player.hasPermission("aqcrates.admin")) {
            return
        }
        binder.crateHandle?.destroy() ?: return
        Messages.CRATE_DESTROYED.message().send(binder.player)
    }
}