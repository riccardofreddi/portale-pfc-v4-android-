package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CachedMessaggioEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun MessaggiScreen(
    activeTab: Int,
    attiviList: List<CachedMessaggioEntity>,
    archiviatiList: List<CachedMessaggioEntity>,
    expandedId: String?,
    onTabChange: (Int) -> Unit,
    onToggleExpand: (String) -> Unit,
    onToggleArchive: (CachedMessaggioEntity) -> Unit,
    onSubmitUpload: (msgId: String, fileName: String) -> Unit
) {
    val currentList = if (activeTab == 0) attiviList else archiviatiList
    val unreadCount = attiviList.count { !it.letto }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Tab Selector (Attivi / Archiviati)
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GeoPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                        color = GeoPrimary
                    )
                }
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { onTabChange(0) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Messaggi Attivi",
                                color = if (activeTab == 0) GeoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(PfcDanger),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { onTabChange(1) },
                    text = {
                        Text(
                            "Archiviati (${archiviatiList.size})",
                            color = if (activeTab == 1) GeoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        // Messages List
        if (currentList.isEmpty()) {
            EmptyStateView(
                icon = if (activeTab == 0) Icons.Outlined.ChatBubbleOutline else Icons.Outlined.Archive,
                title = if (activeTab == 0) "Nessun messaggio attivo" else "Nessun messaggio archiviato",
                description = if (activeTab == 0) "Tutte le comunicazioni dello studio sono state lette o archiviate." else "I messaggi archiviati appariranno in questa sezione."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(currentList, key = { it.id }) { msg ->
                    val isExpanded = expandedId == msg.id
                    MessaggioCard(
                        msg = msg,
                        isExpanded = isExpanded,
                        onCardClick = { onToggleExpand(msg.id) },
                        onArchiveClick = { onToggleArchive(msg) },
                        onSubmitUpload = { fileName -> onSubmitUpload(msg.id, fileName) }
                    )
                }
            }
        }
    }
}

@Composable
fun MessaggioCard(
    msg: CachedMessaggioEntity,
    isExpanded: Boolean,
    onCardClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onSubmitUpload: (fileName: String) -> Unit
) {
    var selectedReplyFile by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onCardClick() }
            .testTag("msg_card_${msg.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (!msg.letto && !msg.archiviato) GeoPrimaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (!msg.letto && !msg.archiviato) GeoPrimary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (!msg.letto && !msg.archiviato) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GeoPrimary)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = msg.titolo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (!msg.letto) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = msg.dataInvio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onArchiveClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (msg.archiviato) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                        contentDescription = if (msg.archiviato) "Ripristina" else "Archivia",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Badges row
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (msg.richiedeUpload && !msg.haRisposta) {
                    StatusBadge(status = "richiede_upload")
                }
                if (msg.haRisposta) {
                    StatusBadge(status = "risposto")
                }
            }

            // Message Preview / Full Body
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = msg.corpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            // Expanded Actions: Upload Request section
            AnimatedVisibility(
                visible = isExpanded && msg.richiedeUpload && !msg.haRisposta,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = GeoPrimaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.UploadFile,
                                contentDescription = null,
                                tint = GeoOnPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Richiesta Documento dallo Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = GeoOnPrimaryContainer
                            )
                        }

                        if (!msg.uploadDescrizione.isNullOrBlank()) {
                            Text(
                                text = msg.uploadDescrizione,
                                fontSize = 12.sp,
                                color = GeoOnPrimaryContainer
                            )
                        }

                        // Simulated file upload selector
                        if (selectedReplyFile == null) {
                            OutlinedButton(
                                onClick = {
                                    selectedReplyFile = "Documento_Richiesto_${msg.id.take(4)}.pdf"
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Seleziona File da Inviare", fontSize = 13.sp)
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selectedReplyFile!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = GeoOnPrimaryContainer
                                )
                                IconButton(
                                    onClick = { selectedReplyFile = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Rimuovi", modifier = Modifier.size(14.dp))
                                }
                            }

                            Button(
                                onClick = {
                                    onSubmitUpload(selectedReplyFile!!)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Invia Risposta con Documento", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

