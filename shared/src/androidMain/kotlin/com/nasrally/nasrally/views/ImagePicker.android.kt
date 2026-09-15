package com.nasrally.nasrally.views

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.ByteArrayOutputStream

@Composable
actual fun rememberImagePicker(
    source: ImageSource,
    onImagePicked: (ByteArray) -> Unit
): () -> Unit {
    val context = LocalContext.current

    if (source == ImageSource.CAMERA) {
        val cameraLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicturePreview()
        ) { bitmap: Bitmap? ->
            if (bitmap != null) {
                try {
                    val outputStream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    onImagePicked(outputStream.toByteArray())
                } catch (e: Exception) {
                    println("Error processing Android camera photo: ${e.message}")
                }
            }
        }
        return {
            cameraLauncher.launch(null)
        }
    } else {
        val galleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            val outputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            onImagePicked(outputStream.toByteArray())
                        } else {
                            onImagePicked(bytes)
                        }
                    }
                } catch (e: Exception) {
                    println("Error processing Android picked image: ${e.message}")
                }
            }
        }
        return {
            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}
