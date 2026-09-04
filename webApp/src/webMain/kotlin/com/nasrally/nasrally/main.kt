package com.nasrally.nasrally

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.nasrally.nasrally.web.WebApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        WebApp()
    }
}