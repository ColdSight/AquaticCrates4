package gg.aquatic.crates.data.price

import gg.aquatic.crates.open.OpenPriceHandle
import gg.aquatic.kurrency.impl.VaultCurrency
import gg.aquatic.waves.serialization.editor.meta.DoubleFieldAdapter
import gg.aquatic.waves.serialization.editor.meta.DoubleFieldConfig
import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bukkit.inventory.ItemStack
import java.math.BigDecimal

@Serializable
@SerialName("vault")
data class VaultOpenPriceData(
    val amount: Double = 100.0,
) : OpenPriceData() {
    override fun toHandle(crateId: String, keyItem: ItemStack): OpenPriceHandle {
        return OpenPriceHandle(
            currencyResolver = { VaultCurrency() },
            price = BigDecimal.valueOf(amount)
        )
    }

    companion object {
        fun TypedNestedSchemaBuilder<VaultOpenPriceData>.defineEditor() {
            field(
                VaultOpenPriceData::amount,
                DoubleFieldAdapter,
                DoubleFieldConfig(prompt = "Enter required vault amount:", min = 0.0),
                displayName = "Amount",
                description = listOf("How much Vault economy money must be paid.")
            )
        }
    }
}
