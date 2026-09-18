package com.nasrally.nasrally.web

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasrally.nasrally.AdminProfile
import com.nasrally.nasrally.AuthResult
import com.nasrally.nasrally.ChatViewModel
import com.nasrally.nasrally.DatabaseRally
import com.nasrally.nasrally.GroupRow
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.RallyParticipantRow
import com.nasrally.nasrally.RallyRequestInsert
import com.nasrally.nasrally.RallyRequestRow
import com.nasrally.nasrally.Waiver
import com.nasrally.nasrally.fetchCurrentProfile
import com.nasrally.nasrally.fetchUserWaivers
import com.nasrally.nasrally.getPlatform
import com.nasrally.nasrally.getProfileImageURL
import com.nasrally.nasrally.getRallyImageURL
import com.nasrally.nasrally.login
import com.nasrally.nasrally.logout
import com.nasrally.nasrally.signup
import com.nasrally.nasrally.supabase
import com.nasrally.nasrally.theme.AppTheme
import com.nasrally.nasrally.updateTheme
import com.nasrally.nasrally.views.AdminView
import com.nasrally.nasrally.views.AsyncImage
import com.nasrally.nasrally.views.IDView
import com.nasrally.nasrally.views.MessageBubble
import com.nasrally.nasrally.views.ProfileView
import com.nasrally.nasrally.views.RallyUserDetailDialog
import com.nasrally.nasrally.views.WaiverDetailDialog
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WebApp() {
    var personInfo by remember { mutableStateOf<PersonInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val platformName = remember { getPlatform().name }
    val isDesktop = remember(platformName) { platformName.contains("Java", ignoreCase = true) || platformName.contains("JVM", ignoreCase = true) }

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
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isDesktop) "Loading NASRally Desktop..." else "Loading NASRally Web...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (personInfo != null) {
                WebContentView(
                    personInfo = personInfo!!,
                    onUpdatePerson = { personInfo = it },
                    onLogout = { personInfo = null }
                )
            } else {
                WebAuthView(onLoginSuccess = { personInfo = it })
            }
        }
    }
}

@Composable
fun WebAuthView(onLoginSuccess: (PersonInfo) -> Unit) {
    var isSignUpMode by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
            ) {
                // Left Side: Brand Hero Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = "NASRALLY WEB",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Text(
                            text = "The Ultimate Automotive & Rally Platform",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Connect with fellow drivers, discover exclusive rally events, sign digital waivers, and manage live group chats directly from your browser.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureHighlight("🏎️", "Explore & Join Rally Events")
                        FeatureHighlight("💬", "Real-time Group Chat & Updates")
                        FeatureHighlight("📝", "Digital Waiver Management")
                        FeatureHighlight("🛡️", "Seamless Admin & Security")
                    }
                }

                // Right Side: Login / Signup Form
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isSignUpMode) {
                        WebSignupForm(
                            onSignUpSuccess = onLoginSuccess,
                            onSwitchToLogin = { isSignUpMode = false }
                        )
                    } else {
                        WebLoginForm(
                            onLoginSuccess = onLoginSuccess,
                            onSwitchToSignup = { isSignUpMode = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlight(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun WebLoginForm(
    onLoginSuccess: (PersonInfo) -> Unit,
    onSwitchToSignup: () -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rallyLogoUrl = remember { getRallyImageURL("NAS Rally") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            url = rallyLogoUrl,
            contentDescription = "NAS Rally Logo",
            modifier = Modifier
                .width(220.dp)
                .height(70.dp),
            error = {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome Back",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Please log in to access your web dashboard",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email Address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = passInput,
            onValueChange = { passInput = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onKeyEvent { keyEvent ->
                    if (keyEvent.key == Key.Enter && emailInput.isNotBlank() && passInput.isNotBlank()) {
                        errorMessage = ""
                        isLoggingIn = true
                        scope.launch {
                            val result = login(emailInput, passInput)
                            when (result) {
                                is AuthResult.Success -> onLoginSuccess(result.person)
                                is AuthResult.Failure -> errorMessage = result.message
                            }
                            isLoggingIn = false
                        }
                        true
                    } else {
                        false
                    }
                }
        )

        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                errorMessage = ""
                isLoggingIn = true
                scope.launch {
                    val result = login(emailInput, passInput)
                    when (result) {
                        is AuthResult.Success -> onLoginSuccess(result.person)
                        is AuthResult.Failure -> errorMessage = result.message
                    }
                    isLoggingIn = false
                }
            },
            enabled = !isLoggingIn && emailInput.isNotBlank() && passInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logging in...")
            } else {
                Text("Log In", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSwitchToSignup) {
                Text("Need an account? Sign up")
            }

            OutlinedButton(
                onClick = { onLoginSuccess(PersonInfo.testUser) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Use Test User", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun WebSignupForm(
    onSignUpSuccess: (PersonInfo) -> Unit,
    onSwitchToLogin: () -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passInput by remember { mutableStateOf("") }
    var confirmPassInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showPasswordMismatch by remember { mutableStateOf(false) }
    var signupErrorMessage by remember { mutableStateOf("") }
    var isSigningUp by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Web Account",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Join NASRally to access events and live chat",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Full Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email Address") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = passInput,
            onValueChange = { passInput = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = confirmPassInput,
            onValueChange = { confirmPassInput = it },
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (showPasswordMismatch) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (signupErrorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = signupErrorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (passInput == confirmPassInput) {
                    showPasswordMismatch = false
                    signupErrorMessage = ""
                    isSigningUp = true

                    scope.launch {
                        val result = signup(nameInput, emailInput, passInput)
                        when (result) {
                            is AuthResult.Success -> onSignUpSuccess(result.person)
                            is AuthResult.Failure -> signupErrorMessage = result.message
                        }
                        isSigningUp = false
                    }
                } else {
                    showPasswordMismatch = true
                    signupErrorMessage = ""
                }
            },
            enabled = !isSigningUp && nameInput.isNotBlank() && emailInput.isNotBlank() && passInput.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (isSigningUp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Creating Account...")
            } else {
                Text("Sign Up", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSwitchToLogin) {
            Text("Already have an account? Log in")
        }
    }
}

@Composable
fun WebContentView(
    personInfo: PersonInfo,
    onUpdatePerson: (PersonInfo) -> Unit,
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var themeMenuExpanded by remember { mutableStateOf(false) }
    val platformName = remember { getPlatform().name }
    val isDesktop = remember(platformName) { platformName.contains("Java", ignoreCase = true) || platformName.contains("JVM", ignoreCase = true) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Web/Desktop Navigation Sidebar
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier
                .width(280.dp)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    // Header Brand
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "NASRally Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "NASRally",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isDesktop) "DESKTOP PLATFORM" else "WEB PLATFORM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // User Profile Banner Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var sidebarAvatarUrl by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(personInfo.id) {
                                if (!personInfo.isTestUser) {
                                    sidebarAvatarUrl = getProfileImageURL(personInfo.id)
                                }
                            }
                            AsyncImage(
                                url = sidebarAvatarUrl,
                                contentDescription = personInfo.name,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape),
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = personInfo.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = personInfo.privligeLevel,
                                    fontSize = 12.sp,
                                    color = if (personInfo.privligeLevel == "Admin") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Text(
                        text = "NAVIGATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
                    )

                    // Navigation Items List
                    WebNavItem(
                        icon = Icons.Default.Home,
                        label = "Home Dashboard",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 }
                    )
                    WebNavItem(
                        icon = Icons.Default.DirectionsCar,
                        label = "Rallies Directory",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 }
                    )
                    WebNavItem(
                        icon = Icons.Default.Chat,
                        label = "Group Chats",
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 }
                    )
                    WebNavItem(
                        icon = Icons.Default.Description,
                        label = "Digital Waivers",
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 }
                    )
                    WebNavItem(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 }
                    )
                    if (personInfo.privligeLevel == "Admin") {
                        WebNavItem(
                            icon = Icons.Default.Security,
                            label = "Admin Console",
                            selected = selectedTab == 5,
                            onClick = { selectedTab = 5 }
                        )
                    }
                }

                // Bottom Sidebar Controls
                Column {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            TextButton(onClick = { themeMenuExpanded = true }) {
                                Text("Theme: ${personInfo.theme}", fontSize = 12.sp)
                            }
                            DropdownMenu(
                                expanded = themeMenuExpanded,
                                onDismissRequest = { themeMenuExpanded = false }
                            ) {
                                listOf("Auto", "Blue", "Red").forEach { themeName ->
                                    DropdownMenuItem(
                                        text = { Text(themeName) },
                                        onClick = {
                                            themeMenuExpanded = false
                                            val updatedPerson = personInfo.copy(theme = themeName)
                                            onUpdatePerson(updatedPerson)
                                            scope.launch {
                                                updateTheme(updatedPerson.id, themeName)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                scope.launch {
                                    logout()
                                    onLogout()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Log out",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Main Web Content Workspace Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            // Web Header Bar
            Surface(
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = when (selectedTab) {
                                0 -> "Home Dashboard"
                                1 -> "Rallies Directory"
                                2 -> "Group Chat Rooms"
                                3 -> "Digital Waivers"
                                4 -> "User Settings"
                                5 -> "Admin Console"
                                else -> "Dashboard"
                            },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "NASRally Web Workspace",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (personInfo.isTestUser) "Test Profile" else "Active Session",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Bounded Max-Width Content Bounding Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier.widthIn(max = 1280.dp).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> WebHomeView(personInfo = personInfo, onNavigate = { selectedTab = it })
                        1 -> WebRalliesView(personInfo = personInfo)
                        2 -> WebChatView(personInfo = personInfo)
                        3 -> WebWaiversView(personInfo = personInfo)
                        4 -> WebSettingsView(
                            personInfo = personInfo,
                            onUpdatePerson = onUpdatePerson,
                            onLogout = onLogout
                        )
                        5 -> AdminView(person = personInfo)
                    }
                }
            }
        }
    }
}

@Composable
private fun WebNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WebHomeView(
    personInfo: PersonInfo,
    onNavigate: (Int) -> Unit
) {
    var profileImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(personInfo.id) {
        if (!personInfo.isTestUser) {
            profileImageUrl = getProfileImageURL(personInfo.id)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome Hero Banner Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back, ${personInfo.name}! 👋",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Access all your rally events, team chat rooms, and waivers right here on the web.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onNavigate(1) }) {
                            Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Rallies")
                        }

                        OutlinedButton(onClick = { onNavigate(2) }) {
                            Icon(imageVector = Icons.Default.Chat, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Chat")
                        }
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                AsyncImage(
                    url = profileImageUrl,
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape),
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(110.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                )
            }
        }

        // Dashboard Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardMetricCard(
                title = "Joined Rallies",
                value = "${personInfo.ralliesJoined}",
                subtitle = if (personInfo.rallieNames.isNotEmpty()) personInfo.rallieNames.joinToString(", ") else "No rallies joined yet",
                icon = Icons.Default.DirectionsCar,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(1) }
            )

            DashboardMetricCard(
                title = "User Role & Access",
                value = personInfo.privligeLevel,
                subtitle = "Account Status: Active",
                icon = Icons.Default.Security,
                modifier = Modifier.weight(1f),
                onClick = { if (personInfo.privligeLevel == "Admin") onNavigate(5) else onNavigate(4) }
            )

            DashboardMetricCard(
                title = "Car Model",
                value = if (personInfo.carModel.isNotBlank()) personInfo.carModel else "Not set",
                subtitle = if (personInfo.instaHandle.isNotBlank()) "@${personInfo.instaHandle}" else "Edit profile in settings",
                icon = Icons.Default.Person,
                modifier = Modifier.weight(1f),
                onClick = { onNavigate(4) }
            )
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun WebRalliesView(personInfo: PersonInfo) {
    var allRallies by remember { mutableStateOf<List<DatabaseRally>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedRally by remember { mutableStateOf<DatabaseRally?>(null) }
    var isSendingRequest by remember { mutableStateOf(false) }
    var requestToast by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(personInfo.id) {
        if (!personInfo.isTestUser) {
            isLoading = true
            try {
                allRallies = supabase.from("rallies")
                    .select()
                    .decodeList<DatabaseRally>()
                if (allRallies.isNotEmpty()) {
                    selectedRally = allRallies.first()
                }
            } catch (e: Exception) {
                println("Error loading rallies: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    if (personInfo.isTestUser) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Test User Profile Active",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Live rally events from Supabase database are disabled for the local test profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else if (isLoading && allRallies.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        // Master-Detail 2-Column Side-by-Side Web View!
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left Master Column: List of Rallies
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Available Events (${allRallies.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allRallies) { rally ->
                            val isJoined = personInfo.rallieNames.contains(rally.name)
                            val isSelected = selectedRally?.id == rally.id

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedRally = rally }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        url = getRallyImageURL(rally.name),
                                        contentDescription = rally.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape),
                                        error = {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rally.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (isJoined) "✓ Joined" else "Not Joined",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isJoined) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Detail Column: Selected Rally View
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (selectedRally != null) {
                    val rally = selectedRally!!
                    val isJoined = personInfo.rallieNames.contains(rally.name)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                url = getRallyImageURL(rally.name),
                                contentDescription = rally.name,
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(60.dp)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Column {
                                Text(
                                    text = rally.name,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isJoined) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                ) {
                                    Text(
                                        text = if (isJoined) "YOU ARE JOINED" else "JOIN REQUEST REQUIRED",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isJoined) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(24.dp))

                        if (!rally.eventStart.isNullOrEmpty() && !rally.eventEnd.isNullOrEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dates: ${rally.eventStart} — ${rally.eventEnd}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        rally.eventCost?.let { cost ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Cost: $$cost",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Text(
                            text = "About this Rally",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = rally.description
                                ?: "Welcome to the ${rally.name} rally! Join us for an incredible driving experience filled with scenic routes, automotive passion, and community camaraderie.",
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (requestToast.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = requestToast,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                isSendingRequest = true
                                scope.launch {
                                    try {
                                        val req = RallyRequestInsert(userId = personInfo.id, rallyId = rally.id)
                                        supabase.from("rally_requests").insert(req)
                                        requestToast = "Join request sent to Rally Admin!"
                                    } catch (e: Exception) {
                                        requestToast = "Failed to send request."
                                    } finally {
                                        isSendingRequest = false
                                    }
                                }
                            },
                            enabled = !isJoined && !isSendingRequest,
                            modifier = Modifier
                                .width(220.dp)
                                .height(48.dp)
                        ) {
                            if (isSendingRequest) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(if (isJoined) "Already Joined" else "Join Rally Event")
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Select a rally event from the left list",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WebChatView(personInfo: PersonInfo) {
    var availableGroups by remember { mutableStateOf<List<GroupRow>>(emptyList()) }
    var selectedGroup by remember { mutableStateOf<GroupRow?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(personInfo.id) {
        if (!personInfo.isTestUser) {
            isLoading = true
            try {
                availableGroups = supabase.from("groups")
                    .select(Columns.raw("id, name, group_members!inner(user_id)")) {
                        filter {
                            eq("group_members.user_id", personInfo.id)
                        }
                    }
                    .decodeList<GroupRow>()
                if (availableGroups.isNotEmpty()) {
                    selectedGroup = availableGroups.first()
                }
            } catch (e: Exception) {
                println("Error loading groups: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Dual-Pane Left Column: Group Channels
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Group Channels",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (availableGroups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No active groups",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableGroups) { group ->
                            val isSelected = selectedGroup?.id == group.id

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedGroup = group }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        url = getRallyImageURL(group.name ?: ""),
                                        contentDescription = group.name,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape),
                                        error = {
                                            Icon(
                                                imageVector = Icons.Default.DirectionsCar,
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = group.name ?: "Group Chat",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dual-Pane Right Column: Active Thread Workspace
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                if (selectedGroup != null) {
                    val group = selectedGroup!!
                    val scope = rememberCoroutineScope()
                    val viewModel = remember(group.id) { ChatViewModel(group.id, scope) }
                    val messages by viewModel.messages.collectAsState()
                    var messageInput by remember { mutableStateOf("") }
                    val listState = rememberLazyListState()

                    LaunchedEffect(group.id) {
                        viewModel.loadMessages(isRefresh = true)
                    }

                    LaunchedEffect(messages.firstOrNull()?.id, messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(0)
                        }
                    }

                    // Thread Top Bar
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                url = getRallyImageURL(group.name ?: ""),
                                contentDescription = group.name,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                error = {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = group.name ?: "Chat Room",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Live Web Channel",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Messages List
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        items(messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                isCurrentUser = message.senderId == personInfo.id
                            )
                        }
                    }

                    // Bottom Message Input Bar with Enter Key Support
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            placeholder = { Text("Write a message...") },
                            modifier = Modifier
                                .weight(1f)
                                .onKeyEvent { keyEvent ->
                                    if (keyEvent.key == Key.Enter && messageInput.trim().isNotBlank()) {
                                        val textToSend = messageInput.trim()
                                        messageInput = ""
                                        scope.launch {
                                            viewModel.sendMessage(textToSend, personInfo.id)
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = {
                                val textToSend = messageInput.trim()
                                if (textToSend.isNotBlank()) {
                                    messageInput = ""
                                    scope.launch {
                                        viewModel.sendMessage(textToSend, personInfo.id)
                                    }
                                }
                            },
                            enabled = messageInput.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Select a channel from the left sidebar to start chatting",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WebWaiversView(personInfo: PersonInfo) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Pending, 1: Signed
    var pendingWaivers by remember { mutableStateOf<List<Waiver>>(emptyList()) }
    var signedWaivers by remember { mutableStateOf<List<Waiver>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedWaiver by remember { mutableStateOf<Waiver?>(null) }
    val scope = rememberCoroutineScope()

    val loadWaivers: () -> Unit = {
        scope.launch {
            isLoading = true
            val result = fetchUserWaivers(personInfo.id)
            pendingWaivers = result.pending
            signedWaivers = result.signed
            isLoading = false
        }
    }

    LaunchedEffect(personInfo.id) {
        loadWaivers()
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(28.dp)) {
            Text(
                text = "Digital Waivers & Legal Documents",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Review and execute required event waivers prior to participating in rallies",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.widthIn(max = 400.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Pending (${pendingWaivers.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Signed (${signedWaivers.size})", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (selectedTab == 0) {
                if (pendingWaivers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "All required waivers have been executed!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "No pending document signatures needed for your profile.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(pendingWaivers) { waiver ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWaiver = waiver }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            waiver.rallies?.name?.let { rallyName ->
                                                if (rallyName.isNotBlank()) {
                                                    Text(
                                                        text = rallyName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                }
                                            }
                                            Text(
                                                text = waiver.waiverName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    Button(onClick = { selectedWaiver = waiver }) {
                                        Text("View & Sign")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (signedWaivers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No signed waivers yet.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(signedWaivers) { waiver ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedWaiver = waiver }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            waiver.rallies?.name?.let { rallyName ->
                                                if (rallyName.isNotBlank()) {
                                                    Text(
                                                        text = rallyName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                }
                                            }
                                            Text(
                                                text = waiver.waiverName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }

                                    OutlinedButton(onClick = { selectedWaiver = waiver }) {
                                        Text("View Document", color = Color(0xFF2E7D32))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedWaiver?.let { waiver ->
        val isAlreadySigned = signedWaivers.any { it.id == waiver.id }
        WaiverDetailDialog(
            waiver = waiver,
            userId = personInfo.id,
            isAlreadySigned = isAlreadySigned,
            onSigned = {
                loadWaivers()
                selectedWaiver = null
            },
            onDismiss = { selectedWaiver = null }
        )
    }
}

@Composable
fun WebSettingsView(
    personInfo: PersonInfo,
    onUpdatePerson: (PersonInfo) -> Unit,
    onLogout: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(0) } // 0=Profile, 1=ID

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            TabRow(
                selectedTabIndex = selectedSection,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Tab(selected = selectedSection == 0, onClick = { selectedSection = 0 }, text = { Text("Profile Editor") })
                Tab(selected = selectedSection == 1, onClick = { selectedSection = 1 }, text = { Text("Verify ID") })
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedSection) {
                    0 -> ProfileView(person = personInfo, onUpdatePerson = onUpdatePerson)
                    1 -> IDView(person = personInfo)
                }
            }
        }
    }
}
