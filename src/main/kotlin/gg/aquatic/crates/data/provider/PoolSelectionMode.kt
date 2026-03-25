package gg.aquatic.crates.data.provider

enum class PoolSelectionMode(val id: String) {
    FIRST_MATCH("first-match"),
    MERGE_ALL("merge-all");

    companion object {
        val entries = listOf(FIRST_MATCH, MERGE_ALL)

        fun of(raw: String): PoolSelectionMode {
            return entries.firstOrNull { it.id.equals(raw, true) } ?: FIRST_MATCH
        }
    }
}
