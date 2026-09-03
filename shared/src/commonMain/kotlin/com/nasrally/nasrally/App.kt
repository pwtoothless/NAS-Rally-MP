package com.nasrally.nasrally

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.nasrally.nasrally.views.AdminView
import com.nasrally.nasrally.views.AuthView
import com.nasrally.nasrally.views.ChatView
import com.nasrally.nasrally.views.HomeView
import com.nasrally.nasrally.views.RalliesView
import com.nasrally.nasrally.views.SettingsView
import com.nasrally.nasrally.views.WaversView

@Composable
fun App() {
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

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (personInfo != null) {
        ContentView(
            personInfo = personInfo!!,
            onUpdatePerson = { personInfo = it },
            onLogout = { personInfo = null }
        )
    } else {
        AuthView(onLoginSuccess = { personInfo = it })
    }
}

@Composable
fun ContentView(
    personInfo: PersonInfo,
    onUpdatePerson: (PersonInfo) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

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
}
