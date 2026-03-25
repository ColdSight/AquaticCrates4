package gg.aquatic.crates.data.condition

import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder

fun TypedNestedSchemaBuilder<PlayerConditionData>.definePlayerConditionEditor() {
    fieldPattern(
        displayName = "Condition",
        adapter = PlayerConditionEntryFieldAdapter,
        description = listOf(
            "Left click to edit this condition.",
            "Right click to change its condition type."
        )
    )
    include<DateRangePlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("date-range") }) {
        with(DateRangePlayerConditionData) {
            defineEditor()
        }
    }
    include<BiomePlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("biome") }) {
        with(BiomePlayerConditionData) {
            defineEditor()
        }
    }
    include<DayOfMonthPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("day-of-month") }) {
        with(DayOfMonthPlayerConditionData) {
            defineEditor()
        }
    }
    include<DayOfWeekPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("day-of-week") }) {
        with(DayOfWeekPlayerConditionData) {
            defineEditor()
        }
    }
    include<HasEmptyInventorySlotPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("has-empty-inventory-slot") }) {
        with(HasEmptyInventorySlotPlayerConditionData) {
            defineEditor()
        }
    }
    include<MonthPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("month") }) {
        with(MonthPlayerConditionData) {
            defineEditor()
        }
    }
    include<OnlinePlayerCountPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("online-player-count") }) {
        with(OnlinePlayerCountPlayerConditionData) {
            defineEditor()
        }
    }
    include<PermissionPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("permission") }) {
        with(PermissionPlayerConditionData) {
            defineEditor()
        }
    }
    include<TimeRangePlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("time-range") }) {
        with(TimeRangePlayerConditionData) {
            defineEditor()
        }
    }
    include<WeekParityPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("week-parity") }) {
        with(WeekParityPlayerConditionData) {
            defineEditor()
        }
    }
    include<WeekOfYearModuloPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("week-of-year-mod") }) {
        with(WeekOfYearModuloPlayerConditionData) {
            defineEditor()
        }
    }
    include<WorldPlayerConditionData>(visibleWhen = { it.matchesConditionSubtype("world") }) {
        with(WorldPlayerConditionData) {
            defineEditor()
        }
    }
}
