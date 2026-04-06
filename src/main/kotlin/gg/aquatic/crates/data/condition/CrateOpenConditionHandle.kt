package gg.aquatic.crates.data.condition

import gg.aquatic.execute.ExecutableObjectHandle
import gg.aquatic.execute.condition.ConditionHandle

class CrateOpenConditionHandle(
    private val delegate: suspend (CrateOpenConditionBinder, (CrateOpenConditionBinder, String) -> String) -> Boolean,
) {

    suspend fun execute(
        binder: CrateOpenConditionBinder,
        textUpdater: (CrateOpenConditionBinder, String) -> String,
    ): Boolean {
        return delegate(binder, textUpdater)
    }

    companion object {
        fun fromPlayer(handle: ConditionHandle<org.bukkit.entity.Player>): CrateOpenConditionHandle {
            return CrateOpenConditionHandle { binder, textUpdater ->
                handle.execute(binder.player) { _, str -> textUpdater(binder, str) }
            }
        }

        fun fromHandle(handle: ExecutableObjectHandle<CrateOpenConditionBinder, Boolean>): CrateOpenConditionHandle {
            return CrateOpenConditionHandle { binder, textUpdater ->
                handle.execute(binder, textUpdater)
            }
        }
    }
}
