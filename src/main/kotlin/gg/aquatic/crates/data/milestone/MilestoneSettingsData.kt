package gg.aquatic.crates.data.milestone

import gg.aquatic.crates.data.CrateData
import gg.aquatic.crates.data.MilestoneData
import kotlinx.serialization.Serializable

@Serializable
data class MilestoneSettingsData(
    val milestones: List<MilestoneData> = emptyList(),
    val repeatableMilestones: List<MilestoneData> = emptyList(),
) {
    companion object {
        fun from(crateData: CrateData): MilestoneSettingsData {
            return MilestoneSettingsData(
                milestones = crateData.milestones,
                repeatableMilestones = crateData.repeatableMilestones
            )
        }
    }
}
