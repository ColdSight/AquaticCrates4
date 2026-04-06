package gg.aquatic.crates.message

import kotlinx.serialization.Serializable

@Serializable
data class MessagesFileData(
    val help: EditableMessageData = EditableMessageData.lines(
        "<#18788C><bold>|</bold> <#3EC4DE>AquaticCrates <white>Commands"
    ),
    val crateGiven: EditableMessageData = EditableMessageData.lines("<green>You have been given the crate!"),
    val keysSelfRequiresPlayer: EditableMessageData = EditableMessageData.lines("<red>You must be a player to give yourself keys!"),
    val keysGivenSelf: EditableMessageData = EditableMessageData.lines("<green>You have been given <yellow>%amount%x</yellow> <gold>%key_type%</gold> key!"),
    val keysGivenTarget: EditableMessageData = EditableMessageData.lines("<green>You have been given <yellow>%amount%x</yellow> <gold>%key_type%</gold> key!"),
    val keysGivenSender: EditableMessageData = EditableMessageData.lines("<green>You have given <yellow>%player%</yellow> <yellow>%amount%x</yellow> <gold>%key_type%</gold> key!"),
    val keyBank: EditableMessageData = EditableMessageData.lines("<#18788C><bold>|</bold> <yellow>%crate_name%</yellow><gray> (<white>%crate_id%</white>)<gray>: <aqua>%amount%</aqua>"),
    val keyBankEmpty: EditableMessageData = EditableMessageData.lines("<red>%player% has no virtual keys."),
    val noPermission: EditableMessageData = EditableMessageData.lines("<red>You do not have permission to do that."),
    val pluginReloading: EditableMessageData = EditableMessageData.lines("<yellow>Reloading..."),
    val pluginReloaded: EditableMessageData = EditableMessageData.lines("<green>Reloaded!"),
    val cratePlaced: EditableMessageData = EditableMessageData.lines("<green>You have placed the crate!"),
    val crateDestroyed: EditableMessageData = EditableMessageData.lines("<red>Crate destroyed!"),
    val crateSaved: EditableMessageData = EditableMessageData.lines("<green>Saved crate '<yellow>%crate_id%</yellow>'."),
    val crateCreatePrompt: EditableMessageData = EditableMessageData.lines("<aqua>Enter new crate ID:"),
    val crateInvalidId: EditableMessageData = EditableMessageData.lines("<red>Invalid crate ID. Use only letters, numbers, '_' or '-'."),
    val crateAlreadyExists: EditableMessageData = EditableMessageData.lines("<red>Crate '<yellow>%crate_id%</yellow>' already exists."),
    val crateEditorOpenFailed: EditableMessageData = EditableMessageData.lines("<red>Failed to open crate editor: <yellow>%reason%</yellow>"),
)
