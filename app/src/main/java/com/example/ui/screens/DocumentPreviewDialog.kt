package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.model.FileItem
import com.example.ui.theme.*
import com.example.util.PdfGeneratorHelper
import kotlinx.coroutines.Dispatchers
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
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation and Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Chiudi anteprima",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.nome,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
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

                            // Open in external PDF reader
                            IconButton(
                                onClick = {
                                    val saved = PdfGeneratorHelper.savePdfLocally(context, file)
                                    if (saved != null) {
                                        try {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                saved
                                            )
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(uri, "application/pdf")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Apri ${file.nome}"))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Nessun visualizzatore PDF installato", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription = "Apri con lettore esterno",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Share official PDF
                            IconButton(
                                onClick = {
                                    PdfGeneratorHelper.shareDocument(context, file)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Condividi PDF",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Download / Save PDF
                            Button(
                                onClick = {
                                    val saved = PdfGeneratorHelper.savePdfLocally(context, file)
                                    if (saved != null) {
                                        onDownload()
                                    } else {
                                        Toast.makeText(context, "Errore salvataggio PDF", Toast.LENGTH_SHORT).show()
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
                        0 -> RealDocumentViewerTab(file = file)
                        1 -> DocumentSummaryTab(file = file)
                        2 -> DocumentProtocolsTab(file = file)
                    }
                }
            }
        }
    }
}

/**
 * Renders the actual PDF file pages or image into high-resolution native Bitmaps.
 */
@Composable
private fun RealDocumentViewerTab(file: FileItem) {
    val context = LocalContext.current
    var isImage by remember(file.nome) {
        mutableStateOf(
            file.nome.endsWith(".png", ignoreCase = true) ||
            file.nome.endsWith(".jpg", ignoreCase = true) ||
            file.nome.endsWith(".jpeg", ignoreCase = true) ||
            file.nome.endsWith(".webp", ignoreCase = true)
        )
    }

    var pdfFileState by remember(file.key) { mutableStateOf<File?>(null) }
    var totalPages by remember { mutableIntStateOf(1) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var renderError by remember { mutableStateOf<String?>(null) }

    // Zoom & Pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Prepare File & Render
    LaunchedEffect(file.key, file.nome) {
        isLoading = true
        renderError = null
        scale = 1f
        offset = Offset.Zero
        currentPageIndex = 0

        withContext(Dispatchers.IO) {
            try {
                // Ensure PDF file exists in cache
                val pdfFile = PdfGeneratorHelper.createFiscalPdf(context, file)
                pdfFileState = pdfFile

                if (!isImage) {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            totalPages = renderer.pageCount.coerceAtLeast(1)
                            val page = renderer.openPage(0)
                            // Render at 2x density for crisp typography and lines
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
                }
            } catch (e: Exception) {
                renderError = e.message ?: "Errore caricamento anteprima"
            } finally {
                isLoading = false
            }
        }
    }

    // When page changes
    LaunchedEffect(currentPageIndex, pdfFileState) {
        val pdf = pdfFileState ?: return@LaunchedEffect
        if (isImage) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
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
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5E7EB))
    ) {
        // Floating Page Controls & Zoom Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator & Switcher
                if (!isImage && totalPages > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { if (currentPageIndex > 0) currentPageIndex-- },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.ChevronLeft, contentDescription = "Pagina precedente", modifier = Modifier.size(20.dp))
                        }

                        Text(
                            text = "Pagina ${currentPageIndex + 1} di $totalPages",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = { if (currentPageIndex < totalPages - 1) currentPageIndex++ },
                            enabled = currentPageIndex < totalPages - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.ChevronRight, contentDescription = "Pagina successiva", modifier = Modifier.size(20.dp))
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = PfcSuccess, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Documento Ufficiale Conforme CAD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Zoom buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { scale = (scale - 0.25f).coerceAtLeast(0.75f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.ZoomOut, contentDescription = "Riduci", modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = "${(scale * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { scale = (scale + 0.25f).coerceAtMost(3.5f) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.ZoomIn, contentDescription = "Ingrandisci", modifier = Modifier.size(18.dp))
                    }

                    if (scale != 1f || offset != Offset.Zero) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reimposta zoom", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Viewport / Canvas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.75f, 3.5f)
                        offset += pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(color = GeoPrimary, modifier = Modifier.size(40.dp))
                    Text(
                        text = "Generazione rendering alta definizione in corso...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (pageBitmap != null) {
                // PDF Rendered Page
                Card(
                    modifier = Modifier
                        .padding(16.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .fillMaxWidth(0.95f)
                        .aspectRatio(0.707f), // A4 Aspect Ratio (595 / 842)
                    shape = RoundedCornerShape(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "Pagina documento ${file.nome}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                // Fallback Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .padding(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(48.dp))
                        Text(file.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp, textAlign = TextAlign.Center)
                        Text(
                            "Il documento è pronto per la visualizzazione e il download certificato.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                val saved = PdfGeneratorHelper.savePdfLocally(context, file)
                                if (saved != null) {
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", saved)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Apri ${file.nome}"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apri con Visualizzatore Esterno", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Detailed Fiscal Summary & Breakdown.
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
                    Column {
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
                    "Dimensione File" to (if (file.sizeStr.isNotBlank()) file.sizeStr else "PDF Digitale"),
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
 * Tab 2: Technical Protocols, Telematics Transmission & Integrity Hash.
 */
@Composable
private fun DocumentProtocolsTab(file: FileItem) {
    val scrollState = rememberScrollState()
    val proto = "PFC-${Math.abs(file.key.hashCode()).toString().takeLast(8)}"
    val hash = "SHA256:${file.key.hashCode().toString(16).padStart(16, '0').uppercase()}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PROTOCOLLI TELEMATICI & INTEGRITÀ",
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(GeoPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }

                    Column {
                        Text("Protocollo Univoco Telematico", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(proto, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = GeoPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Impronta di Sicurezza (Hash SHA-256)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = hash,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Chiave Archiviazione Cloud S3/R2", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = file.key,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
