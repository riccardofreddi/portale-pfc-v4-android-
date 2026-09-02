package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onNotificaClick: (CachedNotificaEntity) -> Unit,
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
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GeoPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = GeoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Notifiche Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (unreadCount > 0) "$unreadCount non lette" else "Tutto aggiornato",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (unreadCount > 0) {
                    TextButton(
                        onClick = onMarkAllAsRead,
                        colors = ButtonDefaults.textButtonColors(contentColor = GeoPrimary),
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Segna lette", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (notifiche.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Nessuna notifica presente",
                    description = "Qui riceverai comunicazioni in tempo reale dallo Studio PFC per nuovi modelli F24, bilanci, messaggi e scadenze."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(notifiche, key = { it.id }) { notif ->
                        NotificaItemRow(
                            notif = notif,
                            onClick = { onNotificaClick(notif) },
                            onMarkAsRead = { onMarkAsRead(notif.id) }
                        )
                    }

                    if (notifiche.any { it.letta }) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = onClearRead,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = PfcDanger, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Rimuovi notifiche già lette", color = PfcDanger, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit
) {
    val lowerTipo = notif.tipo.lowercase()
    val isDocument = lowerTipo.contains("doc") || lowerTipo.contains("f24") || !notif.folder.isNullOrBlank()
    val isMessage = lowerTipo.contains("msg") || lowerTipo.contains("messag") || lowerTipo.contains("upload")
    val isDeadline = lowerTipo.contains("scadenz") || lowerTipo.contains("deadlin")

    val style = when {
        isDocument -> NotificaItemStyle(Icons.Filled.Description, "Fiscale & F24", GeoPrimary, "Apri in Archivio")
        lowerTipo.contains("upload") -> NotificaItemStyle(Icons.Filled.UploadFile, "Richiesta Documento", PfcSapphire, "Invia File")
        isMessage -> NotificaItemStyle(Icons.Filled.Chat, "Comunicazione", GeoPrimary, "Leggi Messaggio")
        isDeadline -> NotificaItemStyle(Icons.Filled.Schedule, "Scadenza Tributaria", PfcDanger, "Verifica Scadenza")
        else -> NotificaItemStyle(Icons.Filled.Notifications, "Avviso Studio", MaterialTheme.colorScheme.onSurfaceVariant, "Visualizza")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("notifica_card_${notif.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (!notif.letta) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (!notif.letta) GeoPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(style.badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = style.icon,
                        contentDescription = null,
                        tint = style.badgeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Content
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
                                        .background(GeoPrimary)
                                )
                            }
                            Surface(
                                color = style.badgeColor.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = style.badgeText,
                                    color = style.badgeColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = notif.dataCreazione,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = notif.titolo,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (!notif.letta) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!notif.corpo.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = notif.corpo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
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
                            tint = GeoPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { onClick() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = style.actionLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeoPrimary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = GeoPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private data class NotificaItemStyle(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badgeText: String,
    val badgeColor: androidx.compose.ui.graphics.Color,
    val actionLabel: String
)
