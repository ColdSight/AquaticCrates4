package gg.aquatic.crates.message

import gg.aquatic.crates.Messages
import gg.aquatic.waves.serialization.editor.meta.EditableModel
import gg.aquatic.waves.serialization.editor.meta.TypedEditorSchemaBuilder

object MessagesLocaleEditorSchema : EditableModel<MessagesFileData>(MessagesFileData.serializer()) {
    override fun TypedEditorSchemaBuilder<MessagesFileData>.define() {
        messageField(MessagesFileData::help, Messages.HELP)
        messageField(MessagesFileData::crateGiven, Messages.CRATE_GIVEN)
        messageField(MessagesFileData::keysSelfRequiresPlayer, Messages.KEYS_SELF_REQUIRES_PLAYER)
        messageField(MessagesFileData::keysGivenSelf, Messages.KEYS_GIVEN_SELF)
        messageField(MessagesFileData::keysGivenTarget, Messages.KEYS_GIVEN_TARGET)
        messageField(MessagesFileData::keysGivenSender, Messages.KEYS_GIVEN_SENDER)
        messageField(MessagesFileData::keyBank, Messages.KEY_BANK)
        messageField(MessagesFileData::keyBankEmpty, Messages.KEY_BANK_EMPTY)
        messageField(MessagesFileData::noPermission, Messages.NO_PERMISSION)
        messageField(MessagesFileData::pluginReloading, Messages.PLUGIN_RELOADING)
        messageField(MessagesFileData::pluginReloaded, Messages.PLUGIN_RELOADED)
        messageField(MessagesFileData::cratePlaced, Messages.CRATE_PLACED)
        messageField(MessagesFileData::crateDestroyed, Messages.CRATE_DESTROYED)
        messageField(MessagesFileData::crateSaved, Messages.CRATE_SAVED)
        messageField(MessagesFileData::crateCreatePrompt, Messages.CRATE_CREATE_PROMPT)
        messageField(MessagesFileData::crateInvalidId, Messages.CRATE_INVALID_ID)
        messageField(MessagesFileData::crateAlreadyExists, Messages.CRATE_ALREADY_EXISTS)
        messageField(MessagesFileData::crateEditorOpenFailed, Messages.CRATE_EDITOR_OPEN_FAILED)
    }
}
