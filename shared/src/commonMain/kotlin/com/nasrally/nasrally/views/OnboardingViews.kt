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
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.updateProfile
import com.nasrally.nasrally.updateTOS
import kotlinx.coroutines.launch

enum class OnboardingStep {
    TOS,
    ONBOARDING,
    ID_PROMPT,
    ID_UPLOAD
}

@Composable
fun OnboardingFlowContainer(
    person: PersonInfo,
    onComplete: (PersonInfo) -> Unit
) {
    var step by remember { mutableStateOf(OnboardingStep.TOS) }
    var currentPerson by remember { mutableStateOf(person) }

    when (step) {
        OnboardingStep.TOS -> {
            TOSView(person = currentPerson) {
                currentPerson = currentPerson.copy(tos = true)
                step = OnboardingStep.ONBOARDING
            }
        }
        OnboardingStep.ONBOARDING -> {
            OnboardingView(person = currentPerson) { updated ->
                currentPerson = updated
                step = OnboardingStep.ID_PROMPT
            }
        }
        OnboardingStep.ID_PROMPT -> {
            IDPromptView(
                onProvideIDNow = {
                    step = OnboardingStep.ID_UPLOAD
                },
                onSkip = {
                    currentPerson = currentPerson.copy(tos = true)
                    onComplete(currentPerson)
                }
            )
        }
        OnboardingStep.ID_UPLOAD -> {
            IDView(
                person = currentPerson,
                isOnboarding = true,
                onComplete = {
                    currentPerson = currentPerson.copy(tos = true)
                    onComplete(currentPerson)
                }
            )
        }
    }
}

@Composable
fun TOSView(
    person: PersonInfo,
    onAccept: () -> Unit
) {
    var showDeclineAlert by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Terms of Service for NAS-Rally",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = """
                Welcome to the NAS-Rally App. By using this application, you agree to comply with and be bound by the following Terms of Service. If you do not agree to these terms, please do not use the app.

                1. Code of Conduct
                To maintain a safe and welcoming environment, all users must adhere to the following behavioral standards:
                Respect the Community: Treat all fellow users and community members with respect. Harassment, discrimination, hate speech, and abusive behavior will not be tolerated.
                Respect the Admins: Application administrators and event organizers dedicate their time to maintaining this platform. Users are expected to follow administrative instructions, respect their decisions, and communicate constructively.
                Respect the Road: Users must treat all roads, trails, and physical environments with respect. Do not engage in activities that cause unnecessary damage to public or private property, local infrastructure, or the natural environment.

                2. Compliance with Laws and Regulations
                Your safety and the safety of the public are paramount. When using the NAS-Rally App or participating in any associated activities:
                Follow the Law: You agree to strictly adhere to all applicable local, state, and federal laws, regulations, and ordinances.
                Traffic and Safety Rules: You are solely responsible for obeying all traffic laws, speed limits, and road signs. The NAS-Rally App is not an excuse to drive recklessly, unlawfully, or dangerously.

                3. Assumption of Risk and Liability
                Using the NAS-Rally App and participating in driving or rally activities carries inherent risks.
                Personal Responsibility: You assume all risks associated with your use of the app and your actions on the road.
                Disclaimer: The developers, administrators, and affiliates of the NAS-Rally App are not liable for any property damage, personal injury, traffic violations, legal consequences, or loss of life that may occur while using the app or participating in related events.

                4. Account Termination
                Administrators reserve the right to suspend or terminate any user account at any time, without prior notice, if a user violates these Terms of Service, acts dangerously on the road, or creates a toxic environment for other members.

                5. Modifications to Terms
                We reserve the right to update or modify these Terms of Service at any time. Continued use of the NAS-Rally App constitutes your acceptance of the revised terms.
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { showDeclineAlert = true }) {
                Text("Decline")
            }

            Button(onClick = {
                scope.launch {
                    updateTOS(person.id, true)
                    onAccept()
                }
            }) {
                Text("Accept")
            }
        }
    }

    if (showDeclineAlert) {
        AlertDialog(
            onDismissRequest = { showDeclineAlert = false },
            title = { Text("Terms of Service Required") },
            text = { Text("You must accept the Terms of Service in order to use the NAS Rally app.") },
            confirmButton = {
                Button(onClick = { showDeclineAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun OnboardingView(
    person: PersonInfo,
    onComplete: (PersonInfo) -> Unit
) {
    var firstNameInput by remember { mutableStateOf("") }
    var lastNameInput by remember { mutableStateOf("") }
    var phoneNumberInput by remember { mutableStateOf(person.phoneNumber) }
    var instaHandleInput by remember { mutableStateOf(person.instaHandle) }
    var carModelInput by remember { mutableStateOf(person.carModel) }
    var bioInput by remember { mutableStateOf(person.bio) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(person) {
        val parts = person.name.split(" ", limit = 2)
        if (parts.isNotEmpty() && firstNameInput.isEmpty()) firstNameInput = parts[0]
        if (parts.size > 1 && lastNameInput.isEmpty()) lastNameInput = parts[1]
        if (phoneNumberInput.isEmpty()) phoneNumberInput = person.phoneNumber
        if (instaHandleInput.isEmpty()) instaHandleInput = person.instaHandle
        if (carModelInput.isEmpty()) carModelInput = person.carModel
        if (bioInput.isEmpty()) bioInput = person.bio
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Onboarding",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Text(
            text = "Here you will fill out some information about yourself",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = firstNameInput,
                onValueChange = { firstNameInput = it },
                label = { Text("First Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = lastNameInput,
                onValueChange = { lastNameInput = it },
                label = { Text("Last Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = phoneNumberInput,
            onValueChange = { phoneNumberInput = it },
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = instaHandleInput,
            onValueChange = { instaHandleInput = it },
            label = { Text("Instagram Handle (No @)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = carModelInput,
            onValueChange = { carModelInput = it },
            label = { Text("Car Model") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = bioInput,
            onValueChange = { bioInput = it },
            label = { Text("Bio - Short Description of Yourself") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val fullName = listOf(firstNameInput, lastNameInput).filter { it.isNotBlank() }.joinToString(" ").ifBlank { person.name }
                val updated = person.copy(
                    name = fullName,
                    phoneNumber = phoneNumberInput,
                    instaHandle = instaHandleInput,
                    carModel = carModelInput,
                    bio = bioInput
                )
                scope.launch {
                    try {
                        updateProfile(updated)
                    } catch (_: Exception) {}
                    onComplete(updated)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IDPromptView(
    onProvideIDNow: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Badge,
            contentDescription = null,
            modifier = Modifier.size(70.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Provide Your ID Card?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Would you like to provide your government-issued ID card right now?\n\nYou do not need to do this immediately, but you must verify your ID in order to participate in a rally.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onProvideIDNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Provide ID Now", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Skip for Now", fontWeight = FontWeight.Bold)
        }
    }
}
