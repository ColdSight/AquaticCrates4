package gg.aquatic.crates.await

import kotlinx.coroutines.Deferred

internal interface StartupDependencyAwaiter {
    fun isAvailable(): Boolean
    fun await(): Deferred<Unit>
}
