package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    loading: Boolean,
    errorMessage: String?,
    onLogin: (username: String, pass: String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Hero Studio Monogram Badge
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(GeoPrimary, GeoOnPrimaryContainer)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PF",
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Portale PFC",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Area riservata clienti e gestione documentale",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Error Message Card
                AnimatedVisibility(visible = !errorMessage.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = PfcDangerSoft),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = PfcDanger
                            )
                            Text(
                                text = errorMessage ?: "",
                                color = PfcDanger,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Input: Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nome Utente / Codice Fiscale") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (username.isNotEmpty()) {
                            IconButton(onClick = { username = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Cancella", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input: Password
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = "Mostra password",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        onLogin(username, password)
                    })
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        onLogin(username, password)
                    },
                    enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("login_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeoPrimary,
                        disabledContainerColor = GeoPrimary.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(50)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                    } else {
                        Text(
                            text = "Accedi al Portale",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))

                // Quick Demo Accounts Selector
                Text(
                    text = "OPPURE PROVA UN ACCOUNT DIMOSTRATIVO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                val demoAccounts = listOf(
                    Triple("rossi", "demo123", "Marco Rossi - Rossi & Co. Srl"),
                    Triple("bianchi", "demo123", "Studio Architettura Bianchi"),
                    Triple("demo", "demo123", "Cliente Dimostrativo PFC")
                )

                demoAccounts.forEach { (usr, pwd, desc) ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                username = usr
                                password = pwd
                                onLogin(usr, pwd)
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = desc,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Username: @$usr",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = GeoPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            // Footer note
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Studio PFC • Piattaforma Fiscale Clienti v2.0",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

