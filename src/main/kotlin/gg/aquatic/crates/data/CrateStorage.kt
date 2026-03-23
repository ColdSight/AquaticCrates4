package gg.aquatic.crates.data

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import gg.aquatic.crates.CratesPlugin
import gg.aquatic.crates.crate.Crate
import java.io.File

object CrateStorage {

    private val yaml = CrateDataFormats.yaml

    private val cratesDirectory: File
        get() = File(CratesPlugin.dataFolder, "crates")

    fun ensureDefaults() {
        cratesDirectory.mkdirs()
        if (availableIds().isNotEmpty()) return

        save("test", CrateData.createDefault(displayName = "<yellow>Test Crate"))
    }

    fun availableIds(): List<String> {
        if (!cratesDirectory.exists()) return emptyList()

        return cratesDirectory.listFiles { file ->
            file.isFile && (
                file.extension.equals("yml", true) ||
                    file.extension.equals("yaml", true)
                )
        }
            ?.map { it.nameWithoutExtension }
            ?.sorted()
            ?.distinct()
            ?: emptyList()
    }

    fun loadData(id: String): CrateData {
        val yamlFile = yamlFileFor(id)
        if (!yamlFile.exists()) {
            return CrateData.createDefault()
        }

        val content = yamlFile.readText()

        return yaml.decodeFromString(CrateData.serializer(), content)
            .normalized(id, availableIds().toSet())
    }

    fun loadAllCrates(): Map<String, Crate> {
        ensureDefaults()
        return availableIds().associateWith { id ->
            loadData(id).toCrate(id)
        }
    }

    fun save(id: String, crateData: CrateData) {
        cratesDirectory.mkdirs()
        yamlFileFor(id).writeText(encodeCompactYaml(crateData.normalized(id, availableIds().toSet() + id)))
    }

    fun exists(id: String): Boolean {
        return yamlFileFor(id).exists() || yamlAltFileFor(id).exists()
    }

    fun delete(id: String): Boolean {
        val deletedPrimary = yamlFileFor(id).delete()
        val deletedAlt = yamlAltFileFor(id).delete()
        return deletedPrimary || deletedAlt
    }

    private fun yamlFileFor(id: String): File {
        return File(cratesDirectory, "$id.yml")
    }

    private fun yamlAltFileFor(id: String): File {
        return File(cratesDirectory, "$id.yaml")
    }

    private fun encodeCompactYaml(crateData: CrateData): String {
        val encoded = yaml.encodeToString(CrateData.serializer(), crateData)
        val compactNode = yaml.parseToYamlNode(encoded).pruneEmpty()
        return yaml.encodeToString(YamlNode.serializer(), compactNode)
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
}
