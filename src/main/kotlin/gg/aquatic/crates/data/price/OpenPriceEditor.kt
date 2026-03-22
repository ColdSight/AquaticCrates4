package gg.aquatic.crates.data.price

import gg.aquatic.waves.serialization.editor.meta.TypedNestedSchemaBuilder

fun TypedNestedSchemaBuilder<OpenPriceData>.defineOpenPriceEditor() {
    include<CrateKeyOpenPriceData>(visibleWhen = { it.matchesSubtype("crate-key") }) {
        with(CrateKeyOpenPriceData) {
            defineEditor()
        }
    }
    include<VaultOpenPriceData>(visibleWhen = { it.matchesSubtype("vault") }) {
        with(VaultOpenPriceData) {
            defineEditor()
        }
    }
}
