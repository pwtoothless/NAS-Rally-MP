package com.nasrally.nasrally.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

@Composable
actual fun rememberImagePicker(
    source: ImageSource,
    onImagePicked: (ByteArray) -> Unit
): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch(Dispatchers.IO) {
            try {
                val dialog = FileDialog(null as Frame?, "Select Image Document", FileDialog.LOAD)
                dialog.isVisible = true
                val fileStr = dialog.file
                val dirStr = dialog.directory
                if (fileStr != null && dirStr != null) {
                    val file = File(dirStr, fileStr)
                    val bufferedImage = ImageIO.read(file)
                    if (bufferedImage != null) {
                        val baos = ByteArrayOutputStream()
                        ImageIO.write(bufferedImage, "png", baos)
                        onImagePicked(baos.toByteArray())
                    } else {
                        onImagePicked(file.readBytes())
                    }
                }
            } catch (e: Exception) {
                println("Error selecting desktop image: ${e.message}")
            }
        }
    }
}
