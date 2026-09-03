package com.nasrally.nasrally.views

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasrally.nasrally.AuthResult
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.getRallyImageURL
import com.nasrally.nasrally.login
import com.nasrally.nasrally.signup
import kotlinx.coroutines.launch

@Composable
fun AuthView(onLoginSuccess: (PersonInfo) -> Unit) {
    var isSignUpMode by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isSignUpMode) {
            SignupContent(
                onSignUpSuccess = onLoginSuccess,
                onSwitchToLogin = { isSignUpMode = false }
            )
        } else {
            LoginContent(
                onLoginSuccess = onLoginSuccess,
                onSwitchToSignup = { isSignUpMode = true }
            )
        }
    }
}

@Composable
private fun LoginContent(
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Login",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            AsyncImage(
                url = rallyLogoUrl,
                contentDescription = "NAS Rally Logo",
                modifier = Modifier
                    .width(300.dp)
                    .height(100.dp),
                loading = {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                },
                error = {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Car Logo Fallback",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { Text("Email") },
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
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    modifier = Modifier.height(50.dp)
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logging In")
                    } else {
                        Text("Login")
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text("Or", style = MaterialTheme.typography.bodyLarge)

                Spacer(modifier = Modifier.width(16.dp))

                TextButton(onClick = onSwitchToSignup) {
                    Text("Signup", fontSize = 16.sp)
                }
            }
        }

        OutlinedButton(
            onClick = {
                onLoginSuccess(PersonInfo.testUser)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Text("Test User")
        }
    }
}

@Composable
private fun SignupContent(
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
    val rallyLogoUrl = remember { getRallyImageURL("NAS Rally") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Signup",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        AsyncImage(
            url = rallyLogoUrl,
            contentDescription = "NAS Rally Logo",
            modifier = Modifier
                .width(300.dp)
                .height(100.dp),
            loading = {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
            },
            error = {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Car Logo Fallback",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("Email") },
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
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (signupErrorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = signupErrorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

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
                .height(50.dp)
        ) {
            if (isSigningUp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Signing Up")
            } else {
                Text("Signup")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onSwitchToLogin) {
            Text("Already have an account? Login", fontSize = 16.sp)
        }
    }
}
