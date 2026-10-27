package com.nasrally.nasrally.views

import androidx.compose.runtime.Composable

/**
 * Enum specifying image capture source selection.
 */
enum class ImageSource {
    CAMERA,
    GALLERY
}

/**
 * Multiplatform expect composable returning an image picker launch callback.
 *
 * @param source Selection source ([ImageSource.CAMERA] or [ImageSource.GALLERY]).
 * @param onImagePicked Callback returning picked image raw byte array.
 * @return Parameterless lambda to launch native platform image picker UI.
 */
@Composable
expect fun rememberImagePicker(
    source: ImageSource = ImageSource.GALLERY,
    onImagePicked: (ByteArray) -> Unit
): () -> Unit
