package gg.aquatic.crates.data.interactable

import gg.aquatic.clientside.serialize.ClientsideSettings
import kotlinx.serialization.Serializable

@Serializable
abstract class CrateInteractableData {
    abstract fun toSettings(): ClientsideSettings<*>
}
