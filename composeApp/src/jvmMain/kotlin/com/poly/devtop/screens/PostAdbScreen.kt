package com.poly.devtop.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.poly.devtop.component.Tag
import com.poly.devtop.component.TagManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*

enum class ParamType(val flag: String) {
    STRING("--es"), BOOLEAN("--ez"), INT("--ei"), ARRAY_STRING("--es"), JSON("--es")
}

@Serializable
data class Param(val key: String, val value: String, val isVisible: Boolean, val type: ParamType)

@Serializable
data class JsonParam(val key: String, val value: String, val type: String, val isVisible: Boolean = true)

@Serializable
data class Config(val intentName: String, val prefix: String, val params: List<JsonParam>)

@Serializable
data class JsonImport(val key: String, val value: String? = null)


@Composable
fun PostAdbScreen() {
    var intentName by remember { mutableStateOf(TextFieldValue("")) }
    var prefix by remember { mutableStateOf(TextFieldValue("")) }
    var configName by remember { mutableStateOf(TextFieldValue("")) }
    var params by remember { mutableStateOf(listOf(Param("", "", true, ParamType.STRING))) }
    var adbCommand by remember { mutableStateOf("") }
    var showJsonDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }
    var jsonInput by remember { mutableStateOf(TextFieldValue("")) }
    var jsonError by remember { mutableStateOf("") }
    var configError by remember { mutableStateOf("") }
    var currentConfigFile by remember { mutableStateOf<File?>(null) }
    var isModified by remember { mutableStateOf(false) }
    var configFiles by remember { mutableStateOf(emptyList<File>()) } // Liste des fichiers pour rafraîchissement
    val scrollState = rememberScrollState()
    var showCsvDialog by remember { mutableStateOf(false) }
    var csvInput by remember { mutableStateOf(TextFieldValue("")) }
    var csvError by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var logCatListening by remember { mutableStateOf(false) } // État pour suivre si la coroutine tourne
    var resultFromApk by remember { mutableStateOf("") }

    // Dans PostAdbScreen
    var tags by remember { mutableStateOf(listOf<Tag>()) }

    // Dossier pour sauvegarder les configurations
    val configDir = File("configs").apply { mkdirs() }

    // Charge les tags au démarrage
    LaunchedEffect(Unit) {
        val tagDir = File("tags").apply { mkdirs() }
        tags = tagDir.listFiles { _, name -> name.endsWith(".json") }?.map { file ->
            Json.decodeFromString<Tag>(file.readText())
        }?.toList() ?: emptyList()

        // load configFile at start
        configFiles = configDir.listFiles { _, name -> name.endsWith(".json") }?.toList() ?: emptyList()
    }

    // Mettre à jour la liste des fichiers à l'ouverture de la popup
    LaunchedEffect(showConfigDialog) {
        if (showConfigDialog) {
            configFiles = configDir.listFiles { _, name -> name.endsWith(".json") }?.toList() ?: emptyList()
        }
    }

    // Générer la commande à la volée
    LaunchedEffect(intentName, prefix, params, configName) {
        adbCommand = generateAdbCommand(
            intentName.text,
            prefix.text,
            params.filter { it.isVisible && it.key.isNotBlank() })
    }

    // Vérifier si la configuration a été modifiée
    LaunchedEffect(intentName, prefix, params, configName) {
        if (currentConfigFile != null && configName.text == currentConfigFile?.nameWithoutExtension) {
            try {
                val savedConfig = Json.decodeFromString<Config>(currentConfigFile?.readText() ?: "")
                val currentConfig = Config(
                    intentName = intentName.text,
                    prefix = prefix.text,
                    params = params.map { JsonParam(it.key, it.value, it.type.name, it.isVisible) }
                )
                isModified = savedConfig != currentConfig
            } catch (e: Exception) {
                isModified = false
            }
        } else {
            isModified = false
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier.padding(vertical = 4.dp),
            contentAlignment = Alignment.Center // Centre le contenu principal
        ) {
            Row {
                // tag
                Column(Modifier.weight(0.1f).padding(horizontal = 4.dp)) {
                    TagManager(
                        tags = tags,
                        configFiles = configFiles, // Passe la liste existante des fichiers de configuration
                        onCreateTag = { name ->
                            tags = tags + Tag(name)
                            File("tags/$name.json").writeText(Json.encodeToString(Tag(name)))
                        },
                        onEditTag = { tag, newName ->
                            tags = tags.map { if (it == tag) Tag(newName, tag.configPaths) else it }
                            File("tags/${tag.name}.json").delete()
                            File("tags/$newName.json").writeText(Json.encodeToString(Tag(newName, tag.configPaths)))
                        },
                        onDeleteTag = { tag ->
                            tags = tags - tag
                            File("tags/${tag.name}.json").delete()
                        },
                        onMoveConfigToTag = { configFile, targetTag ->
                            tags = tags.map { tag ->
                                if (tag == targetTag) {
                                    // Ajoute la config au tag cible (si targetTag n'est pas vide)
                                    Tag(
                                        tag.name,
                                        if (targetTag.name.isNotEmpty()) (tag.configPaths + configFile.path).distinct() else tag.configPaths
                                    )
                                } else {
                                    // Retire la config des autres tags
                                    Tag(tag.name, tag.configPaths.filter { it != configFile.path })
                                }
                            }
                            // Sauvegarde tous les tags
                            tags.forEach { File("tags/${it.name}.json").writeText(Json.encodeToString(it)) }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // screen
                Column(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .verticalScroll(scrollState)
                        .weight(0.6f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.align(Alignment.Start),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Générateur de commande ADB", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                installApk()
                            }
                        ) {
                            Text("Install middleMan.apk")
                        }
                    }


                    // Boutons pour sauvegarder et charger les configurations
                    Column(
                        Modifier.background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ).padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.Start),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Configuration", style = MaterialTheme.typography.titleLarge)

                            Button(
                                onClick = {
                                    if (configName.text.isNotBlank()) {
                                        try {
                                            val config = Config(
                                                intentName = intentName.text,
                                                prefix = prefix.text,
                                                params = params.map {
                                                    JsonParam(
                                                        it.key,
                                                        it.value,
                                                        it.type.name,
                                                        it.isVisible
                                                    )
                                                }
                                            )
                                            val json = Json { prettyPrint = true }.encodeToString(config)
                                            val file = File(configDir, "${configName.text}.json")
                                            file.writeText(json)
                                            currentConfigFile = file
                                            configError = ""
                                            isModified = false
                                            configFiles =
                                                configDir.listFiles { _, name -> name.endsWith(".json") }?.toList()
                                                    ?: emptyList()
                                        } catch (e: Exception) {
                                            configError = "Erreur lors de la sauvegarde : ${e.message}"
                                        }
                                    } else {
                                        configError = "Le nom de la configuration ne peut pas être vide"
                                    }
                                },
                                enabled = configName.text.isNotBlank()
                            ) {
                                Text("Sauvegarder")
                            }
                            Button(
                                onClick = { showConfigDialog = true }
                            ) {
                                Text("Charger")
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = {
                                    configName = TextFieldValue("")
                                    intentName = TextFieldValue("android.intent.action.VIEW")
                                    prefix = TextFieldValue("")
                                    params = listOf()
                                    adbCommand = ""
                                    currentConfigFile = null
                                    isModified = false
                                    configError = ""
                                    resultFromApk = ""
                                    logCatListening = false
                                }
                            ) {
                                Text("Clear")
                            }
                        }

                        // Champ pour le nom de la configuration avec icône de modification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = configName,
                                singleLine = true,
                                onValueChange = { configName = it },
                                label = { Text("Nom de la configuration") },
                                modifier = Modifier.weight(1f)
                            )
                            if (isModified) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Configuration modifiée",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (configError.isNotEmpty()) {
                            Text(configError, color = MaterialTheme.colorScheme.error)
                        }
                    }


                    Column(
                        Modifier.background(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Intent", style = MaterialTheme.typography.titleLarge)

                        // Champ pour le nom de l'intent
                        OutlinedTextField(
                            value = intentName,
                            singleLine = true,
                            onValueChange = { intentName = it },
                            label = { Text("Nom de l'intent (ex. android.intent.action.VIEW)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Champ pour le préfixe des paramètres
                        OutlinedTextField(
                            value = prefix,
                            singleLine = true,
                            onValueChange = { prefix = it },
                            label = { Text("Préfixe des paramètres (ex. extra_)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Paramètres clé-valeur
                    Row(
                        modifier = Modifier.align(Alignment.Start),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paramètres", style = MaterialTheme.typography.titleLarge)

                        // Bouton pour ajouter un paramètre
                        Button(
                            modifier = Modifier.focusProperties { canFocus = false },
                            onClick = { params = params + Param("", "", true, ParamType.STRING) },
                        ) {
                            Text("Ajouter paramètre")
                        }

                        // Bouton pour ouvrir la popup d'importation JSON
//                    Button(
//                        modifier = Modifier.focusProperties { canFocus = false },
//                        onClick = { showJsonDialog = true },
//                    ) {
//                        Text("Importer JSON")
//                    }

                        // Bouton pour ouvrir la popup d'importation CSV
                        Button(
                            modifier = Modifier.focusProperties { canFocus = false },
                            onClick = { showCsvDialog = true },
                        ) {
                            Text("Importer CSV")
                        }
                    }

                    params.forEachIndexed { index, param ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Bouton visible/caché
                            IconButton(
                                modifier = Modifier.focusProperties { canFocus = false },
                                onClick = {
                                    params = params.toMutableList().apply {
                                        this[index] = param.copy(isVisible = !param.isVisible)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (param.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (param.isVisible) "Cacher" else "Afficher"
                                )
                            }

                            // Champ pour la clé
                            OutlinedTextField(
                                value = param.key,
                                singleLine = true,
                                onValueChange = { newKey ->
                                    params = params.toMutableList().apply { this[index] = param.copy(key = newKey) }
                                },
                                label = { Text("Clé") },
                                modifier = Modifier.weight(1f),
                                enabled = param.isVisible
                            )

                            // Champ pour la valeur
                            OutlinedTextField(
                                value = param.value,
                                singleLine = true,
                                onValueChange = { newValue ->
                                    val newType = detectParamType(newValue)
                                    params = params.toMutableList().apply {
                                        this[index] = param.copy(value = newValue, type = newType)
                                    }
                                },
                                label = { Text("Valeur") },
                                modifier = Modifier.weight(1f),
                                enabled = param.isVisible
                            )

                            // Menu déroulant pour le type
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                OutlinedTextField(
                                    value = param.type.name,
                                    singleLine = true,
                                    onValueChange = {},
                                    label = { Text("Type") },
                                    modifier = Modifier.width(120.dp).focusProperties { canFocus = false },
                                    enabled = param.isVisible,
                                    readOnly = true
                                )
                                DropdownMenu(
                                    modifier = Modifier.focusProperties { canFocus = false },
                                    expanded = expanded && param.isVisible,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    ParamType.entries.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type.name) },
                                            onClick = {
                                                params = params.toMutableList().apply {
                                                    this[index] = param.copy(type = type)
                                                }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier.focusProperties { canFocus = false }
                                        .matchParentSize()
                                        .clickable(enabled = param.isVisible) { expanded = true }
                                )
                            }

                            // Bouton supprimer
                            IconButton(
                                modifier = Modifier.focusProperties { canFocus = false },
                                onClick = {
                                    params = params.toMutableList().apply { removeAt(index) }
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }

                    // Bouton pour générer la commande ADB
//                Button(
//                    onClick = {
//                        adbCommand = generateAdbCommand(
//                            intentName.text,
//                            prefix.text,
//                            params.filter { it.isVisible && it.key.isNotBlank() })
//                    },
//                    enabled = intentName.text.isNotBlank()
//                ) {
//                    Text("Générer commande ADB")
//                }

                    // Afficher la commande ADB
                    if (adbCommand.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$adbCommand",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                    clipboard.setContents(StringSelection(adbCommand), null)
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copier")
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        adbCommand = generateAdbCommand(
                                            intentName.text,
                                            prefix.text,
                                            params.filter { it.isVisible && it.key.isNotBlank() })

                                        // execute
                                        val output = executeCommand(adbCommand)
                                        toastMessage = output
                                        showToast = true
                                        delay(4000) // Afficher le Toast pendant 2 secondes
                                        showToast = false
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Lancer")
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        adbCommand = generateAdbCommand(
                                            intentName.text,
                                            prefix.text,
                                            params.filter { it.isVisible && it.key.isNotBlank() })

                                        // execute
                                        resultFromApk = ""
                                        val output = executeCommandWithMiddleAPK(adbCommand, intentName.text)
                                        executeCommand(output.first)
//                                        toastMessage = output.first
//                                        showToast = true
//                                        delay(4000) // Afficher le Toast pendant 2 secondes
//                                        showToast = false

                                        // Écouter les logs avec adb logcat
                                        logCatListening = true
                                        scope.launch(Dispatchers.IO) {
                                            val logcatResult = listenLogcat(output.second) // listen with UID
                                            withContext(Dispatchers.Main) {
                                                println("Fin de l'écoute des logs")
                                                logCatListening = false
                                                resultFromApk = logcatResult
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.PlayForWork, contentDescription = "Lancer a travers middleMan")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier.background(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp)
                            ).padding(16.dp).fillMaxWidth()
                        ) {
                            Row {
                                if (logCatListening) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(Modifier.width(12.dp))
                                }
                                Text(
                                    if (logCatListening) "En attente de réponse" else "Reponse",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Text(resultFromApk)
                        }
                    }
                }
            }

            // Toast personnalisé
            if (showToast) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter) // Positionne le Toast en bas
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(8.dp),
                ) {
                    Text(
                        text = toastMessage,
                        color = Color.White
                    )
                }
            }

            // Popup pour importer le JSON
            if (showJsonDialog) {
                AlertDialog(
                    onDismissRequest = { showJsonDialog = false },
                    title = { Text("Importer des paramètres JSON") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = jsonInput,
                                onValueChange = { jsonInput = it },
                                label = { Text("Coller le JSON ici") },
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                isError = jsonError.isNotEmpty()
                            )
                            if (jsonError.isNotEmpty()) {
                                Text(jsonError, color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = {
                                    val fileDialog =
                                        FileDialog(null as Frame?, "Choisir un fichier JSON", FileDialog.LOAD)
                                    fileDialog.file = "*.json"
                                    fileDialog.isVisible = true
                                    val file = fileDialog.files.firstOrNull()
                                    if (file != null) {
                                        try {
                                            jsonInput = TextFieldValue(file.readText())
                                            jsonError = ""
                                        } catch (e: Exception) {
                                            jsonError = "Erreur lors de la lecture du fichier : ${e.message}"
                                        }
                                    }
                                }
                            ) {
                                Text("Choisir un fichier JSON")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    val jsonParams = Json.decodeFromString<List<JsonImport>>(jsonInput.text)
                                    params = jsonParams.map { jsonParam ->
                                        val type = try {
                                            jsonParam.value?.let {
                                                detectParamType(it)
                                            } ?: ParamType.STRING
                                        } catch (e: IllegalArgumentException) {
                                            ParamType.STRING
                                        }
                                        Param(jsonParam.key, jsonParam.value ?: "", true, type)
                                    }
                                    jsonError = ""
                                    showJsonDialog = false
                                    currentConfigFile = null
                                    configName = TextFieldValue("")
                                    isModified = false
                                } catch (e: Exception) {
                                    jsonError = "JSON invalide : ${e.message}"
                                }
                            },
                            enabled = jsonInput.text.isNotBlank()
                        ) {
                            Text("Importer")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showJsonDialog = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }

            // Popup pour importer le CSV
            if (showCsvDialog) {
                AlertDialog(
                    onDismissRequest = { showCsvDialog = false },
                    title = { Text("Importer des paramètres CSV") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = csvInput,
                                onValueChange = { csvInput = it },
                                label = { Text("Coller le CSV ici (format: key,value)") },
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                isError = csvError.isNotEmpty()
                            )
                            if (csvError.isNotEmpty()) {
                                Text(csvError, color = MaterialTheme.colorScheme.error)
                            }
                            Button(
                                onClick = {
                                    val fileDialog =
                                        FileDialog(null as Frame?, "Choisir un fichier CSV", FileDialog.LOAD)
                                    fileDialog.file = "*.csv"
                                    fileDialog.isVisible = true
                                    val file = fileDialog.files.firstOrNull()
                                    if (file != null) {
                                        try {
                                            csvInput = TextFieldValue(file.readText())
                                            csvError = ""
                                        } catch (e: Exception) {
                                            csvError = "Erreur lors de la lecture du fichier : ${e.message}"
                                        }
                                    }
                                }
                            ) {
                                Text("Choisir un fichier CSV")
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    val csvParams = parseCsv(csvInput.text)
                                    params = csvParams.map { csvParam ->
                                        val value = csvParam.second ?: ""
                                        Param(csvParam.first, value, true, detectParamType(value))
                                    }
                                    csvError = ""
                                    showCsvDialog = false
                                    currentConfigFile = null
                                    configName = TextFieldValue("")
                                    isModified = false
                                } catch (e: Exception) {
                                    csvError = "CSV invalide : ${e.message}"
                                }
                            },
                            enabled = csvInput.text.isNotBlank()
                        ) {
                            Text("Importer")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showCsvDialog = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }

            // Popup pour charger une configuration
            if (showConfigDialog) {
                AlertDialog(
                    onDismissRequest = { showConfigDialog = false },
                    title = { Text("Charger une configuration") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(200.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (configFiles.isEmpty()) {
                                Text("Aucune configuration trouvée", color = MaterialTheme.colorScheme.onSurface)
                            } else {
                                configFiles.forEach { file ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = {
                                                try {
                                                    val config = Json.decodeFromString<Config>(file.readText())
                                                    intentName = TextFieldValue(config.intentName)
                                                    prefix = TextFieldValue(config.prefix)
                                                    params = config.params.map { jsonParam ->
                                                        val type = try {
                                                            ParamType.valueOf(jsonParam.type.uppercase())
                                                        } catch (e: IllegalArgumentException) {
                                                            ParamType.STRING
                                                        }
                                                        Param(jsonParam.key, jsonParam.value, jsonParam.isVisible, type)
                                                    }
                                                    configName = TextFieldValue(file.nameWithoutExtension)
                                                    currentConfigFile = file
                                                    configError = ""
                                                    isModified = false
                                                    showConfigDialog = false
                                                    resultFromApk = ""
                                                    logCatListening = false
                                                } catch (e: Exception) {
                                                    configError =
                                                        "Erreur lors du chargement de ${file.name} : ${e.message}"
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(file.nameWithoutExtension)
                                        }
                                        IconButton(
                                            onClick = {
                                                fileToDelete = file
                                                showDeleteConfirmDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Supprimer ${file.name}")
                                        }
                                    }
                                }
                            }
                            if (configError.isNotEmpty()) {
                                Text(configError, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        Button(onClick = { showConfigDialog = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }

            // Popup de confirmation pour la suppression
            if (showDeleteConfirmDialog && fileToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Confirmer la suppression") },
                    text = { Text("Voulez-vous vraiment supprimer la configuration ${fileToDelete?.nameWithoutExtension}?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    fileToDelete?.delete()
                                    configFiles =
                                        configDir.listFiles { _, name -> name.endsWith(".json") }?.toList()
                                            ?: emptyList()
                                    configError = ""
                                    if (currentConfigFile == fileToDelete) {
                                        currentConfigFile = null
                                        configName = TextFieldValue("")
                                        isModified = false
                                    }
                                    showDeleteConfirmDialog = false
                                } catch (e: Exception) {
                                    configError =
                                        "Erreur lors de la suppression de ${fileToDelete?.name} : ${e.message}"
                                }
                            }
                        ) {
                            Text("Supprimer")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { showDeleteConfirmDialog = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }
        }
    }
}

// Parse un CSV en liste de paires (key, value)
fun parseCsv(csv: String): List<Pair<String, String?>> {
    return csv.lines()
        .filter { it.isNotBlank() }
        .map { line ->
            val parts = line.split(",", limit = 2)
            val key = parts[0].trim()
            val value = if (parts.size > 1) parts[1].trim().takeIf { it.isNotEmpty() } else null
            key to value
        }
        .filter { it.first.isNotBlank() }
}

// Détecte automatiquement le type de la valeur
fun detectParamType(value: String): ParamType {
    return when {
        value.matches("^(true|false)$".toRegex()) -> ParamType.BOOLEAN
        value.matches("^-?\\d+$".toRegex()) -> ParamType.INT
        value.firstOrNull()?.equals('{') == true -> ParamType.JSON
        value.contains(",") -> ParamType.ARRAY_STRING
        else -> ParamType.STRING
    }
}

// Génère une commande ADB à partir du nom de l'intent, du préfixe et des paramètres
fun generateAdbCommand(intentName: String, prefix: String, params: List<Param>): String {
    if (intentName.isBlank()) return ""

    var filtered = params.filter { it.value.isNotBlank() }

    val paramString = filtered.joinToString(" ") { param ->
        param.value.trim().takeIf { it.isNotBlank() }.let {
            buildString {
                append(param.type.flag)
                append(" ")
                append(
                    (prefix.toString() + param.key.toString()).encodeAdbParameter()
                        .wrap()
                )
                append(" ")
                append(param.value.toString().escapeValue(param.type).encodeAdbParameter().wrap(param.type))
            }.toString().trim().trimIndent()
        }
    }.takeIf { it.isNotEmpty() } ?: ""

    return "adb shell am start -a ${intentName.wrap()} $paramString".trim()
}

private fun String.escapeValue(type: ParamType? = null): String {
    return if (type == ParamType.JSON) {
        this.replace("\"", "\\\"")
    } else
        this
}

private fun String.wrap(type: ParamType? = null): String {
    return if (type == null || type == ParamType.STRING || type == ParamType.ARRAY_STRING) {
        "\"$this\""
    } else if (type == ParamType.JSON) {
        "\'$this\'"
    } else this
}

// Encode les paramètres pour la commande ADB
fun String.encodeAdbParameter(): String {
    return this.replace('"', '\"')
}

fun installApk() {
    val workingDir = File("scrcpy-v1.25/")
    val apkName = "middleman-debug.apk"
    val commande = "adb install apk/$apkName"

    executeCommand(commande)
}

fun executeCommandWithMiddleAPK(command: String, intentName: String): Pair<String, String> {
    val uid = UUID.randomUUID().toString()

    val target = "--es \"targetIntent\" \"$intentName\""
    val tag = "--es \"uidResult\" \"$uid\""
    var newComand = command.replace(intentName, "com.poly.intent.middleman") + " " + target + " " + tag

    executeCommand("adb shell am force-stop com.poly.middleman") // close l'activity avant..

    return Pair(newComand, uid)
}

fun executeCommand(command: String): String {
    return try {
        val workingDir = File("scrcpy-v1.25/")
        val adbPath = "${workingDir.absolutePath}/adb.exe"

        // use local adb
        var localCommand = command.replace("adb", adbPath)
        localCommand = localCommand.replace("apk/", "${workingDir.absolutePath}/../apk/")
        var commandSplit = localCommand.split(" ")

        println("Execute : ${localCommand}")

        // Créer un ProcessBuilder pour exécuter la commande
        val process = ProcessBuilder(commandSplit).directory(File("scrcpy-v1.25/")).start()

        // Lire la sortie de la commande
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val output = reader.readLines().joinToString("\n")

        // Attendre que le processus se termine
        process.waitFor()

        // Vérifier si le processus s'est terminé avec succès
        if (process.exitValue() == 0) {
            output
        } else {
            "Erreur lors de l'exécution de la commande."
        }
    } catch (e: Exception) {
        println("Erreur : ${e.message}")
        e.message ?: "Erreur inconnue durant l'execution de la commande"
    }
}

fun executeCommandAsync(command: String): Process {
    val workingDir = File("scrcpy-v1.25/")
    val adbPath = "${workingDir.absolutePath}/adb.exe"

    // use local adb
    var localCommand = command.replace("adb", adbPath)
    localCommand = localCommand.replace("apk/", "${workingDir.absolutePath}/../apk/")
    var commandSplit = localCommand.split(" ")

    println("Execute : ${localCommand}")

    // Créer un ProcessBuilder pour exécuter la commande
    return ProcessBuilder(commandSplit).directory(File("scrcpy-v1.25/")).start()
}

suspend fun listenLogcat(uid: String): String {
    var process: Process? = null
    var reader: BufferedReader? = null
    return try {
        println("Écoute du logcat: cherche ResultTag_UID:$uid pour le package com.poly.middleman")

        // Nettoyer les logs précédents
        executeCommand("adb logcat -c")

        // Lancer adb logcat avec un filtre par package, tag, et durée maximale de 10 secondes
        val logcatCommand = "adb logcat com.poly.middleman:D -T 600"
        println("Commande Logcat : $logcatCommand")

        process = executeCommandAsync(logcatCommand)

        reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?

        while (true) {
            reader.readLine().also { line = it }
//            println(uid)
//            println(line)
            if (line?.contains(uid) == true) {
                val result = line.substringAfter("$uid").trim()
                val message = result
                print(message)
                return message
            }
        }

        println("Erreur : Aucun résultat trouvé dans les logs pour UID:$uid")
        "Erreur : Aucun résultat trouvé dans les logs pour UID:$uid"
    } catch (e: Exception) {
        println("Erreur lors de l'écoute des logs : ${e.message}")
        "Erreur lors de l'écoute des logs : ${e.message}"
    } finally {
        reader?.close()
        process?.destroy()
        println("Processus Logcat arrêté pour UID:$uid")
    }
}