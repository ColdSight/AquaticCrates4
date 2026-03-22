package gg.aquatic.crates.data

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import gg.aquatic.crates.data.action.RewardActionFormats
import gg.aquatic.crates.data.condition.PlayerConditionFormats
import gg.aquatic.crates.data.hologram.CrateHologramLineFormats
import gg.aquatic.crates.data.interactable.CrateInteractableFormats
import gg.aquatic.crates.data.price.OpenPriceFormats
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

@OptIn(ExperimentalSerializationApi::class)
object CrateDataFormats {
    val module = SerializersModule {
        include(RewardActionFormats.module)
        include(PlayerConditionFormats.module)
        include(CrateHologramLineFormats.module)
        include(CrateInteractableFormats.module)
        include(OpenPriceFormats.module)
    }

    val json = Json {
        serializersModule = module
        classDiscriminator = "type"
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val yaml = Yaml(
        serializersModule = module,
        configuration = YamlConfiguration(
            encodeDefaults = true,
            yamlNamingStrategy = YamlNamingStrategy.KebabCase,
            polymorphismStyle = PolymorphismStyle.Property,
            polymorphismPropertyName = "type"
        )
    )
}
