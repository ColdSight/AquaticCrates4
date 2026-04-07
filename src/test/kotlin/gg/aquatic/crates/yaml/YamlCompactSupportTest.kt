package gg.aquatic.crates.yaml

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class YamlCompactSupportTest {
    private val yaml = Yaml.default

    @Test
    fun `encodeCompactString omits nulls and empty containers`() {
        val encoded = yaml.encodeCompactString(
            CompactExample.serializer(),
            CompactExample(
                required = "kept",
                nullable = null,
                nested = null,
                emptyList = emptyList(),
                emptyMap = emptyMap()
            )
        )

        val node = yaml.parseToYamlNode(encoded) as YamlMap

        assertEquals("kept", node.scalarValue("required"))
        assertFalse(node.hasKey("nullable"))
        assertFalse(node.hasKey("nested"))
        assertFalse(node.hasKey("empty-list"))
        assertFalse(node.hasKey("empty-map"))
    }

    @Test
    fun `mergeMissing only fills missing values and preserves custom values`() {
        val current = yaml.parseToYamlNode(
            """
            root:
              existing: custom
              nullable: null
              extra: keep
            """.trimIndent()
        )
        val defaults = yaml.parseToYamlNode(
            """
            root:
              existing: default
              nullable: restored
              missing: added
            """.trimIndent()
        )

        val merged = current.mergeMissing(defaults) as YamlMap
        val root = merged.mapValue("root")

        assertEquals("custom", root.scalarValue("existing"))
        assertEquals("restored", root.scalarValue("nullable"))
        assertEquals("added", root.scalarValue("missing"))
        assertEquals("keep", root.scalarValue("extra"))
    }

    @Test
    fun `parseCompactNode prunes null and empty sections from parsed yaml`() {
        val node = yaml.parseCompactNode(
            """
            root:
              keep: yes
              nullable: null
              empty-map: {}
              empty-list: []
            """.trimIndent()
        ) as YamlMap

        val root = node.mapValue("root")
        assertEquals("yes", root.scalarValue("keep"))
        assertFalse(root.hasKey("nullable"))
        assertFalse(root.hasKey("empty-map"))
        assertFalse(root.hasKey("empty-list"))
    }

    @Serializable
    private data class CompactExample(
        val required: String,
        val nullable: String? = null,
        val nested: Nested? = null,
        val emptyList: List<String> = emptyList(),
        val emptyMap: Map<String, String> = emptyMap(),
    )

    @Serializable
    private data class Nested(
        val value: String? = null,
    )

    private fun YamlMap.hasKey(key: String): Boolean {
        return entries.keys.any { it.content == key }
    }

    private fun YamlMap.scalarValue(key: String): String? {
        return (entries.entries.firstOrNull { it.key.content == key }?.value as? YamlScalar)?.content
    }

    private fun YamlMap.mapValue(key: String): YamlMap {
        return entries.entries.first { it.key.content == key }.value as YamlMap
    }
}
