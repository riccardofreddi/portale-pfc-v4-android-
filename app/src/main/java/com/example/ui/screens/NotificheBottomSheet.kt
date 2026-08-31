package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CachedNotificaEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificheBottomSheet(
    notifiche: List<CachedNotificaEntity>,
    onDismiss: () -> Unit,
    onMarkAsRead: (id: String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onClearRead: () -> Unit
) {
    val unreadCount = notifiche.count { !it.letta }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = PfcAmber
                    )
                    Text(
                        text = "Notifiche",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (unreadCount > 0) {
                    TextButton(
                        onClick = onMarkAllAsRead,
                        colors = ButtonDefaults.textButtonColors(contentColor = PfcAmber),
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Segna tutte lette", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            if (notifiche.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Nessuna notifica",
                    description = "Non ci sono nuove comunicazioni o avvisi dallo studio."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(notifiche, key = { it.id }) { notif ->
                        NotificaItemRow(
                            notif = notif,
                            onMarkAsRead = { onMarkAsRead(notif.id) }
                        )
                    }

                    if (notifiche.any { it.letta }) {
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = onClearRead,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = PfcDanger, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Elimina notifiche lette", color = PfcDanger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificaItemRow(
    notif: CachedNotificaEntity,
    onMarkAsRead: () -> Unit
) {
    val (icon, bgColor, tintColor) = when (notif.tipo.lowercase()) {
        "documento_nuovo" -> Triple(Icons.Filled.Description, PfcSuccessSoft, PfcSuccess)
        "messaggio" -> Triple(Icons.Filled.Chat, PfcInfoSoft, PfcInfo)
        "avviso" -> Triple(Icons.Filled.Warning, PfcWarningSoft, PfcWarning)
        "richiesta_upload" -> Triple(Icons.Filled.UploadFile, PfcPurpleSoft, PfcPurple)
        "scadenza" -> Triple(Icons.Filled.Schedule, PfcDangerSoft, PfcDanger)
        "upload_confermato" -> Triple(Icons.Filled.CheckCircle, PfcSuccessSoft, PfcSuccess)
        else -> Triple(Icons.Filled.Notifications, PfcSurfaceAltLight, PfcSlate)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { if (!notif.letta) onMarkAsRead() },
        colors = CardDefaults.cardColors(
            containerColor = if (!notif.letta) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!notif.letta) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!notif.letta) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PfcAmber)
                            )
                        }
                        Text(
                            text = notif.titolo,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (!notif.letta) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = notif.dataCreazione,
                        fontSize = 10.sp,
                        color = PfcSlate
                    )
                }

                if (!notif.corpo.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = notif.corpo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            if (!notif.letta) {
                IconButton(
                    onClick = onMarkAsRead,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Segna come letta",
                        tint = PfcAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
