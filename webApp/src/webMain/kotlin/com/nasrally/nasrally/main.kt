package com.nasrally.nasrally

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.nasrally.nasrally.web.WebApp

/**
 * Main application entry point for NASRally Wasm/JS Web target.
 *
 * Initializes the web canvas viewport using [ComposeViewport] and launches [WebApp] for web browser rendering.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        WebApp()
    }
}