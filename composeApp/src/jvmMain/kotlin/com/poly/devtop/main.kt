package com.poly.devtop

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.poly.devtop.screens.App
import com.poly.devtop.screens.ConfigScreen
import com.poly.devtop.screens.PostAdbScreen

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication, title = "DevTop", state = rememberWindowState(
            size = DpSize(1024.dp, 768.dp), // Taille par défaut : 800x600 pixels
            position = WindowPosition.PlatformDefault // Centré par défaut
        ),
//        icon = painterResource("src/main/resources/icon.png")
    ) {
        NavigationHost()
    }
}

// Routes disponibles
sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object PostAdb : Screen("ADB", "Post ADB", Icons.Default.Build)
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Config : Screen("config", "Config", Icons.Default.Settings)
}

@Composable
fun NavigationHost() {
    // start destination
    var currentScreen by remember { mutableStateOf<Screen>(Screen.PostAdb) }

    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Barre de navigation latérale
        NavigationRail(
            modifier = Modifier.width(72.dp).fillMaxHeight(),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            listOf(Screen.PostAdb).forEach { screen ->
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
                Screen.PostAdb -> PostAdbScreen()
                Screen.Home -> App()
                Screen.Config -> ConfigScreen()
            }
        }
    }
}