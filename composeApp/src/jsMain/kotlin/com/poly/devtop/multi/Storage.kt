// jsMain/kotlin/Storage.kt
import com.poly.devtop.data.Config
import com.poly.devtop.data.Tag
import kotlinx.browser.window
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable


actual object Storage {
    actual suspend fun saveConfig(key: String, config: Config) {
        val json = Json { prettyPrint = true }.encodeToString<Config>(config)
        window.localStorage.setItem("config:$key", json)
    }

    actual suspend fun loadConfig(key: String): Config? {
        val json = window.localStorage.getItem("config:$key") ?: return null
        return Json.decodeFromString(json)
    }

    actual suspend fun deleteConfig(key: String) {
        window.localStorage.removeItem("config:$key")
    }

    actual suspend fun listConfigs(): List<String> {
        val keys = mutableListOf<String>()
        for (i in 0 until window.localStorage.length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.startsWith("config:")) {
                keys.add(key.removePrefix("config:"))
            }
        }
        return keys
    }

    actual suspend fun saveTag(key: String, tag: Tag) {
        val json = Json.encodeToString<Tag>(tag)
        window.localStorage.setItem("tag:$key", json)
    }

    actual suspend fun loadTag(key: String): Tag? {
        val json = window.localStorage.getItem("tag:$key") ?: return null
        return Json.decodeFromString(json)
    }

    actual suspend fun deleteTag(key: String) {
        window.localStorage.removeItem("tag:$key")
    }

    actual suspend fun listTags(): List<String> {
        val keys = mutableListOf<String>()
        for (i in 0 until window.localStorage.length) {
            val key = window.localStorage.key(i) ?: continue
            if (key.startsWith("tag:")) {
                keys.add(key.removePrefix("tag:"))
            }
        }
        return keys
    }
}