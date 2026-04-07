package gg.aquatic.crates.yaml

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import gg.aquatic.crates.data.editor.encodeToNode
import kotlinx.serialization.KSerializer

fun <T> Yaml.encodeCompactString(serializer: KSerializer<T>, value: T): String {
    val compactNode = encodeToNode(serializer, value).pruneEmpty()
    return encodeToString(YamlNode.serializer(), compactNode)
}

fun Yaml.parseCompactNode(text: String): YamlNode {
    return parseToYamlNode(text).pruneEmpty()
}

private fun YamlNode.pruneEmpty(): YamlNode {
    return when (this) {
        is YamlNull -> this
        is YamlList -> {
            val items = items.mapNotNull { child ->
                child.pruneEmpty().takeUnless { it is YamlNull }
            }
            copy(items = items)
        }

        is YamlMap -> {
            val entries = entries.mapNotNull { (key, value) ->
                value.pruneEmpty()
                    .takeUnless { pruned -> pruned is YamlNull || pruned.isEmptyContainer() }
                    ?.let { key to it }
            }.toMap()
            copy(entries = entries)
        }

        else -> this
    }
}

private fun YamlNode.isEmptyContainer(): Boolean {
    return when (this) {
        is YamlList -> items.isEmpty()
        is YamlMap -> entries.isEmpty()
        else -> false
    }
}
