package com.poly.devtop

import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable
import java.util.regex.Pattern
import kotlinx.coroutines.runBlocking
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import java.io.File

// Configuration JIRA
data class JiraConfig(
    val baseUrl: String,
    val email: String,
    val apiToken: String
)

object JiraInteractor {
    // Représentation du ticket JIRA
    @Serializable
    data class JiraTicket(
        val key: String,
        val fields: JiraFields
    )

    @Serializable
    data class JiraFields(
        val summary: String
    )

    val defaultConfig = JiraConfig(
        baseUrl = "",
        email = "",
        apiToken = ""
    )

    // Extrait le ticket JIRA via l'API
    fun extractJiraTicket(url: String, config: JiraConfig = defaultConfig): JiraTicket = runBlocking {
        val pattern = Pattern.compile("([A-Z]+-[0-9]+)")
        val matcher = pattern.matcher(url)
        if (!matcher.find()) {
            throw IllegalArgumentException("URL JIRA invalide : aucun ticket trouvé (ex. ABC-123)")
        }
        val ticketId = matcher.group(1)

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        try {
            val response = client.get("${config.baseUrl}/rest/api/2/issue/$ticketId") {
                headers {
                    append(HttpHeaders.Authorization, "Basic ${encodeBase64("${config.email}:${config.apiToken}")}")
                    append(HttpHeaders.Accept, "application/json")
                }
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<JiraTicket>()
            } else {
                throw IllegalStateException("Erreur API JIRA : ${response.status}")
            }
        } finally {
            client.close()
        }
    }

    // Encode en Base64 pour l'authentification Basic
    fun encodeBase64(input: String): String {
        return java.util.Base64.getEncoder().encodeToString(input.toByteArray())
    }
}