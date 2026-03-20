package gg.aquatic.crates.data.action

import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder

fun TypedNestedSchemaBuilder<RewardActionData>.defineRewardActionEditor() {
    include<GiveItemRewardActionData>(visibleWhen = { it.matchesSubtype("give-item") }) {
        with(GiveItemRewardActionData) {
            defineEditor()
        }
    }
    include<MessageRewardActionData>(visibleWhen = { it.matchesSubtype("message") }) {
        with(MessageRewardActionData) {
            defineEditor()
        }
    }
    include<ActionbarRewardActionData>(visibleWhen = { it.matchesSubtype("actionbar") }) {
        with(ActionbarRewardActionData) {
            defineEditor()
        }
    }
    include<CommandRewardActionData>(visibleWhen = { it.matchesSubtype("command") }) {
        with(CommandRewardActionData) {
            defineEditor()
        }
    }
    include<SoundRewardActionData>(visibleWhen = { it.matchesSubtype("sound") }) {
        with(SoundRewardActionData) {
            defineEditor()
        }
    }
    include<StopSoundRewardActionData>(visibleWhen = { it.matchesSubtype("stop-sound") }) {
        with(StopSoundRewardActionData) {
            defineEditor()
        }
    }
    include<TitleRewardActionData>(visibleWhen = { it.matchesSubtype("title") }) {
        with(TitleRewardActionData) {
            defineEditor()
        }
    }
    include<CloseInventoryRewardActionData>(visibleWhen = { it.matchesSubtype("close-inventory") }) {
        with(CloseInventoryRewardActionData) {
            defineEditor()
        }
    }
}
