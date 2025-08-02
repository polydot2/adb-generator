package com.poly.devtop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.poly.devtop.screens.App
import com.poly.devtop.screens.ConfigScreen
import com.poly.devtop.screens.PostAdbScreen


fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "DevTop") {
        NavigationHost()
    }
}

// Routes disponibles
sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object PostAdb : Screen("postAdb", "Post ADB", Icons.Default.Build)
    object Config : Screen("config", "Config", Icons.Default.Settings)
}

@Composable
fun NavigationHost() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Barre de navigation latérale
        NavigationRail(
            modifier = Modifier.width(72.dp).fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            listOf(Screen.Home, Screen.PostAdb, Screen.Config).forEach { screen ->
                NavigationRailItem(
                    selected = currentScreen == screen,
                    onClick = { currentScreen = screen },
                    icon = { Icon(screen.icon, contentDescription = screen.label) },
                    label = { Text(screen.label) }
                )
            }
        }

        // Contenu principal
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            when (currentScreen) {
                Screen.Home -> App()
                Screen.PostAdb -> PostAdbScreen()
                Screen.Config -> ConfigScreen()
            }
        }
    }
}