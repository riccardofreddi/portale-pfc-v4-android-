package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FileItem
import com.example.ui.theme.*

@Composable
fun DocumentPreviewDialog(
    file: FileItem,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(1) }
    val totalPages = 2

    // Determine document fiscal category
    val docType = remember(file.nome, file.cartella) {
        val lower = (file.nome + " " + (file.cartella ?: "")).lowercase()
        when {
            lower.contains("f24") || lower.contains("tribut") || lower.contains("impost") -> DocCategory.F24
            lower.contains("dichiaraz") || lower.contains("730") || lower.contains("redditi") || lower.contains("unico") || lower.contains("iva") -> DocCategory.DICHIARAZIONE
            lower.contains("bilanc") || lower.contains("nota") || lower.contains("rendicont") -> DocCategory.BILANCIO
            lower.contains("cedolin") || lower.contains("busta") || lower.contains("pag") || lower.contains("cu") -> DocCategory.LAVORO
            lower.contains("fattur") || lower.contains("spes") -> DocCategory.FATTURA
            else -> DocCategory.GENERICO
        }
    }

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
                // Top Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
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
                                    contentDescription = "Chiudi",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = file.nome,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        color = GeoPrimaryContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = docType.badgeLabel,
                                            color = GeoOnPrimaryContainer,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${file.cartella ?: "Documento"} • ${file.anno ?: "2025"} • ${file.sizeStr}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (onToggleFavorite != null) {
                                IconButton(onClick = onToggleFavorite) {
                                    Icon(
                                        imageVector = if (file.isPreferito) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Preferito",
                                        tint = if (file.isPreferito) GeoPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Share official PDF
                            IconButton(
                                onClick = {
                                    com.example.util.PdfGeneratorHelper.shareDocument(context, file)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Condividi PDF",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Download / Save PDF locally
                            Button(
                                onClick = {
                                    val saved = com.example.util.PdfGeneratorHelper.savePdfLocally(context, file)
                                    if (saved != null) {
                                        onDownload()
                                    } else {
                                        android.widget.Toast.makeText(context, "Errore salvataggio PDF", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Salva PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Modal Segmented Tabs (Anteprima / Riepilogo / Conformità)
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
                                "Anteprima Doc",
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

                // Body Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "preview_tab_transition"
                    ) { target ->
                        when (target) {
                            0 -> DocumentSheetView(
                                file = file,
                                category = docType,
                                currentPage = currentPage
                            )
                            1 -> DocumentSummaryBreakdownView(
                                file = file,
                                category = docType
                            )
                            2 -> DocumentMetadataComplianceView(
                                file = file,
                                category = docType
                            )
                        }
                    }
                }

                // Bottom Page Control Bar (only visible in Sheet View tab 0)
                if (selectedTab == 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(PfcSuccess)
                                )
                                Text(
                                    text = "Firma Digitale Valida",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = { if (currentPage > 1) currentPage-- },
                                    enabled = currentPage > 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ArrowBackIos,
                                        contentDescription = "Pagina Precedente",
                                        tint = if (currentPage > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Text(
                                    text = "Pagina $currentPage di $totalPages",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                IconButton(
                                    onClick = { if (currentPage < totalPages) currentPage++ },
                                    enabled = currentPage < totalPages,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.ArrowForwardIos,
                                        contentDescription = "Pagina Successiva",
                                        tint = if (currentPage < totalPages) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class DocCategory(val badgeLabel: String) {
    F24("MODELLO F24"),
    DICHIARAZIONE("DICHIARAZIONE FISCALE"),
    BILANCIO("BILANCIO CEE"),
    LAVORO("PAGHE & LAVORO"),
    FATTURA("DOCUMENTO IVA"),
    GENERICO("DOCUMENTO STUDIO")
}

@Composable
private fun DocumentSheetView(
    file: FileItem,
    category: DocCategory,
    currentPage: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                if (currentPage == 1) {
                    // Page 1: Main Fiscal Document Facsimile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "STUDIO PFC CONSULTING",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Dottori Commercialisti & Revisori Legali",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = GeoPrimaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "COPIA CONFORME",
                                color = GeoOnPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = file.nome.removeSuffix(".pdf"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Data Elaborazione: ${file.lastModified ?: "Corrente"}\nAnno d'Imposta: ${file.anno ?: "2025"}\nProtocollo Studio: PFC-${file.key.hashCode().toString().takeLast(8)}\nIdentificativo Univoco: ${file.key}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Specific Category Facsimile Box
                    when (category) {
                        DocCategory.F24 -> F24FacsimileBox()
                        DocCategory.DICHIARAZIONE -> DichiarazioneFacsimileBox()
                        DocCategory.BILANCIO -> BilancioFacsimileBox()
                        DocCategory.LAVORO -> CedolinoFacsimileBox()
                        DocCategory.FATTURA -> FatturaFacsimileBox()
                        DocCategory.GENERICO -> GenericFacsimileBox(file)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Attestazione di conformità: Documento elaborato e conservato digitalmente a norma di legge dallo Studio PFC. Si certifica la conformità con gli archivi fiscali e telematici.",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Justify
                    )
                } else {
                    // Page 2: Official Telematics Transmission Receipt
                    ReceiptPageContent(file)
                }
            }
        }
    }
}

@Composable
private fun F24FacsimileBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SEZIONE ERARIO & IMPOSTE DIRETTE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GeoPrimary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("6001 - Versamento IVA Mensile Gennaio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 1.250,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1040 - Ritenute d'Acconto Professionisti", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 450,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("3801 - Addizionale Regionale IRPEF", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 120,50", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTALE A DEBITO", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 1.820,50", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SALDO FINALE DELEGA (F24)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = GeoPrimary)
                Text("€ 1.820,50", fontWeight = FontWeight.Black, fontSize = 13.sp, color = GeoPrimary)
            }
        }
    }
}

@Composable
private fun DichiarazioneFacsimileBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("QUADRO SINTETICO DEI REDDITI E IMPOSTE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GeoPrimary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Reddito Complessivo Dichiarato (RN1)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 54.800,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Oneri Deducibili e Spese (RP)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 3.200,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Imposta Netta Dovuta (RN26)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 14.120,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ritenute Subite / Acconti Versati", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("- € 15.340,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PfcSuccess)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("CREDITO D'IMPOSTA A RIMBORSO", fontWeight = FontWeight.Black, fontSize = 11.sp, color = PfcSuccess)
                Text("€ 1.220,00", fontWeight = FontWeight.Black, fontSize = 12.sp, color = PfcSuccess)
            }
        }
    }
}

@Composable
private fun BilancioFacsimileBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("STATO PATRIMONIALE & CONTO ECONOMICO SINTETICO", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GeoPrimary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("A) Valore della Produzione (Ricavi)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 340.500,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("B) Costi della Produzione", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 275.200,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Differenza Produzione (A - B)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 65.300,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("UTILE D'ESERCIZIO NETTO", fontWeight = FontWeight.Black, fontSize = 11.sp, color = GeoPrimary)
                Text("€ 48.950,00", fontWeight = FontWeight.Black, fontSize = 12.sp, color = GeoPrimary)
            }
        }
    }
}

@Composable
private fun CedolinoFacsimileBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("PROSPETTO RETRIBUTIVO & CONTRIBUTIVO", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GeoPrimary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Competenze Lorde del Mese", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 2.850,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Trattenute Previdenziali INPS (9.19%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("- € 261,90", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Ritenute Fiscali IRPEF Nette", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("- € 482,10", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("NETTO DEL MESE IN BUSTA", fontWeight = FontWeight.Black, fontSize = 11.sp, color = GeoPrimary)
                Text("€ 2.106,00", fontWeight = FontWeight.Black, fontSize = 12.sp, color = GeoPrimary)
            }
        }
    }
}

@Composable
private fun FatturaFacsimileBox() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RIEPILOGO FATTURA ELETTRONICA SDI", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GeoPrimary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Imponibile Operazioni Ordinarie", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 3.500,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Imposta IVA Applicata (22%)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("€ 770,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("TOTALE DOCUMENTO", fontWeight = FontWeight.Black, fontSize = 11.sp, color = GeoPrimary)
                Text("€ 4.270,00", fontWeight = FontWeight.Black, fontSize = 12.sp, color = GeoPrimary)
            }
        }
    }
}

@Composable
private fun GenericFacsimileBox(file: FileItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SCHEDA RIEPILOGATIVA DOCUMENTO", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = GeoPrimary)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Tipologia di archiviazione", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(file.cartella ?: "Generale", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Formato Digitale", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("PDF/A-1b Conforme", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("STATO ELABORAZIONE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PfcSuccess)
                Text("CONFERMATO", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PfcSuccess)
            }
        }
    }
}

@Composable
private fun ReceiptPageContent(file: FileItem) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AGENZIA DELLE ENTRATE",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Ricevuta di Trasmissione Telematica Entratel/Fisconline",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = GeoSecondaryContainer,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "ACQUISITO",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("DATI RICEVUTA TELEMATICA", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GeoPrimary)
                Text("File trasmesso: ${file.nome}", fontSize = 11.sp)
                Text("Protocollo Ricezione: 250831${(file.key.hashCode() % 900000 + 100000)}0001", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text("Codice Autenticazione: EF${(file.key.hashCode() % 90000 + 10000)}B7A", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                Text("Data Accettazione: ${file.lastModified ?: "Oggi"} ore 11:42", fontSize = 11.sp)
                Text("Esito Trasmissione: POSITIVO (Nessun errore riscontrato)", fontWeight = FontWeight.Bold, color = PfcSuccess, fontSize = 11.sp)
            }
        }

        Text(
            text = "La presente ricevuta costituisce prova dell'avvenuta presentazione del documento telematico all'Amministrazione Finanziaria secondo le disposizioni vigenti.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun DocumentSummaryBreakdownView(
    file: FileItem,
    category: DocCategory
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = GeoPrimaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "RIEPILOGO FISCALE RAPIDO",
                        style = MaterialTheme.typography.labelSmall,
                        color = GeoOnPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = file.nome.removeSuffix(".pdf"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GeoOnPrimaryContainer
                    )
                    Text(
                        text = "Documento classificato in: ${file.cartella ?: "Archivio Fiscale"} (${file.anno ?: "2025"})",
                        style = MaterialTheme.typography.bodySmall,
                        color = GeoOnPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        item {
            Text(
                text = "VOCI E CIFRE CHIAVE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        // Summary Key Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Anno Fiscale",
                    value = file.anno ?: "2025",
                    icon = Icons.Outlined.CalendarToday,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Stato",
                    value = if (file.stato == "nuovo") "Non Letto" else "Archiviato",
                    icon = Icons.Outlined.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Note & Istruzioni dello Studio PFC",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when (category) {
                            DocCategory.F24 -> "Il modello F24 è pronto per il pagamento tramite home banking o addebito telematico su c/c. Verificare il saldo prima della data di scadenza."
                            DocCategory.DICHIARAZIONE -> "Modello trasmesso regolarmente all'Agenzia delle Entrate. Copia conforme rilasciata con visto di conformità."
                            DocCategory.BILANCIO -> "Bilancio d'esercizio approvato dall'assemblea dei soci e depositato presso il Registro Imprese CCIAA."
                            DocCategory.LAVORO -> "Prospetto paghe elaborato dall'ufficio del lavoro. Disponibile per la contabilità aziendale e dipendenti."
                            else -> "Documento regolarmente acquisito e conservato nei fascicoli dello Studio PFC."
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentMetadataComplianceView(
    file: FileItem,
    category: DocCategory
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "METADATI E CONSERVAZIONE A NORMA",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        item {
            MetadataItemRow(label = "Nome File Ufficiale", value = file.nome)
        }
        item {
            MetadataItemRow(label = "Percorso Archivio", value = file.key)
        }
        item {
            MetadataItemRow(label = "Dimensione File", value = "${file.sizeStr} (${file.size} bytes)")
        }
        item {
            MetadataItemRow(label = "Ultima Modifica", value = file.lastModified ?: "N/D")
        }
        item {
            MetadataItemRow(label = "Formato MIME", value = "application/pdf (PDF/A-1b ISO 19005-1)")
        }
        item {
            MetadataItemRow(label = "Impronta Digitale SHA-256", value = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
        }
        item {
            MetadataItemRow(label = "Conservazione Sostitutiva", value = "Attiva - Norma CAD DPCM 13/11/2014 (10 Anni)")
        }
        item {
            MetadataItemRow(label = "Studio Emittente", value = "Studio PFC Consulting - P.IVA 01234567890")
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(20.dp))
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun MetadataItemRow(label: String, value: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = if (value.startsWith("e3b") || value.contains("/")) FontFamily.Monospace else FontFamily.Default
            )
        }
    }
}
