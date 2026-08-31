package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
    isRemindersEnabled: Boolean,
    isRemindDocumentsEnabled: Boolean,
    isRemindMessagesEnabled: Boolean,
    isRemindDeadlinesEnabled: Boolean,
    reminderIntervalMin: Int,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggleReminders: (Boolean) -> Unit,
    onToggleRemindDocuments: (Boolean) -> Unit,
    onToggleRemindMessages: (Boolean) -> Unit,
    onToggleRemindDeadlines: (Boolean) -> Unit,
    onSetReminderInterval: (Int) -> Unit,
    onTestDocumentNotification: () -> Unit,
    onTestMessageNotification: () -> Unit,
    onTestDeadlineNotification: () -> Unit,
    onSimulateStudioDocument: () -> Unit,
    onSimulateStudioMessage: () -> Unit,
    onDismiss: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onSendTestPush: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showIntervalDropdown by remember { mutableStateOf(false) }

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
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GeoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.name ?: "PF").take(2).uppercase(),
                            color = Color.White,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            color = GeoPrimaryContainer,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = if (user?.role == "admin") "Amministratore" else "Cliente Attivo",
                                color = GeoOnPrimaryContainer,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // === LOCAL NOTIFICATION SYSTEM & STUDIO REMINDERS CARD ===
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("local_notification_settings_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = GeoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "PROMEMORIA LOCALI STUDIO",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            color = if (hasNotificationPermission && isRemindersEnabled) PfcSuccessSoft else PfcWarningSoft,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (hasNotificationPermission && isRemindersEnabled) "Attivi" else "Non attivi",
                                color = if (hasNotificationPermission && isRemindersEnabled) PfcSuccess else PfcWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // System Permission Banner (if needed)
                    if (!hasNotificationPermission) {
                        Surface(
                            color = PfcWarningSoft,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PfcWarning.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = PfcWarning, modifier = Modifier.size(22.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Permesso Notifiche Richiesto", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PfcWarning)
                                    Text("Consenti all'app di mostrare notifiche per ricevere promemoria di F24 e messaggi.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = onRequestPermission,
                                    colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Consenti", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Main Master Switch: Automatic Reminders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Promemoria Automatici", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Ricevi notifiche quando sono disponibili nuovi documenti o messaggi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isRemindersEnabled,
                            onCheckedChange = { onToggleReminders(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GeoPrimary
                            )
                        )
                    }

                    if (isRemindersEnabled) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Sub-option: Nuovi Documenti & F24
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Nuovi Modelli F24 e Documenti", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("Avviso per F24, dichiarazioni e bilanci", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isRemindDocumentsEnabled,
                                onCheckedChange = { onToggleRemindDocuments(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GeoPrimary)
                            )
                        }

                        // Sub-option: Messaggi & Richieste
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Messaggi & Richieste dello Studio", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("Avviso per comunicazioni e richieste file", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isRemindMessagesEnabled,
                                onCheckedChange = { onToggleRemindMessages(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GeoPrimary)
                            )
                        }

                        // Sub-option: Scadenze Fiscali
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(18.dp))
                                Column {
                                    Text("Scadenze Fiscali e Tributarie", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                    Text("Promemoria per versamenti in arrivo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = isRemindDeadlinesEnabled,
                                onCheckedChange = { onToggleRemindDeadlines(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = GeoPrimary)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Interval Frequency Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Frequenza Controllo Promemoria", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Box {
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showIntervalDropdown = true },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val label = when (reminderIntervalMin) {
                                            15 -> "Ogni 15 Minuti (Demo / Frequente)"
                                            60 -> "Ogni Ora (Consigliato)"
                                            360 -> "Ogni 6 Ore"
                                            1440 -> "Una volta al Giorno"
                                            else -> "$reminderIntervalMin Minuti"
                                        }
                                        Text(label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showIntervalDropdown,
                                    onDismissRequest = { showIntervalDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Ogni 15 Minuti (Demo / Frequente)") },
                                        onClick = {
                                            onSetReminderInterval(15)
                                            showIntervalDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ogni Ora (Consigliato)") },
                                        onClick = {
                                            onSetReminderInterval(60)
                                            showIntervalDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Ogni 6 Ore") },
                                        onClick = {
                                            onSetReminderInterval(360)
                                            showIntervalDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Una volta al Giorno") },
                                        onClick = {
                                            onSetReminderInterval(1440)
                                            showIntervalDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        // Live Testing Buttons
                        Text(
                            text = "TEST NOTIFICHE LOCALI & SIMULAZIONE STUDIO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onTestDocumentNotification,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test F24", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onTestMessageNotification,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Messaggio", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = onTestDeadlineNotification,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Scadenza", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Simulate Real Incoming Studio Upload & Notification
                        Button(
                            onClick = onSimulateStudioDocument,
                            colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simula Nuovo Documento dallo Studio", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onSimulateStudioMessage,
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, GeoPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.MarkEmailUnread, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simula Richiesta File dal Commercialista", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Push Notifications Diagnostics Card (FCM)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            text = "CANALE SERVER PUSH (FCM)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )

                        Surface(
                            color = if (fcmStatus?.fcmEnabled == true) PfcSuccessSoft else PfcWarningSoft,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (fcmStatus?.fcmEnabled == true) "Attivo & Connesso" else "In attesa",
                                color = if (fcmStatus?.fcmEnabled == true) PfcSuccess else PfcWarning,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "Il dispositivo è registrato per ricevere notifiche push remote dallo studio PFC.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onSendTestPush,
                        enabled = !fcmTesting,
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                        shape = RoundedCornerShape(50),
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
                            Text("Invia Push Server di Prova", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Theme Preferences & Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "PREFERENZE APPLICAZIONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
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
                                tint = GeoPrimary
                            )
                            Column {
                                Text("Modalità Scura", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Attiva interfaccia scura", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GeoPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column {
                                Text("Versione App", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text("Portale PFC Native Android v2.2 (con Notifiche Locali)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                border = BorderStroke(1.dp, PfcDanger),
                shape = RoundedCornerShape(50)
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
            shape = RoundedCornerShape(24.dp),
            icon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = PfcDanger) },
            title = { Text("Conferma Disconnessione") },
            text = { Text("Sei sicuro di voler uscire dal Portale PFC?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PfcDanger),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Disconnetti", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Annulla", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
