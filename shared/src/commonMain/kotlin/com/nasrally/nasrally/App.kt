package com.nasrally.nasrally

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasrally.nasrally.theme.AppTheme
import com.nasrally.nasrally.views.AdminView
import com.nasrally.nasrally.views.AuthView
import com.nasrally.nasrally.views.ChatView
import com.nasrally.nasrally.views.HomeView
import com.nasrally.nasrally.views.OnboardingFlowContainer
import com.nasrally.nasrally.views.RalliesView
import com.nasrally.nasrally.views.SettingsView
import com.nasrally.nasrally.views.WaversView
import com.nasrally.nasrally.web.WebApp
import kotlinx.coroutines.delay

@Composable
fun App() {
    val platformName = getPlatform().name
    val isLargeScreenPlatform = platformName.contains("Web", ignoreCase = true) ||
            platformName.contains("Java", ignoreCase = true) ||
            platformName.contains("JVM", ignoreCase = true) ||
            platformName.contains("Chrome", ignoreCase = true) ||
            platformName.contains("Firefox", ignoreCase = true) ||
            platformName.contains("Safari", ignoreCase = true) ||
            platformName.contains("Opera", ignoreCase = true) ||
            platformName.contains("Edge", ignoreCase = true)

    var personInfo by remember { mutableStateOf<PersonInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            personInfo = fetchCurrentProfile()
        } catch (e: Exception) {
            println("No active session: ${e.message}")
        } finally {
            isLoading = false
        }
    }

    val themeName = personInfo?.theme ?: "Auto"

    AppTheme(themeName = themeName) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreenPlatform) {
                WebApp()
            } else {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (personInfo != null) {
                    if (!personInfo!!.tos) {
                        OnboardingFlowContainer(
                            person = personInfo!!,
                            onComplete = { completed ->
                                personInfo = completed
                            }
                        )
                    } else {
                        ContentView(
                            personInfo = personInfo!!,
                            onUpdatePerson = { personInfo = it },
                            onLogout = { personInfo = null }
                        )
                    }
                } else {
                    AuthView(onLoginSuccess = { personInfo = it })
                }
            }
        }
    }
}

@Composable
fun ContentView(
    personInfo: PersonInfo,
    onUpdatePerson: (PersonInfo) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val bannerData by NotificationManager.inAppBannerState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Rallies") },
                        label = { Text("Rallies") },
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                        label = { Text("Chat") },
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Description, contentDescription = "Wavers") },
                        label = { Text("Wavers") },
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 }
                    )
                    if (personInfo.privligeLevel == "Admin") {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Security, contentDescription = "Admin") },
                            label = { Text("Admin") },
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 }
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                when (selectedTab) {
                    0 -> HomeView(personInfo)
                    1 -> RalliesView(personInfo)
                    2 -> ChatView(personInfo)
                    3 -> WaversView(personInfo)
                    4 -> SettingsView(
                        person = personInfo,
                        onUpdatePerson = onUpdatePerson,
                        onLogout = onLogout
                    )
                    5 -> AdminView(personInfo)
                }
            }
        }

        bannerData?.let { data ->
            SubtleInAppBannerOverlay(
                data = data,
                onBannerClick = {
                    selectedTab = 2
                    NotificationManager.dismissInAppBanner()
                },
                onDismiss = {
                    NotificationManager.dismissInAppBanner()
                }
            )
        }
    }
}

@Composable
fun SubtleInAppBannerOverlay(
    data: InAppBannerData,
    onBannerClick: () -> Unit,
    onDismiss: () -> Unit
) {
    LaunchedEffect(data) {
        delay(6000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBannerClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${data.groupName} (${data.unreadCount} new messages)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (data.latestMessageText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${data.latestSenderName}: ${data.latestMessageText}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
