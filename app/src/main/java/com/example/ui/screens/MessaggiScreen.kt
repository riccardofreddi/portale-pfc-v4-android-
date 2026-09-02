package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.CachedMessaggioEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.components.StatusBadge
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
    onSubmitUploadFile: (msgId: String, file: File) -> Unit
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

            // Quick Info Banner: Istruzioni upload richiesti dallo studio
            Surface(
                color = GeoPrimary.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = GeoPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Tocca un messaggio con richiesta per visualizzare i dettagli ed allegare il file richiesto dallo Studio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 16.sp
                    )
                }
            }

            // Messages List
            if (currentList.isEmpty()) {
                EmptyStateView(
                    icon = if (activeTab == 0) Icons.Outlined.ChatBubbleOutline else Icons.Outlined.Archive,
                    title = if (activeTab == 0) "Nessun messaggio attivo" else "Nessun messaggio archiviato",
                    description = if (activeTab == 0)
                        "Tutte le comunicazioni dello studio sono state lette o archiviate."
                    else
                        "I messaggi archiviati appariranno in questa sezione."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(currentList, key = { it.id }) { msg ->
                        val isExpanded = expandedId == msg.id
                        MessaggioCard(
                            msg = msg,
                            isExpanded = isExpanded,
                            onCardClick = { onToggleExpand(msg.id) },
                            onArchiveClick = { onToggleArchive(msg) },
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
    onSubmitUploadFile: (file: File) -> Unit
) {
    val context = LocalContext.current
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
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
            .clickable { onCardClick() }
            .testTag("msg_card_${msg.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) GeoPrimaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 3.dp else 1.dp),
        border = BorderStroke(
            1.dp,
            if (isUnread) GeoPrimary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
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
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isUnread) GeoPrimary else MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isUnread) Icons.Filled.Mail else Icons.Outlined.MailOutline,
                            contentDescription = null,
                            tint = if (isUnread) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = msg.titolo,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.SemiBold,
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
            if ((msg.richiedeUpload && !msg.haRisposta) || msg.haRisposta) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (msg.richiedeUpload && !msg.haRisposta) {
                        StatusBadge(status = "richiede_upload")
                    }
                    if (msg.haRisposta) {
                        StatusBadge(status = "risposto")
                    }
                }
            }

            // Message Body
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = msg.corpo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            // Attachment from studio if available
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
                            text = "Allegato: ${msg.allegatoNome}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Upload Request section
            AnimatedVisibility(
                visible = isExpanded && msg.richiedeUpload && !msg.haRisposta,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.UploadFile,
                                contentDescription = null,
                                tint = GeoPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Richiesta Documento dallo Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (!msg.uploadDescrizione.isNullOrBlank()) {
                            Text(
                                text = msg.uploadDescrizione,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }

                        // Native File Selector from Device
                        if (selectedFile == null) {
                            OutlinedButton(
                                onClick = {
                                    filePickerLauncher.launch("*/*")
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.5.dp, GeoPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scegli File dal Dispositivo", color = GeoPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Description, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(20.dp))
                                        Column {
                                            Text(
                                                text = selectedFileName ?: "File selezionato",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val sizeKb = (selectedFile?.length() ?: 0L) / 1024
                                            Text(
                                                text = "$sizeKb KB",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedFile = null
                                            selectedFileName = null
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "Rimuovi", tint = PfcDanger, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

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
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Invia Documento allo Studio", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
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
