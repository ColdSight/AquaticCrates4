package gg.aquatic.crates.interact

import gg.aquatic.common.argument.ArgumentContext
import gg.aquatic.crates.debug.CratesDebug
import gg.aquatic.execute.Action

object PreviewCrateClickAction : Action<CrateClickBinder> {
    override val binder: Class<out CrateClickBinder> = CrateClickBinder::class.java
    override val arguments = emptyList<gg.aquatic.common.argument.ObjectArgument<*>>()

    override suspend fun execute(binder: CrateClickBinder, args: ArgumentContext<CrateClickBinder>) {
        val preview = binder.crate.preview ?: return
        preview.open(binder.player, binder.crate, binder.crateHandle)
        CratesDebug.message(binder.player, 1, "Crate preview opened!")
    }
}