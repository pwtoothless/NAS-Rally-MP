package com.nasrally.nasrally.views

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import org.jetbrains.compose.resources.decodeToImageBitmap

private val imageHttpClient = HttpClient()

sealed class AsyncImageState {
    object Loading : AsyncImageState()
    data class Success(val bitmap: ImageBitmap) : AsyncImageState()
    data class Error(val throwable: Throwable? = null) : AsyncImageState()
}

@Composable
fun AsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    loading: @Composable () -> Unit = {},
    error: @Composable () -> Unit = {}
) {
    var state by remember(url) { mutableStateOf<AsyncImageState>(AsyncImageState.Loading) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank()) {
            state = AsyncImageState.Error()
            return@LaunchedEffect
        }
        try {
            val bytes = imageHttpClient.get(url).bodyAsBytes()
            val bitmap = bytes.decodeToImageBitmap()
            state = AsyncImageState.Success(bitmap)
        } catch (e: Exception) {
            state = AsyncImageState.Error(e)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when (val s = state) {
            is AsyncImageState.Loading -> loading()
            is AsyncImageState.Success -> {
                Image(
                    bitmap = s.bitmap,
                    contentDescription = contentDescription,
                    modifier = Modifier.matchParentSize(),
                    contentScale = contentScale
                )
            }
            is AsyncImageState.Error -> error()
        }
    }
}
