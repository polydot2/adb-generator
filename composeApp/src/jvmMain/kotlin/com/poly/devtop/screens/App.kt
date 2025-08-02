package com.poly.devtop.screens

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.poly.devtop.GitIntereactor
import com.poly.devtop.JiraInteractor
import java.io.File

// Map de configuration : étiquette JIRA -> chemin du dépôt
val jiraToRepoMap = mapOf(
    "ABC" to File("Projets/MyProject1"),
    "XYZ" to File("Projets/MyProject2")
)

@Composable
@Preview
fun App() {
    var jiraUrl by remember { mutableStateOf(TextFieldValue("")) }
    var resultMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Créer une branche Git à partir d'une URL JIRA", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = jiraUrl,
                onValueChange = { jiraUrl = it },
                label = { Text("URL JIRA (ex. https://jira.example.com/browse/ABC-123)") },
                modifier = Modifier.fillMaxWidth(),
                isError = isError
            )

            Button(
                onClick = {
                    makeMagic(jiraUrl.text, { message, error ->
                        resultMessage = message
                        isError = error
                    })
                },
                enabled = jiraUrl.text.isNotBlank()
            ) {
                Text("Créer la branche")
            }

            if (resultMessage.isNotEmpty()) {
                Text(
                    text = resultMessage,
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Fait l'appel à JIRA et créer la branche
fun makeMagic(jiraUrl: String, onResult: (String, Boolean) -> Unit) {
    try {
        val ticket = extractJiraTicket(jiraUrl)
        val branchName = GitIntereactor.createBranchName(ticket)

        val repoDir = getRepoDir(ticket, jiraToRepoMap)

        GitIntereactor.createGitBranch(repoDir, branchName)

        onResult("Branche '$branchName' créée dans ${repoDir.absolutePath}", false)
    } catch (e: Exception) {
        onResult("Erreur : ${e.message}", true)
    }
}

// Extrait le ticket JIRA (ex. "ABC-123") depuis l'URL
fun extractJiraTicket(url: String): String {
    return JiraInteractor.extractJiraTicket(url).fields.summary
}

// Récupère le répertoire du dépôt à partir de l'étiquette JIRA
fun getRepoDir(ticket: String, jiraToRepoMap: Map<String, File>): File {
    val projectKey = ticket.split("-")[0]
    val repoDir = jiraToRepoMap[projectKey]
        ?: throw IllegalArgumentException("Aucun dépôt configuré pour l'étiquette JIRA '$projectKey'")
    if (!repoDir.exists() || !repoDir.isDirectory) {
        throw IllegalArgumentException("Le répertoire ${repoDir.absolutePath} n'existe pas ou n'est pas un dossier")
    }
    return repoDir
}


