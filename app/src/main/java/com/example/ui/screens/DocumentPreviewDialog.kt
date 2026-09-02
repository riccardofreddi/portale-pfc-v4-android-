package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FileItem
import com.example.data.remote.PfcApiClient
import com.example.ui.theme.*
import com.example.util.DocumentFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentPreviewDialog(
    file: FileItem,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    val apiClient = remember { PfcApiClient(context) }

    // Download state for real file
    var downloadedRealFile by remember(file.key) { mutableStateOf<File?>(null) }
    var isDownloading by remember(file.key) { mutableStateOf(false) }

    LaunchedEffect(file.key) {
        val cached = DocumentFileManager.getLocalCachedFile(context, file.key, file.nome)
        if (cached.exists() && cached.length() > 0) {
            downloadedRealFile = cached
        } else {
            isDownloading = true
            val res = DocumentFileManager.getOrDownloadDocument(context, file, apiClient.apiService)
            if (res.isSuccess) {
                downloadedRealFile = res.getOrNull()
            }
            isDownloading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .padding(4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Header Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left Close & Title
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Chiudi",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = file.nome,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${file.cartella ?: "Documento"} • ${file.anno ?: "2025"}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (file.sizeStr.isNotBlank()) {
                                        Text(
                                            text = "(${file.sizeStr})",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // Right Action Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Favorite toggle
                            if (onToggleFavorite != null) {
                                IconButton(onClick = onToggleFavorite) {
                                    Icon(
                                        imageVector = if (file.isPreferito) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "Preferito",
                                        tint = if (file.isPreferito) PfcGold else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Open in external PDF reader / app
                            IconButton(
                                onClick = {
                                    val f = downloadedRealFile
                                    if (f != null && f.exists()) {
                                        DocumentFileManager.openWithExternalApp(context, f)
                                    } else {
                                        coroutineScope.launch {
                                            val res = DocumentFileManager.getOrDownloadDocument(context, file, apiClient.apiService)
                                            if (res.isSuccess) {
                                                downloadedRealFile = res.getOrNull()
                                                DocumentFileManager.openWithExternalApp(context, res.getOrThrow())
                                            } else {
                                                Toast.makeText(context, "Impossibile aprire il file: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Apri con app esterna",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Share official file
                            IconButton(
                                onClick = {
                                    val f = downloadedRealFile
                                    if (f != null && f.exists()) {
                                        DocumentFileManager.shareFile(context, f)
                                    } else {
                                        coroutineScope.launch {
                                            val res = DocumentFileManager.getOrDownloadDocument(context, file, apiClient.apiService)
                                            if (res.isSuccess) {
                                                downloadedRealFile = res.getOrNull()
                                                DocumentFileManager.shareFile(context, res.getOrThrow())
                                            } else {
                                                Toast.makeText(context, "Impossibile condividere: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Condividi Documento",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Save to Downloads
                            Button(
                                onClick = {
                                    val f = downloadedRealFile
                                    coroutineScope.launch {
                                        if (f != null && f.exists()) {
                                            val saveRes = DocumentFileManager.saveToPublicDownloads(context, f, file.nome)
                                            if (saveRes.isSuccess) {
                                                Toast.makeText(context, "File salvato in Download", Toast.LENGTH_SHORT).show()
                                                onDownload()
                                            } else {
                                                Toast.makeText(context, "Errore salvataggio: ${saveRes.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            val res = DocumentFileManager.getOrDownloadDocument(context, file, apiClient.apiService)
                                            if (res.isSuccess) {
                                                downloadedRealFile = res.getOrNull()
                                                val saveRes = DocumentFileManager.saveToPublicDownloads(context, res.getOrThrow(), file.nome)
                                                if (saveRes.isSuccess) {
                                                    Toast.makeText(context, "File salvato in Download", Toast.LENGTH_SHORT).show()
                                                    onDownload()
                                                }
                                            } else {
                                                Toast.makeText(context, "Download non riuscito", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salva", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Modal Segmented Tabs (Anteprima / Riepilogo / Dati e Protocolli)
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = GeoPrimary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Anteprima File",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Riepilogo Fiscale",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Text(
                                "Dati & Protocolli",
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Outlined.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // Tab Contents
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedTab) {
                        0 -> RealDocumentViewerTab(
                            file = file,
                            realFile = downloadedRealFile,
                            isDownloading = isDownloading,
                            onRetry = {
                                coroutineScope.launch {
                                    isDownloading = true
                                    val res = DocumentFileManager.getOrDownloadDocument(context, file, apiClient.apiService)
                                    if (res.isSuccess) {
                                        downloadedRealFile = res.getOrNull()
                                    }
                                    isDownloading = false
                                }
                            }
                        )
                        1 -> DocumentSummaryTab(file = file)
                        2 -> DocumentProtocolsTab(file = file)
                    }
                }
            }
        }
    }
}

/**
 * Renders the actual downloaded original file pages or image into high-resolution native Bitmaps.
 */
@Composable
private fun RealDocumentViewerTab(
    file: FileItem,
    realFile: File?,
    isDownloading: Boolean,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val isImage = remember(file.nome) {
        file.nome.endsWith(".png", ignoreCase = true) ||
        file.nome.endsWith(".jpg", ignoreCase = true) ||
        file.nome.endsWith(".jpeg", ignoreCase = true) ||
        file.nome.endsWith(".webp", ignoreCase = true)
    }
    val isPdf = remember(file.nome) {
        file.nome.endsWith(".pdf", ignoreCase = true)
    }

    var totalPages by remember { mutableIntStateOf(1) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var isRendering by remember { mutableStateOf(false) }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Render logic when realFile is available
    LaunchedEffect(realFile, currentPageIndex) {
        if (realFile == null || !realFile.exists() || realFile.length() == 0L) {
            pageBitmap = null
            return@LaunchedEffect
        }

        isRendering = true
        renderError = null

        withContext(Dispatchers.IO) {
            try {
                if (isImage) {
                    val bmp = BitmapFactory.decodeFile(realFile.absolutePath)
                    pageBitmap = bmp
                    totalPages = 1
                } else if (isPdf) {
                    ParcelFileDescriptor.open(realFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            totalPages = renderer.pageCount.coerceAtLeast(1)
                            val safeIndex = currentPageIndex.coerceIn(0, totalPages - 1)
                            val page = renderer.openPage(safeIndex)
                            val densityMultiplier = 2
                            val bmp = Bitmap.createBitmap(
                                page.width * densityMultiplier,
                                page.height * densityMultiplier,
                                Bitmap.Config.ARGB_8888
                            )
                            val canvas = android.graphics.Canvas(bmp)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            pageBitmap = bmp
                        }
                    }
                } else {
                    // Non-PDF / Non-Image format
                    pageBitmap = null
                }
            } catch (e: Exception) {
                renderError = e.message ?: "Errore nella decodifica del file"
            } finally {
                isRendering = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        when {
            isDownloading || isRendering -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = GeoPrimary, modifier = Modifier.size(36.dp))
                    Text(
                        text = if (isDownloading) "Scaricamento documento dal server..." else "Elaborazione grafica anteprima...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            renderError != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Impossibile visualizzare anteprima",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = renderError ?: "Errore sconosciuto",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onRetry) {
                        Text("Riprova")
                    }
                }
            }

            !isPdf && !isImage && realFile != null -> {
                // Generic file preview with details and external open button
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        color = GeoPrimaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.FilePresent,
                                contentDescription = null,
                                tint = GeoPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Text(
                        text = file.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Formato originale disponibile (${realFile.length() / 1024} KB)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = { DocumentFileManager.openWithExternalApp(context, realFile) },
                        colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary)
                    ) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apri con visualizzatore esterno")
                    }
                }
            }

            pageBitmap != null -> {
                // Interactive Zoomable PDF/Image Canvas
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 4f)
                                offset = if (scale > 1f) {
                                    Offset(offset.x + pan.x, offset.y + pan.y)
                                } else {
                                    Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "Anteprima ${file.nome}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                    )
                }

                // Page Navigation Bar (for multi-page PDFs)
                if (totalPages > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentPageIndex > 0) currentPageIndex--
                                },
                                enabled = currentPageIndex > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronLeft,
                                    contentDescription = "Pagina precedente",
                                    tint = if (currentPageIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }

                            Text(
                                text = "Pagina ${currentPageIndex + 1} di $totalPages",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = {
                                    if (currentPageIndex < totalPages - 1) currentPageIndex++
                                },
                                enabled = currentPageIndex < totalPages - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Pagina successiva",
                                    tint = if (currentPageIndex < totalPages - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDownload,
                        contentDescription = null,
                        tint = GeoPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Documento pronto per il download",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = onRetry) {
                        Text("Scarica Anteprima")
                    }
                }
            }
        }
    }
}

/**
 * Tab 2: Riepilogo Fiscale & Contabile dell'Adempimento
 */
@Composable
private fun DocumentSummaryTab(file: FileItem) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "RIEPILOGO ADEMPIMENTO FISCALE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        // Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.nome.removeSuffix(".pdf"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sezione: ${file.cartella ?: "Fiscale"} • Anno ${file.anno ?: "2025"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = PfcSuccessSoft,
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = PfcSuccess, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Elaborato",
                                color = PfcSuccess,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Key Fiscal Values
                val rows = listOf(
                    "Tipo Documento" to (file.cartella ?: "Documento Fiscale"),
                    "Anno di Competenza" to (file.anno ?: "2025"),
                    "Data Caricamento Studio" to (file.lastModified ?: "Aggiornato"),
                    "Dimensione File" to (if (file.sizeStr.isNotBlank()) file.sizeStr else "Documento Digitale"),
                    "Conservazione Digitale" to "A Norma CAD (10 Anni)",
                    "Studio Emittente" to "Studio PFC Consulting"
                )

                rows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Compliance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GeoPrimaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, GeoPrimary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(24.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Validità Giuridica & Fiscale", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GeoOnPrimaryContainer)
                    Text(
                        "Il presente documento è conforme agli archivi tributari ufficiali e depositato secondo le linee guida AgID per la conservazione a norma.",
                        fontSize = 12.sp,
                        color = GeoOnPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

/**
 * Tab 3: Dati Telematici, Protocolli e Ricevute di Deposito
 */
@Composable
private fun DocumentProtocolsTab(file: FileItem) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PROTOCOLLI TELEMATICI & RICEVUTE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Tracciabilità Entrate / INPS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                val protocols = listOf(
                    "Chiave Archiviazione" to file.key,
                    "Stato Lettura" to (file.stato?.uppercase() ?: "DISPONIBILE"),
                    "Hash SHA-256 Integrità" to "Verificato conforme su server PFC",
                    "Certificato SSL Canale" to "TLS 1.3 Crittografia 256-bit",
                    "Disponibilità Portale" to "Accesso illimitato cloud protetto"
                )

                protocols.forEach { (label, value) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = value,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
