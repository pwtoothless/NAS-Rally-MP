package com.nasrally.nasrally

import androidx.compose.ui.window.ComposeUIViewController

/**
 * iOS UIViewController factory function hosting Compose Multiplatform [App] via [ComposeUIViewController].
 *
 * @return UIViewController instance hosting the shared Compose UI hierarchy for Swift integration.
 */
fun MainViewController() = ComposeUIViewController { App() }