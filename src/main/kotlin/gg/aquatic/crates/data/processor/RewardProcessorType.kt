package gg.aquatic.crates.data.processor

enum class RewardProcessorType(val id: String) {
    BASIC("basic"),
    CHOOSE("choose");

    companion object {
        val entries = listOf(BASIC, CHOOSE)

        fun of(raw: String): RewardProcessorType {
            return entries.firstOrNull { it.id.equals(raw, true) } ?: BASIC
        }
    }
}
