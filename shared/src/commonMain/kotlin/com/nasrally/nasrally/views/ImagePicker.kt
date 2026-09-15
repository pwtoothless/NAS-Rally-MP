package com.nasrally.nasrally.views

import androidx.compose.runtime.Composable

enum class ImageSource {
    CAMERA,
    GALLERY
}

@Composable
expect fun rememberImagePicker(
    source: ImageSource = ImageSource.GALLERY,
    onImagePicked: (ByteArray) -> Unit
): () -> Unit
