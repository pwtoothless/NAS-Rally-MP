package com.nasrally.nasrally

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Main application entry point for NASRally Compose Desktop (JVM target).
 *
 * Configures the desktop window title, initial dimensions (1280x800), and launches the shared root [App] composable.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "NASRally Desktop",
        state = rememberWindowState(width = 1280.dp, height = 800.dp)
    ) {
        App()
    }
}