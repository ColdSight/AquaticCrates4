package gg.aquatic.crates.interact

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.crates.crate.opening.CrateOpeningService
import gg.aquatic.crates.debug.CratesDebug
import gg.aquatic.execute.Action

object OpenCrateClickAction : Action<CrateClickBinder> {
    override val binder: Class<out CrateClickBinder> = CrateClickBinder::class.java
    override val arguments = emptyList<gg.aquatic.common.argument.ObjectArgument<*>>()

    override suspend fun execute(binder: CrateClickBinder, args: ArgumentContext<CrateClickBinder>) {
        val session = CrateOpeningService.reserveOpening(binder.player, binder.crate) ?: return
        CratesDebug.message(binder.player, 1, "Opening the crate!")
        if (CrateOpeningService.executeOpening(session, binder.crateHandle)) {
            CratesDebug.message(binder.player, 1, "Crate opened!")
        }
    }
}