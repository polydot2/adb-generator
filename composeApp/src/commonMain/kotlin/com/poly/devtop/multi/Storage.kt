// commonMain/kotlin/Storage.kt
import com.poly.devtop.data.Config
import com.poly.devtop.data.Tag


expect object Storage {
    suspend fun saveConfig(key: String, config: Config)
    suspend fun loadConfig(key: String): Config?
    suspend fun deleteConfig(key: String)
    suspend fun listConfigs(): List<String>

    suspend fun saveTag(key: String, tag: Tag)
    suspend fun loadTag(key: String): Tag?
    suspend fun deleteTag(key: String)
    suspend fun listTags(): List<String>
}