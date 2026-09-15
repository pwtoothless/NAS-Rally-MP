package com.nasrally.nasrally.views

import androidx.compose.runtime.Composable
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

@Composable
actual fun rememberImagePicker(
    source: ImageSource,
    onImagePicked: (ByteArray) -> Unit
): () -> Unit {
    return {
        try {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = "image/*"
            if (source == ImageSource.CAMERA) {
                input.setAttribute("capture", "environment")
            }
            input.onchange = {
                val files = input.files
                if (files != null && files.length > 0) {
                    val file = files[0]
                    val reader = FileReader()
                    reader.onload = {
                        val dataUrl = reader.result.toString()
                        val img = document.createElement("img") as HTMLImageElement
                        img.onload = {
                            val canvas = document.createElement("canvas") as HTMLCanvasElement
                            canvas.width = img.width
                            canvas.height = img.height
                            val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
                            ctx.drawImage(img, 0.0, 0.0)
                            val pngDataUrl = canvas.toDataURL("image/png")
                            val base64 = pngDataUrl.substringAfter(",")
                            val binaryString = window.atob(base64)
                            val bytes = ByteArray(binaryString.length) { i -> binaryString[i].code.toByte() }
                            onImagePicked(bytes)
                        }
                        img.src = dataUrl
                    }
                    reader.readAsDataURL(file!!)
                }
            }
            input.click()
        } catch (e: Exception) {
            println("Error in JS file picker: ${e.message}")
        }
    }
}
