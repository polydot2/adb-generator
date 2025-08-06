package com.poly.devtop.multi

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame

actual object FilePicker {
    actual suspend fun pickFile(accept: String): String? = withContext(Dispatchers.Swing) {
        val fileDialog = FileDialog(null as Frame?, "Choisir un fichier", FileDialog.LOAD)
        fileDialog.file = accept // "*.json" ou "*.csv"
        fileDialog.isVisible = true
        fileDialog.files.firstOrNull()?.readText()
    }
}