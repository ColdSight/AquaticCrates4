package gg.aquatic.crates.limit

internal object LimitEvaluation {
    suspend fun canPass(limits: Collection<LimitHandle>, countProvider: suspend (LimitHandle) -> Long): Boolean {
        if (limits.isEmpty()) return true

        for (limit in limits) {
            if (countProvider(limit) >= limit.limit) {
                return false
            }
        }

        return true
    }
}
