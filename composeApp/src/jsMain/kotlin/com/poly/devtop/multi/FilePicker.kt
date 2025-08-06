// jsMain/kotlin/FilePicker.kt
import kotlinx.browser.document
import kotlinx.coroutines.await
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.get
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual object FilePicker {
    actual suspend fun pickFile(accept: String): String? = suspendCoroutine { cont ->
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = accept // ".json" ou ".csv"
        input.onchange = {
            val file = input.files?.get(0)
            if (file != null) {
                val reader = js("new FileReader()")
                reader.onload = { event ->
                    val content = event.target.asDynamic().result as String
                    cont.resume(content)
                }
                reader.onerror = { cont.resume(null) }
                reader.readAsText(file)
            } else {
                cont.resume(null)
            }
        }
        input.click()
    }
}
