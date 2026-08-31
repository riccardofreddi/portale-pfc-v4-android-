package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FcmStatusResponse
import com.example.data.model.User
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    user: User?,
    isDarkMode: Boolean,
    fcmStatus: FcmStatusResponse?,
    fcmTesting: Boolean,
    onDismiss: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onSendTestPush: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Title
            Text(
                text = "Impostazioni & Profilo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // User Info Section Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(PfcNavyDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.name ?: "PF").take(2).uppercase(),
                            color = PfcAmber,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.name ?: "Cliente PFC",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "@${user?.username ?: "utente"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = PfcSlate
                        )
                        Surface(
                            color = PfcAmberSoft,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = if (user?.role == "admin") "Amministratore" else "Cliente Attivo",
                                color = PfcAmberDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Push Notifications Diagnostics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "STATO NOTIFICHE PUSH (FCM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PfcSlateLight,
                            fontWeight = FontWeight.Bold
                        )

                        Surface(
                            color = if (fcmStatus?.fcmEnabled == true) PfcSuccessSoft else PfcWarningSoft,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (fcmStatus?.fcmEnabled == true) "Attivo & Connesso" else "In attesa",
                                color = if (fcmStatus?.fcmEnabled == true) PfcSuccess else PfcWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "Il dispositivo è configurato per ricevere notifiche su nuove scadenze fiscali, modelli F24 e messaggi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PfcSlate
                    )

                    Button(
                        onClick = onSendTestPush,
                        enabled = !fcmTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = PfcAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_test_push_btn")
                    ) {
                        if (fcmTesting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Invia Notifica Push di Prova", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Theme Preferences & Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "PREFERENZE APPLICAZIONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = PfcSlateLight,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                                contentDescription = null,
                                tint = PfcAmber
                            )
                            Column {
                                Text("Modalità Scura", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Attiva interfaccia scura", fontSize = 12.sp, color = PfcSlate)
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(checkedThumbColor = PfcAmber, checkedTrackColor = PfcAmberSoft)
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = PfcSlate)
                            Column {
                                Text("Versione App", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Portale PFC Native Android v2.1", fontSize = 12.sp, color = PfcSlate)
                            }
                        }
                    }
                }
            }

            // Logout Button
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PfcDanger),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, tint = PfcDanger, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Disconnetti dall'Account", color = PfcDanger, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = PfcDanger) },
            title = { Text("Conferma Disconnessione") },
            text = { Text("Sei sicuro di voler uscire dal Portale PFC?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PfcDanger)
                ) {
                    Text("Disconnetti", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Annulla", color = PfcSlate)
                }
            }
        )
    }
}
