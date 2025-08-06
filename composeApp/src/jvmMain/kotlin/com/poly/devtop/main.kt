package com.poly.devtop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

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
