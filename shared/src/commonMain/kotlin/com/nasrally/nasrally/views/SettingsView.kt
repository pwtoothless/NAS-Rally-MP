package com.nasrally.nasrally.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.deleteUserAccount
import com.nasrally.nasrally.logout
import com.nasrally.nasrally.updateTheme
import kotlinx.coroutines.launch

sealed class SettingsSubScreen {
    object Main : SettingsSubScreen()
    object Profile : SettingsSubScreen()
    object ID : SettingsSubScreen()
}

@Composable
fun SettingsView(
    person: PersonInfo,
    onUpdatePerson: (PersonInfo) -> Unit,
    onLogout: () -> Unit
) {
    var subScreen by remember { mutableStateOf<SettingsSubScreen>(SettingsSubScreen.Main) }
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var showSelfDeleteConfirmStep1 by remember { mutableStateOf(false) }
    var showSelfDeleteConfirmStep2 by remember { mutableStateOf(false) }
    val themes = listOf("Auto", "Blue", "Red")
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        when (subScreen) {
            is SettingsSubScreen.Profile -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { subScreen = SettingsSubScreen.Main },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    ProfileView(person = person, onUpdatePerson = onUpdatePerson)
                }
            }
            is SettingsSubScreen.ID -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { subScreen = SettingsSubScreen.Main },
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    IDView(person = person)
                }
            }
            is SettingsSubScreen.Main -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column {
                            SettingsRow(
                                icon = Icons.Default.Person,
                                title = "Profile",
                                onClick = { subScreen = SettingsSubScreen.Profile }
                            )

                            HorizontalDivider()

                            // Theme Selector Row
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { themeMenuExpanded = true }
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "Theme: ${person.theme}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                DropdownMenu(
                                    expanded = themeMenuExpanded,
                                    onDismissRequest = { themeMenuExpanded = false }
                                ) {
                                    themes.forEach { themeName ->
                                        DropdownMenuItem(
                                            text = { Text(themeName) },
                                            onClick = {
                                                themeMenuExpanded = false
                                                val updatedPerson = person.copy(theme = themeName)
                                                onUpdatePerson(updatedPerson)
                                                scope.launch {
                                                    updateTheme(updatedPerson.id, themeName)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            SettingsRow(
                                icon = Icons.Default.PersonPin,
                                title = "ID",
                                onClick = { subScreen = SettingsSubScreen.ID }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                logout()
                                onLogout()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Logout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Account Deletion",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Permanently delete your account and remove login credentials.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { showSelfDeleteConfirmStep1 = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text("Delete Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSelfDeleteConfirmStep1) {
        AlertDialog(
            onDismissRequest = { showSelfDeleteConfirmStep1 = false },
            title = { Text("Delete Your Account?") },
            text = { Text("Are you sure you want to delete your account? This will log you out and remove your login credentials.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSelfDeleteConfirmStep1 = false
                        showSelfDeleteConfirmStep2 = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelfDeleteConfirmStep1 = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSelfDeleteConfirmStep2) {
        AlertDialog(
            onDismissRequest = { showSelfDeleteConfirmStep2 = false },
            title = { Text("FINAL WARNING: Delete Account") },
            text = { Text("This action cannot be undone. Your login credentials will be permanently removed. Your profile record and signed waivers will be retained for legal compliance. Are you sure you want to proceed?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSelfDeleteConfirmStep2 = false
                        scope.launch {
                            deleteUserAccount(person.id)
                            logout()
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSelfDeleteConfirmStep2 = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
