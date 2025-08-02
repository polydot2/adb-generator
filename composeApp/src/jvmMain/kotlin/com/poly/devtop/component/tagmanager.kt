package com.poly.devtop.component


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File

@Serializable
data class Tag(val name: String, val configPaths: List<String> = emptyList())

// Implémentation personnalisée de Transferable pour transférer un chemin de fichier
private class StringTransferable(private val text: String) : Transferable {
    private val supportedFlavors = arrayOf(DataFlavor.stringFlavor)

    override fun getTransferDataFlavors(): Array<DataFlavor> = supportedFlavors

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
        flavor == DataFlavor.stringFlavor

    override fun getTransferData(flavor: DataFlavor?): Any {
        if (!isDataFlavorSupported(flavor)) {
            throw UnsupportedFlavorException(flavor)
        }
        return text
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TagManager(
    tags: List<Tag>,
    configFiles: List<File>,
    onCreateTag: (String) -> Unit,
    onEditTag: (Tag, String) -> Unit,
    onDeleteTag: (Tag) -> Unit,
    onMoveConfigToTag: (File, Tag) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateTagDialog by remember { mutableStateOf(false) }
    var showEditTagDialog by remember { mutableStateOf<Tag?>(null) }
    var newTagName by remember { mutableStateOf(TextFieldValue("")) }
    var draggedConfig by remember { mutableStateOf<File?>(null) }
    var dropTargetTag by remember { mutableStateOf<Tag?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Bouton pour créer un nouveau tag
        Button(
            onClick = { showCreateTagDialog = true },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Créer un tag")
        }

        // Liste des tags
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tags) { tag ->
                var isExpanded by remember { mutableStateOf(false) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (dropTargetTag == tag) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    // En-tête du tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = if (isExpanded) "Fermer" else "Ouvrir"
                                )
                            }
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Row {
                            IconButton(onClick = { showEditTagDialog = tag }) {
                                Icon(Icons.Default.Edit, contentDescription = "Éditer tag")
                            }
                            IconButton(onClick = { onDeleteTag(tag) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer tag")
                            }
                        }
                    }

                    // Zone de drop pour le tag
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                color = if (dropTargetTag == tag) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .dragAndDropTarget(
                                shouldStartDragAndDrop = { true },
                                target = remember {
                                    object : DragAndDropTarget {
                                        override fun onDrop(event: DragAndDropEvent): Boolean {
                                            val transferable = event.dragData() as? DragData.Text
                                            val path = try {
                                                transferable?.readText()
                                            } catch (e: Exception) {
                                                println("Error reading transferable: ${e.message}")
                                                null
                                            }
                                            println("Dropped $path on ${tag.name}")
                                            return if (path != null) {
                                                onMoveConfigToTag(File(path), tag)
                                                draggedConfig = null
                                                dropTargetTag = null
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        override fun onEntered(event: DragAndDropEvent) {
                                            dropTargetTag = tag
                                            println("Dragging over ${tag.name}")
                                        }

                                        override fun onExited(event: DragAndDropEvent) {
                                            dropTargetTag = null
                                            println("Exited drop target ${tag.name}")
                                        }
                                    }
                                }
                            )
                    )

                    // Liste des configurations
                    AnimatedVisibility(visible = isExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            tag.configPaths.forEach { path ->
                                val configFile = File(path)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (draggedConfig == configFile) MaterialTheme.colorScheme.secondary.copy(
                                                alpha = 0.1f
                                            )
                                            else MaterialTheme.colorScheme.background,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp)
                                        .dragAndDropSource(
                                            transferData = {
                                                draggedConfig = configFile
                                                println("Started dragging ${configFile.name}")
                                                DragAndDropTransferData(
                                                    transferable = DragAndDropTransferable(StringTransferable(configFile.path)),
                                                    supportedActions = listOf(DragAndDropTransferAction.Move),
                                                    onTransferCompleted = { action ->
                                                        println("Transfer completed for ${configFile.name}: $action")
                                                        draggedConfig = null
                                                        dropTargetTag = null
                                                    }
                                                )
                                            }
                                        )
                                ) {
                                    Text(configFile.nameWithoutExtension)
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "Glisser",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section pour les configurations non assignées
            item {
                var isUnassignedExpanded by remember { mutableStateOf(true) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (dropTargetTag?.name == "") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isUnassignedExpanded = !isUnassignedExpanded }) {
                            Icon(
                                imageVector = if (isUnassignedExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = if (isUnassignedExpanded) "Fermer" else "Ouvrir"
                            )
                        }
                        Text(
                            text = "Non assignées",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .background(
                                color = if (dropTargetTag?.name == "") MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else Color.Transparent,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .dragAndDropTarget(
                                shouldStartDragAndDrop = { true },
                                target = remember {
                                    object : DragAndDropTarget {
                                        override fun onDrop(event: DragAndDropEvent): Boolean {
                                            val transferable = event.dragData() as? DragData.Text
                                            val path = try {
                                                transferable?.readText()
                                            } catch (e: Exception) {
                                                println("Error reading transferable: ${e.message}")
                                                null
                                            }
                                            println("Dropped $path on Non assignées")
                                            return if (path != null) {
                                                onMoveConfigToTag(File(path), Tag(""))
                                                draggedConfig = null
                                                dropTargetTag = null
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        override fun onEntered(event: DragAndDropEvent) {
                                            dropTargetTag = Tag("")
                                            println("Dragging over Non assignées")
                                        }

                                        override fun onExited(event: DragAndDropEvent) {
                                            dropTargetTag = null
                                            println("Exited drop target Non assignées")
                                        }
                                    }
                                }
                            )
                    )
                    AnimatedVisibility(visible = isUnassignedExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val unassignedConfigs = configFiles.filter { file ->
                                tags.none { tag -> tag.configPaths.contains(file.path) }
                            }
                            unassignedConfigs.forEach { configFile ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (draggedConfig == configFile) MaterialTheme.colorScheme.secondary.copy(
                                                alpha = 0.1f
                                            )
                                            else MaterialTheme.colorScheme.background,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(8.dp)
                                        .dragAndDropSource(
                                            transferData = {
                                                draggedConfig = configFile
                                                println("Started dragging ${configFile.name}")
                                                DragAndDropTransferData(
                                                    transferable = DragAndDropTransferable(StringTransferable(configFile.path)),
                                                    supportedActions = listOf(DragAndDropTransferAction.Move),
                                                    onTransferCompleted = { action ->
                                                        println("Transfer completed for ${configFile.name}: $action")
                                                        draggedConfig = null
                                                        dropTargetTag = null
                                                    }
                                                )
                                            }
                                        )
                                ) {
                                    Text(configFile.nameWithoutExtension)
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = "Glisser",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogue pour créer un tag
    if (showCreateTagDialog) {
        AlertDialog(
            onDismissRequest = { showCreateTagDialog = false },
            title = { Text("Créer un tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Nom du tag") },
                    isError = newTagName.text.isBlank() || tags.any { it.name == newTagName.text }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagName.text.isNotBlank() && tags.none { it.name == newTagName.text }) {
                            onCreateTag(newTagName.text)
                            newTagName = TextFieldValue("")
                            showCreateTagDialog = false
                        }
                    },
                    enabled = newTagName.text.isNotBlank() && tags.none { it.name == newTagName.text }
                ) {
                    Text("Créer")
                }
            },
            dismissButton = {
                Button(onClick = { showCreateTagDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Dialogue pour éditer un tag
    showEditTagDialog?.let { tag ->
        AlertDialog(
            onDismissRequest = { showEditTagDialog = null },
            title = { Text("Éditer le tag") },
            text = {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    label = { Text("Nouveau nom du tag") },
                    isError = newTagName.text.isBlank() || tags.any { it.name == newTagName.text && it != tag }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTagName.text.isNotBlank() && tags.none { it.name == newTagName.text && it != tag }) {
                            onEditTag(tag, newTagName.text)
                            newTagName = TextFieldValue("")
                            showEditTagDialog = null
                        }
                    },
                    enabled = newTagName.text.isNotBlank() && tags.none { it.name == newTagName.text && it != tag }
                ) {
                    Text("Modifier")
                }
            },
            dismissButton = {
                Button(onClick = { showEditTagDialog = null }) {
                    Text("Annuler")
                }
            }
        )
    }
}