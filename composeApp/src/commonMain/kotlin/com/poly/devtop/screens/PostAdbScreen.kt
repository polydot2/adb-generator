package com.poly.devtop.screens

import Storage
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
import com.poly.devtop.data.*
import com.poly.devtop.multi.Adb
import com.poly.devtop.multi.Clipboard
import com.poly.devtop.multi.FilePicker
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

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
    var showSaveConfirmDialog by remember { mutableStateOf(false) } // Nouveau dialogue
    var fileToDelete by remember { mutableStateOf<String?>(null) }
    var configToLoad by remember { mutableStateOf<String?>(null) } // Config à charger après confirmation
    var jsonInput by remember { mutableStateOf(TextFieldValue("")) }
    var jsonError by remember { mutableStateOf("") }
    var configError by remember { mutableStateOf("") }
    var currentConfigFile by remember { mutableStateOf<String?>(null) }
    var isModified by remember { mutableStateOf(false) }
    var configFiles by remember { mutableStateOf(emptyList<String>()) }
    var selectedConfig by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()
    var showCsvDialog by remember { mutableStateOf(false) }
    var csvInput by remember { mutableStateOf(TextFieldValue("")) }
    var csvError by remember { mutableStateOf("") }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var logCatListening by remember { mutableStateOf(false) }
    var resultFromApk by remember { mutableStateOf("") }
    var job: Job? = null

    // Charge les configs au démarrage
    LaunchedEffect(Unit) {
        configFiles = Storage.listConfigs()
    }

    // Mettre à jour la liste des configs à l'ouverture de la popup
    LaunchedEffect(showConfigDialog) {
        if (showConfigDialog) {
            configFiles = Storage.listConfigs()
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
        if (currentConfigFile != null && configName.text == currentConfigFile) {
            try {
                val savedConfig = Storage.loadConfig(currentConfigFile!!)
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

    // Fonction pour charger une configuration
    val loadConfig: (String) -> Unit = { config ->
        scope.launch {
            try {
                val loadedConfig = Storage.loadConfig(config)
                if (loadedConfig != null) {
                    intentName = TextFieldValue(loadedConfig.intentName)
                    prefix = TextFieldValue(loadedConfig.prefix)
                    params = loadedConfig.params.map { jsonParam ->
                        val type = try {
                            ParamType.valueOf(jsonParam.type.uppercase())
                        } catch (e: IllegalArgumentException) {
                            ParamType.STRING
                        }
                        Param(jsonParam.key, jsonParam.value, jsonParam.isVisible, type)
                    }
                    configName = TextFieldValue(config)
                    currentConfigFile = config
                    configError = ""
                    isModified = false
                    resultFromApk = ""
                    logCatListening = false
                    selectedConfig = config
                }
            } catch (e: Exception) {
                configError = "Erreur lors du chargement de $config : ${e.message}"
            }
        }
    }

    MaterialTheme {
        Box(
            modifier = Modifier.padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row {
                // ConfigManager
                Column(Modifier.weight(0.1f).padding(horizontal = 4.dp)) {
                    ConfigManager(
                        configFiles = configFiles,
                        selectedConfig = selectedConfig,
                        onConfigClick = { config ->
                            if (isModified && config != currentConfigFile) {
                                // Afficher le dialogue de confirmation si la config est modifiée
                                configToLoad = config
                                showSaveConfirmDialog = true
                            } else {
                                // Charger directement si non modifié ou même config
                                loadConfig(config)
                            }
                        },
                        isModified = isModified,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Screen
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
                                scope.launch {
                                    val result = Adb.installApk()
                                    toastMessage = result
                                    showToast = true
                                    delay(4000)
                                    showToast = false
                                }
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
                                        scope.launch {
                                            try {
                                                val config = Config(
                                                    intentName = intentName.text,
                                                    prefix = prefix.text,
                                                    params = params.map {
                                                        JsonParam(it.key, it.value, it.type.name, it.isVisible)
                                                    }
                                                )
                                                Storage.saveConfig(configName.text, config)
                                                currentConfigFile = configName.text
                                                configError = ""
                                                isModified = false
                                                configFiles = Storage.listConfigs()
                                            } catch (e: Exception) {
                                                configError = "Erreur lors de la sauvegarde : ${e.message}"
                                            }
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
                                Text("Gérer les configs")
                            }
                            Spacer(Modifier.weight(1f))
                            Button(
                                onClick = {
                                    configName = TextFieldValue("")
                                    intentName = TextFieldValue("")
                                    prefix = TextFieldValue("")
                                    params = listOf()
                                    adbCommand = ""
                                    currentConfigFile = null
                                    isModified = false
                                    configError = ""
                                    resultFromApk = ""
                                    logCatListening = false
                                    selectedConfig = null
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
                        Modifier.background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Intent", style = MaterialTheme.typography.titleLarge)

                        // Champ pour le nom de l'intent
                        OutlinedTextField(
                            value = intentName,
                            singleLine = true,
                            onValueChange = { intentName = it },
                            label = { Text("Nom de l'intent") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Champ pour le préfixe des paramètres
                        OutlinedTextField(
                            value = prefix,
                            singleLine = true,
                            onValueChange = { prefix = it },
                            label = { Text("Préfixe des paramètres") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(
                        Modifier.background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp)
                        ).padding(16.dp)
                    ) {
                        // Afficher la commande ADB
                        if (adbCommand.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
//                                Text(
//                                    text = adbCommand,
//                                    modifier = Modifier.weight(1f),
//                                    color = MaterialTheme.colorScheme.primary
//                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            Clipboard.copyToClipboard(adbCommand)
                                        }
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
                                            val output = Adb.executeCommand(adbCommand)
                                            toastMessage = output
                                            showToast = true
                                            delay(4000)
                                            showToast = false
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Lancer")
                                }
                                Text(
                                    text = adbCommand,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        job = scope.launch(Dispatchers.Main) {
                                            adbCommand = generateAdbCommand(
                                                intentName.text,
                                                prefix.text,
                                                params.filter { it.isVisible && it.key.isNotBlank() })
                                            resultFromApk = ""

                                            val (newCommand, uid) = Adb.executeCommandWithMiddleApk(
                                                adbCommand,
                                                intentName.text
                                            )
                                            Adb.executeCommand(newCommand)
                                            logCatListening = true

                                            val logcatResult = withContext(Dispatchers.Default) {
                                                Adb.listenLogcat(uid)
                                            }
                                            logCatListening = false
                                            resultFromApk = logcatResult
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.PlayForWork, contentDescription = "Lancer a travers middleMan")
                                }
                            }
                        }
                    }

                    // Paramètres clé-valeur
                    Row(
                        modifier = Modifier.align(Alignment.Start),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Paramètres", style = MaterialTheme.typography.titleLarge)
                        Button(
                            modifier = Modifier.focusProperties { canFocus = false },
                            onClick = { params = params + Param("", "", true, ParamType.STRING) }
                        ) {
                            Text("Ajouter paramètre")
                        }
                        Button(
                            modifier = Modifier.focusProperties { canFocus = false },
                            onClick = { showCsvDialog = true }
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
                                    modifier = Modifier
                                        .focusProperties { canFocus = false }
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
                                if (logCatListening) {
                                    Button(onClick = {
                                        job?.cancel() // Cancels the entire coroutine, including the IO thread
                                        logCatListening = false
                                    }) {
                                        Text("Annuler")
                                    }
                                }
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
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(8.dp)
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
                                    scope.launch {
                                        try {
                                            val content = FilePicker.pickFile(".json")
                                            if (content != null) {
                                                jsonInput = TextFieldValue(content)
                                                jsonError = ""
                                            } else {
                                                jsonError = "Aucun fichier sélectionné"
                                            }
                                        } catch (e: Exception) {
                                            jsonError = "Erreur lors de la sélection du fichier : ${e.message}"
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
                                            jsonParam.value?.let { detectParamType(it) } ?: ParamType.STRING
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
                                    scope.launch {
                                        try {
                                            val content = FilePicker.pickFile(".csv")
                                            if (content != null) {
                                                csvInput = TextFieldValue(content)
                                                csvError = ""
                                            } else {
                                                csvError = "Aucun fichier sélectionné"
                                            }
                                        } catch (e: Exception) {
                                            csvError = "Erreur lors de la sélection du fichier : ${e.message}"
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
                    title = { Text("Gérer les configurations") },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(200.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (configFiles.isEmpty()) {
                                Text("Aucune configuration trouvée", color = MaterialTheme.colorScheme.onSurface)
                            } else {
                                configFiles.forEach { file ->
                                    Card(Modifier.height(48.dp).fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(file)
                                            IconButton(
                                                onClick = {
                                                    fileToDelete = file
                                                    showDeleteConfirmDialog = true
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Supprimer $file")
                                            }
                                        }
                                    }
                                }
                                if (configError.isNotEmpty()) {
                                    Text(configError, color = MaterialTheme.colorScheme.error)
                                }
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

            // Popup de confirmation pour sauvegarder avant de charger
            if (showSaveConfirmDialog && configToLoad != null) {
                AlertDialog(
                    onDismissRequest = {
                        showSaveConfirmDialog = false
                        configToLoad = null
                    },
                    title = { Text("Configuration modifiée") },
                    text = { Text("La configuration actuelle a été modifiée. Voulez-vous sauvegarder avant de charger '${configToLoad}' ?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (configName.text.isNotBlank()) {
                                    scope.launch {
                                        try {
                                            // Sauvegarder la configuration actuelle
                                            val config = Config(
                                                intentName = intentName.text,
                                                prefix = prefix.text,
                                                params = params.map {
                                                    JsonParam(it.key, it.value, it.type.name, it.isVisible)
                                                }
                                            )
                                            Storage.saveConfig(configName.text, config)
                                            configError = ""
                                            isModified = false
                                            configFiles = Storage.listConfigs()

                                            // Charger la nouvelle configuration
                                            loadConfig(configToLoad!!)

                                            showSaveConfirmDialog = false
                                            configToLoad = null
                                        } catch (e: Exception) {
                                            configError = "Erreur lors de la sauvegarde : ${e.message}"
                                            showSaveConfirmDialog = false
                                            configToLoad = null
                                        }
                                    }
                                } else {
                                    configError = "Le nom de la configuration ne peut pas être vide"
                                    showSaveConfirmDialog = false
                                    configToLoad = null
                                }
                            },
                            enabled = configName.text.isNotBlank()
                        ) {
                            Text("Sauvegarder et charger")
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    // Charger la nouvelle configuration sans sauvegarder
                                    loadConfig(configToLoad!!)
                                    showSaveConfirmDialog = false
                                    configToLoad = null
                                }
                            ) {
                                Text("Charger sans sauvegarder")
                            }
                            Button(
                                onClick = {
                                    // Annuler
                                    showSaveConfirmDialog = false
                                    configToLoad = null
                                }
                            ) {
                                Text("Annuler")
                            }
                        }
                    }
                )
            }

            // Popup de confirmation pour la suppression
            if (showDeleteConfirmDialog && fileToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text("Confirmer la suppression") },
                    text = { Text("Voulez-vous vraiment supprimer la configuration $fileToDelete?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        Storage.deleteConfig(fileToDelete!!)
                                        configFiles = Storage.listConfigs()
                                        configError = ""
                                        if (currentConfigFile == fileToDelete) {
                                            currentConfigFile = null
                                            configName = TextFieldValue("")
                                            isModified = false
                                        }
                                        if (selectedConfig == fileToDelete) {
                                            selectedConfig = null
                                        }
                                        showDeleteConfirmDialog = false
                                    } catch (e: Exception) {
                                        configError =
                                            "Erreur lors de la suppression de $fileToDelete : ${e.message}"
                                    }
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

    val paramString = params
        .filter { it.value.isNotBlank() }
        .joinToString(" ") { param ->
            buildString {
                append(param.type.flag)
                append(" ")
                append((prefix + param.key).encodeAdbParameter().wrap())
                append(" ")
                append(param.value.escapeValue(param.type).encodeAdbParameter().wrap(param.type))
            }.trim()
        }.takeIf { it.isNotEmpty() } ?: ""

    return "adb shell am start -a ${intentName.wrap()} $paramString".trim()
}

private fun String.escapeValue(type: ParamType? = null): String {
    return if (type == ParamType.JSON) {
        this.replace("\"", "\\\"")
    } else {
        this
    }
}

private fun String.wrap(type: ParamType? = null): String {
    return if (type == null || type == ParamType.STRING || type == ParamType.ARRAY_STRING) {
        "\"$this\""
    } else if (type == ParamType.JSON) {
        "\'$this\'"
    } else {
        this
    }
}

// Encode les paramètres pour la commande ADB
fun String.encodeAdbParameter(): String {
    return this.replace('"', '\"')
}