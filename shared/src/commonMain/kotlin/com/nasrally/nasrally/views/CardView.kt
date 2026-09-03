package com.nasrally.nasrally.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nasrally.nasrally.PersonInfo
import com.nasrally.nasrally.SensitiveInfoRow
import com.nasrally.nasrally.loadSensitiveInfo
import com.nasrally.nasrally.saveSensitiveInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CreditCardPreview(
    cardNumber: String,
    cardholderName: String,
    expirationDate: String,
    cvv: String
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1E3C72),
            Color(0xFF2A5298),
            Color(0xFFE52D27)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = "Card Icon",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "PAYMENT CARD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            Text(
                text = if (cardNumber.isBlank()) "•••• •••• •••• ••••" else cardNumber,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CARDHOLDER",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (cardholderName.isBlank()) "YOUR NAME" else cardholderName.uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPIRES",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (expirationDate.isBlank()) "MM/YY" else expirationDate,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun CardView(person: PersonInfo) {
    var ccnString by remember { mutableStateOf("") }
    var cvvString by remember { mutableStateOf("") }
    var exp by remember { mutableStateOf("") }
    var cardholderName by remember(person.name) { mutableStateOf(person.name) }

    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val cleanCCN = ccnString.replace(" ", "")
    val cleanEXP = exp.replace("/", "")
    val isFormValid = cleanCCN.length in 15..16 &&
            cvvString.length in 3..4 &&
            cleanEXP.length == 4 &&
            cardholderName.isNotBlank()

    LaunchedEffect(person.id) {
        if (!person.isTestUser) {
            isLoading = true
            val existing = loadSensitiveInfo()
            if (existing != null) {
                if (existing.ccn != 0L) {
                    val rawCCN = existing.ccn.toString()
                    val formatted = rawCCN.chunked(4).joinToString(" ")
                    ccnString = formatted
                }
                if (existing.cvv != 0) {
                    cvvString = existing.cvv.toString().padStart(3, '0')
                }
                exp = existing.exp
                if (existing.name.isNotBlank()) cardholderName = existing.name
            }
            isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Payment Info",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            CreditCardPreview(
                cardNumber = ccnString,
                cardholderName = cardholderName,
                expirationDate = exp,
                cvv = cvvString
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = cardholderName,
                        onValueChange = { cardholderName = it },
                        label = { Text("Cardholder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ccnString,
                        onValueChange = { newValue ->
                            val digits = newValue.filter { it.isDigit() }
                            val formatted = digits.chunked(4).joinToString(" ")
                            if (formatted.length <= 19) {
                                ccnString = formatted
                            }
                        },
                        label = { Text("Card Number") },
                        placeholder = { Text("1234 5678 1234 5678") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = exp,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }
                                var formatted = digits
                                if (digits.length >= 2) {
                                    val month = digits.take(2)
                                    val year = digits.drop(2).take(2)
                                    formatted = if (year.isNotEmpty()) "$month/$year" else month
                                }
                                if (formatted.length <= 5) {
                                    exp = formatted
                                }
                            },
                            label = { Text("Expiration Date") },
                            placeholder = { Text("MM/YY") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = cvvString,
                            onValueChange = { newValue ->
                                val digits = newValue.filter { it.isDigit() }
                                if (digits.length <= 4) {
                                    cvvString = digits
                                }
                            },
                            label = { Text("CVV") },
                            placeholder = { Text("123") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (person.isTestUser) {
                        toastMessage = "Card details are not saved for Test User."
                        isError = false
                        showToast = true
                        return@Button
                    }
                    isSaving = true
                    scope.launch {
                        try {
                            val ccnVal = cleanCCN.toLongOrNull() ?: 0L
                            val cvvVal = cvvString.toIntOrNull() ?: 0
                            val row = SensitiveInfoRow(
                                id = person.id,
                                ccn = ccnVal,
                                cvv = cvvVal,
                                exp = exp,
                                name = cardholderName
                            )
                            saveSensitiveInfo(row)
                            toastMessage = "Card details saved successfully!"
                            isError = false
                            showToast = true
                            delay(2000)
                            showToast = false
                        } catch (e: Exception) {
                            toastMessage = "Failed to save: ${e.message}"
                            isError = true
                            showToast = true
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = isFormValid && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Card Details")
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        AnimatedVisibility(
            visible = showToast,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = toastMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
