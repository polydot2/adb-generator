package com.poly.devtop.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ConfigScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Configuration",
                style = MaterialTheme.typography.headlineMedium
            )
            TextField(
                value = "",
                onValueChange = {},
                label = { Text("Exemple de champ de configuration") },
                modifier = Modifier.width(300.dp)
            )
            Button(onClick = { /* TODO: Ajouter une action de sauvegarde */ }) {
                Text("Sauvegarder")
            }
        }
    }
}