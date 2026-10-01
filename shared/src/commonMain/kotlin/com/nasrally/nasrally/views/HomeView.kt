package com.nasrally.nasrally.views

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasrally.nasrally.LocalCache
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.fetchAISummary
import com.nasrally.nasrally.getPlatform
import com.nasrally.nasrally.getProfileImageURL
import kotlinx.coroutines.launch
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeView(person: PersonInfo) {
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var aiSummary by remember { mutableStateOf<String?>(null) }
    var isLoadingSummary by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var restrictMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val isAndroid = remember { getPlatform().name.startsWith("Android") }

    suspend fun loadSummary(forceRefresh: Boolean) {
        if (person.isTestUser) return

        val lastFetchKey = "aiSummaryLastFetch_${person.id}"
        val summaryKey = "aiSummaryText_${person.id}"

        val lastFetchTime = LocalCache.getLong(lastFetchKey)
        val cachedSummary = LocalCache.getString(summaryKey)

        val now = Clock.System.now().toEpochMilliseconds()
        val timeSinceLastFetch = if (lastFetchTime != null) now - lastFetchTime else Long.MAX_VALUE

        // 1 day (24 hours) = 86,400,000 ms, 30 mins = 1,800,000 ms
        val shouldFetch: Boolean

        if (forceRefresh) {
            if (timeSinceLastFetch > 1_800_000L) {
                shouldFetch = true
            } else {
                shouldFetch = false
                val remainingMs = 1_800_000L - timeSinceLastFetch
                val remainingSec = (remainingMs + 999) / 1000
                val minutes = remainingSec / 60
                val seconds = remainingSec % 60
                restrictMessage = "Please wait $minutes minute(s) and $seconds second(s) before refreshing again."
            }
        } else {
            if (cachedSummary == null || timeSinceLastFetch > 86_400_000L) {
                shouldFetch = true
            } else {
                shouldFetch = false
            }
        }

        if (!shouldFetch) {
            if (cachedSummary != null) {
                aiSummary = cachedSummary
            }
            return
        }

        restrictMessage = null
        if (!forceRefresh) {
            isLoadingSummary = true
        }

        try {
            val fetched = fetchAISummary()
            LocalCache.setLong(lastFetchKey, now)
            LocalCache.setString(summaryKey, fetched)
            aiSummary = fetched
        } catch (e: Exception) {
            println("Failed to fetch AI Summary: ${e.message}")
            aiSummary = cachedSummary ?: "Failed to load summary."
        } finally {
            isLoadingSummary = false
        }
    }

    LaunchedEffect(person.id) {
        if (!person.isTestUser) {
            profileImageUrl = getProfileImageURL(person.id)
        }
        loadSummary(forceRefresh = false)
    }

    // Gradient loading shimmer animation
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val aiGradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E54B3).copy(alpha = 0.3f),
            Color(0xFFC11326).copy(alpha = 0.6f),
            Color(0xFF1E54B3).copy(alpha = 0.3f)
        ),
        start = Offset(offset, 0f),
        end = Offset(offset + 500f, 500f)
    )

    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Home",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Profile card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CachedProfileImage(
                    userId = person.id,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    loading = {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    },
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Hi, ${person.name}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Notification Summary Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Sparkles",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Notification Summary",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoadingSummary) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(aiGradientBrush)
                )
            } else if (aiSummary != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = aiSummary ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = if (isAndroid) "Pull down to refresh your summary." else "Use the refresh button to update summary.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            restrictMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Snackbar(
                    shape = RoundedCornerShape(12.dp),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text(msg)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isAndroid) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        isRefreshing = true
                        loadSummary(forceRefresh = true)
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                content()
            }
        } else {
            content()

            // Desktop and Web Refresh Button on Bottom Right
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        isRefreshing = true
                        loadSummary(forceRefresh = true)
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Summary",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}
