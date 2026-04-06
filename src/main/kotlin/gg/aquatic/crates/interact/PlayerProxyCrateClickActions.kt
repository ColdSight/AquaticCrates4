package gg.aquatic.crates.interact

import gg.aquatic.execute.action.impl.ActionbarAction
import gg.aquatic.execute.action.impl.CloseInventory
import gg.aquatic.execute.action.impl.CommandAction
import gg.aquatic.execute.action.impl.SoundAction
import gg.aquatic.execute.action.impl.SoundStopAction
import gg.aquatic.execute.action.impl.TitleAction
import gg.aquatic.waves.util.action.MessageAction

object PlayerProxyCrateClickActions {
    val message = PlayerProxyCrateClickAction(MessageAction)
    val actionbar = PlayerProxyCrateClickAction(ActionbarAction)
    val command = PlayerProxyCrateClickAction(CommandAction)
    val sound = PlayerProxyCrateClickAction(SoundAction)
    val stopSound = PlayerProxyCrateClickAction(SoundStopAction)
    val title = PlayerProxyCrateClickAction(TitleAction)
    val closeInventory = PlayerProxyCrateClickAction(CloseInventory)
}
