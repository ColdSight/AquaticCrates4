package gg.aquatic.crates.data.editor

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlPath
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.KSerializer

private val rootYamlPath = YamlPath.root

private fun yamlFieldKey(name: String): String {
    return buildString(name.length + 4) {
        name.forEachIndexed { index, char ->
            if (char.isUpperCase()) {
                if (index > 0) append('-')
                append(char.lowercaseChar())
            } else {
                append(char)
            }
        }
    }
}

fun yamlScalar(value: Any?): YamlScalar = YamlScalar(value?.toString() ?: "", rootYamlPath)

fun yamlNull(): YamlNull = YamlNull(rootYamlPath)

fun yamlList(items: List<YamlNode>): YamlList = YamlList(items, rootYamlPath)

fun yamlMap(entries: Map<String, YamlNode>): YamlMap = YamlMap(
    entries.entries.associate { (key, value) -> yamlScalar(key) to value },
    rootYamlPath
)

fun <T> Yaml.encodeToNode(serializer: KSerializer<T>, value: T): YamlNode {
    return parseToYamlNode(encodeToString(serializer, value))
}

val YamlNode.stringContentOrNull: String?
    get() = (this as? YamlScalar)?.content

fun YamlNode.mapValue(key: String): YamlNode? {
    val map = this as? YamlMap ?: return null
    return map.get<YamlNode>(key) ?: map.get<YamlNode>(yamlFieldKey(key))
}

fun YamlNode.listValue(index: Int): YamlNode? = (this as? YamlList)?.items?.getOrNull(index)

fun YamlNode.findByPath(path: List<String>): YamlNode? {
    var current: YamlNode = this
    for (segment in path) {
        val index = segment.toIntOrNull()
        current = when (index) {
            null -> current.mapValue(segment) ?: return null
            else -> current.listValue(index) ?: return null
        }
    }
    return current
}

fun YamlNode.withMapValue(key: String, value: YamlNode): YamlNode {
    val currentMap = this as? YamlMap ?: yamlMap(emptyMap())
    val mutable = currentMap.entries.entries.associate { it.key.content to it.value }.toMutableMap()
    val existingKey = when {
        mutable.containsKey(key) -> key
        mutable.containsKey(yamlFieldKey(key)) -> yamlFieldKey(key)
        else -> yamlFieldKey(key)
    }
    mutable[existingKey] = value
    return yamlMap(mutable)
}
