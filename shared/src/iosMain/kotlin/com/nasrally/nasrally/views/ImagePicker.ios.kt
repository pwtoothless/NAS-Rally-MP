package com.nasrally.nasrally.views

import androidx.compose.runtime.Composable

/**
 * iOS image picker implementation stub.
 */
@Composable
actual fun rememberImagePicker(
    source: ImageSource,
    onImagePicked: (ByteArray) -> Unit
): () -> Unit {
    return {
        println("iOS image picker invoked ($source)")
    }
}
