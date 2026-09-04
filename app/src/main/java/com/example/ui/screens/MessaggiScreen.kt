package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CachedMessaggioEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import java.io.File
import java.io.FileOutputStream

@Composable
fun MessaggiScreen(
    activeTab: Int,
    attiviList: List<CachedMessaggioEntity>,
    archiviatiList: List<CachedMessaggioEntity>,
    expandedId: String?,
    onTabChange: (Int) -> Unit,
    onToggleExpand: (String) -> Unit,
    onToggleArchive: (CachedMessaggioEntity) -> Unit,
    onSubmitUploadFile: (msgId: String, file: File) -> Unit,
    onMarkAllAsRead: () -> Unit = {},
    onToggleRead: ((CachedMessaggioEntity) -> Unit)? = null
) {
    val currentList = if (activeTab == 0) attiviList else archiviatiList
    val unreadCount = attiviList.count { !it.letto }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Top Tab Selector (Attivi / Archiviati)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GeoPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = GeoPrimary,
                            height = 3.dp
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { onTabChange(0) },
                        modifier = Modifier.testTag("tab_messaggi_attivi"),
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Messaggi Attivi",
                                    color = if (activeTab == 0) GeoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (activeTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                if (unreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
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
                        modifier = Modifier.testTag("tab_messaggi_archiviati"),
                        text = {
                            Text(
                                "Archiviati (${archiviatiList.size})",
                                color = if (activeTab == 1) GeoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (activeTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }

            // Quick Info Banner + "Segna tutti come letti" action if unread messages exist
            Surface(
                color = GeoPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mail,
                            contentDescription = null,
                            tint = GeoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Comunicazioni ufficiali dello Studio PFC. Puoi leggere i dettagli e inviare gli allegati richiesti.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }

                    if (activeTab == 0 && unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onMarkAllAsRead,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GeoPrimary),
                            modifier = Modifier.testTag("mark_all_read_messages_button")
                        ) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Segna letti", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Messages List
            if (currentList.isEmpty()) {
                EmptyStateView(
                    icon = if (activeTab == 0) Icons.Outlined.ChatBubbleOutline else Icons.Outlined.Archive,
                    title = if (activeTab == 0) "Nessun messaggio attivo" else "Nessun messaggio archiviato",
                    description = if (activeTab == 0)
                        "Tutte le comunicazioni dello Studio PFC sono state lette o archiviate."
                    else
                        "I messaggi archiviati appariranno in questa sezione."
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("messaggi_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(currentList, key = { it.id }) { msg ->
                        val isExpanded = expandedId == msg.id
                        MessaggioCard(
                            msg = msg,
                            isExpanded = isExpanded,
                            onCardClick = { onToggleExpand(msg.id) },
                            onArchiveClick = { onToggleArchive(msg) },
                            onToggleRead = { onToggleRead?.invoke(msg) },
                            onSubmitUploadFile = { file -> onSubmitUploadFile(msg.id, file) }
                        )
                    }
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
    onToggleRead: (() -> Unit)? = null,
    onSubmitUploadFile: (file: File) -> Unit
) {
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var showOptionalUpload by remember { mutableStateOf(false) }
    var showFullBody by remember { mutableStateOf(false) }

    val isUnread = !msg.letto && !msg.archiviato

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val file = uriToFile(context, uri)
            if (file != null) {
                selectedFile = file
                selectedFileName = file.name
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .testTag("msg_card_${msg.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 3.dp else 1.dp),
        border = BorderStroke(
            if (isUnread) 1.5.dp else 1.dp,
            if (isUnread) GeoPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Unread decorative indicator bar
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(GeoPrimary)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header Row: Studio PFC info + Date + Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Category / Status Icon
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        msg.haRisposta -> PfcSuccess.copy(alpha = 0.15f)
                                        msg.richiedeUpload -> PfcWarning.copy(alpha = 0.15f)
                                        isUnread -> GeoPrimary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    msg.haRisposta -> Icons.Filled.CheckCircle
                                    msg.richiedeUpload -> Icons.Filled.UploadFile
                                    isUnread -> Icons.Filled.Mail
                                    else -> Icons.Outlined.MailOutline
                                },
                                contentDescription = null,
                                tint = when {
                                    msg.haRisposta -> PfcSuccess
                                    msg.richiedeUpload -> PfcWarning
                                    isUnread -> Color.White
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            // Subtitle: Studio label and date
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "STUDIO PFC",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = GeoPrimary,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = "•",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = msg.dataInvio,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Message Title
                            Text(
                                text = msg.titolo,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    // Action Buttons (Mark as Read / Archive)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onToggleRead != null) {
                            IconButton(
                                onClick = onToggleRead,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (msg.letto) Icons.Outlined.MarkEmailUnread else Icons.Filled.Drafts,
                                    contentDescription = if (msg.letto) "Segna da leggere" else "Segna come letto",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
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
                }

                // Status Badges
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isUnread) {
                        Surface(
                            color = GeoPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "Nuovo",
                                color = GeoPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    if (msg.richiedeUpload && !msg.haRisposta) {
                        Surface(
                            color = PfcWarning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.UploadFile,
                                    contentDescription = null,
                                    tint = PfcWarning,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Richiesta Documento",
                                    color = PfcWarning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (msg.haRisposta) {
                        Surface(
                            color = PfcSuccess.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = PfcSuccess,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Documento Inviato",
                                    color = PfcSuccess,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // MESSAGE BODY - DISPLAYED CLEARLY & HIGH CONTRAST
                val messageText = when {
                    msg.corpo.isNotBlank() -> msg.corpo
                    !msg.uploadDescrizione.isNullOrBlank() -> msg.uploadDescrizione
                    else -> "Comunicazione ufficiale dallo Studio PFC. Consulta la documentazione allegata."
                }

                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = messageText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (showFullBody || isExpanded) Int.MAX_VALUE else 5,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 22.sp,
                            modifier = Modifier.testTag("msg_body_${msg.id}")
                        )

                        if (messageText.length > 180) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (showFullBody || isExpanded) "Mostra meno ▲" else "Leggi tutto ▼",
                                color = GeoPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { showFullBody = !showFullBody }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }

                // Studio Attachment reference if available
                if (!msg.allegatoNome.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AttachFile,
                                contentDescription = null,
                                tint = GeoPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Allegato dallo Studio: ${msg.allegatoNome}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // =========================================================================
                // UPLOAD SECTION: PROMINENT AND ACCESSIBLE FOR SENDING THE REQUESTED FILE
                // =========================================================================
                val shouldShowUpload = msg.richiedeUpload || showOptionalUpload

                if (shouldShowUpload) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("upload_section_${msg.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.haRisposta) {
                                PfcSuccess.copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (msg.haRisposta) PfcSuccess.copy(alpha = 0.5f) else GeoPrimary.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section Header
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (msg.haRisposta) Icons.Filled.CheckCircle else Icons.Filled.UploadFile,
                                    contentDescription = null,
                                    tint = if (msg.haRisposta) PfcSuccess else GeoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (msg.haRisposta) "Documento Inviato allo Studio" else "Richiesta Documento dallo Studio",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Request instructions
                            val uploadDescription = msg.uploadDescrizione
                                ?: "Si richiede l'invio della documentazione richiesta (es. documento d'identità, visura o ricevuta)."

                            Text(
                                text = uploadDescription,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            // If already answered, show confirmation & option to re-send
                            if (msg.haRisposta && selectedFile == null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(PfcSuccess.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = PfcSuccess,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (!msg.allegatoNome.isNullOrBlank())
                                            "File inviato: ${msg.allegatoNome}"
                                        else
                                            "Risposta inviata allo Studio PFC con successo",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                TextButton(
                                    onClick = { filePickerLauncher.launch("*/*") },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Invia un altro allegato", fontSize = 12.sp, color = GeoPrimary)
                                }
                            } else {
                                // Native File Selector from Device
                                if (selectedFile == null) {
                                    Button(
                                        onClick = {
                                            filePickerLauncher.launch("*/*")
                                        },
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_pick_file_${msg.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.FolderOpen,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Scegli File dal Dispositivo",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    // Selected file preview box
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Description,
                                                    contentDescription = null,
                                                    tint = GeoPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = selectedFileName ?: "File selezionato",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    val sizeKb = (selectedFile?.length() ?: 0L) / 1024
                                                    Text(
                                                        text = "$sizeKb KB • Pronto per l'invio",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = {
                                                    selectedFile = null
                                                    selectedFileName = null
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Rimuovi file",
                                                    tint = PfcDanger,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Primary Submission Button
                                    Button(
                                        onClick = {
                                            selectedFile?.let { f ->
                                                onSubmitUploadFile(f)
                                                selectedFile = null
                                                selectedFileName = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("btn_submit_file_${msg.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Send,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Invia Documento allo Studio",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // If this message did not strictly require an upload, provide client affordance to attach a file
                    Spacer(modifier = Modifier.height(10.dp))
                    TextButton(
                        onClick = { showOptionalUpload = true },
                        modifier = Modifier
                            .align(Alignment.Start)
                            .testTag("btn_toggle_attach_${msg.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AttachFile,
                            contentDescription = null,
                            tint = GeoPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Invia un allegato per questo messaggio",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GeoPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val contentResolver = context.contentResolver
        var fileName = "documento_${System.currentTimeMillis()}"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                val foundName = cursor.getString(nameIndex)
                if (!foundName.isNullOrBlank()) fileName = foundName
            }
        }
        val tempFile = File(context.cacheDir, fileName)
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        tempFile
    } catch (_: Exception) {
        null
    }
}
