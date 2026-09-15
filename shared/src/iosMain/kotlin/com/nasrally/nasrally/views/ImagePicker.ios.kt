package com.nasrally.nasrally.views

import androidx.compose.runtime.Composable

@Composable
actual fun rememberImagePicker(
    source: ImageSource,
    onImagePicked: (ByteArray) -> Unit
): () -> Unit {
    return {
        println("iOS image picker invoked ($source)")
    }
}
