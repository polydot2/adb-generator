// jvmMain/kotlin/Storage.kt
import com.poly.devtop.data.Config
import com.poly.devtop.data.Tag
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual object Storage {
    private val resourcesDir: File by lazy {
        if (isRunningInDistribution()) {
            File(System.getProperty("user.dir")).parentFile
        } else {
            File(System.getProperty("user.dir")).resolve("app/resources")
        }
    }

    private val configDir: File by lazy {
        // Vérifier si le dossier common/ existe (mode développement)
        val commonDir = File(resourcesDir, "common")
        if (commonDir.exists() && commonDir.isDirectory) {
            File(commonDir, "configs")
        } else {
            File(resourcesDir, "configs")
        }
    }

    private val tagDir: File by lazy {
        // Vérifier si le dossier common/ existe (mode développement)
        val commonDir = File(resourcesDir, "common")
        if (commonDir.exists() && commonDir.isDirectory) {
            File(commonDir, "tags")
        } else {
            File(resourcesDir, "tags")
        }
    }

    init {
        // Créer les dossiers s'ils n'existent pas
        configDir.mkdirs()
        tagDir.mkdirs()
    }

    // Détecter si l'application est en mode distribution
    private fun isRunningInDistribution(): Boolean {
        // Vérifier si user.dir contient "bin", typique des distributions
        val userDir = System.getProperty("user.dir")
        return File(userDir).name == "bin"
    }

    actual suspend fun saveConfig(key: String, config: Config) {
        val json = Json { prettyPrint = true }.encodeToString(config)
        File(configDir, "$key.json").writeText(json)
    }

    actual suspend fun loadConfig(key: String): Config? {
        val file = File(configDir, "$key.json")
        return if (file.exists()) Json.decodeFromString(file.readText()) else null
    }

    actual suspend fun deleteConfig(key: String) {
        File(configDir, "$key.json").delete()
    }

    actual suspend fun listConfigs(): List<String> {
        return configDir.listFiles { _, name -> name.endsWith(".json") }?.map { it.nameWithoutExtension } ?: emptyList()
    }

    actual suspend fun saveTag(key: String, tag: Tag) {
        val json = Json.encodeToString(tag)
        File(tagDir, "$key.json").writeText(json)
    }

    actual suspend fun loadTag(key: String): Tag? {
        val file = File(tagDir, "$key.json")
        return if (file.exists()) Json.decodeFromString(file.readText()) else null
    }

    actual suspend fun deleteTag(key: String) {
        File(tagDir, "$key.json").delete()
    }

    actual suspend fun listTags(): List<String> {
        return tagDir.listFiles { _, name -> name.endsWith(".json") }?.map { it.nameWithoutExtension } ?: emptyList()
    }
}