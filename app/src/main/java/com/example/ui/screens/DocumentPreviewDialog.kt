package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    onDownload: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(1) }
    val totalPages = 2

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
                                Icon(Icons.Filled.Close, contentDescription = "Chiudi", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.nome,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${file.cartella ?: "Documento"} • ${file.sizeStr}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = onDownload,
                            colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scarica", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Document Viewer Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Realistic Document Page Sheet
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
                                .padding(24.dp)
                        ) {
                            // Header of Document
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
                                        text = "Consulenza Tributaria e Societaria",
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

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = file.nome.removeSuffix(".pdf"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Data Elaborazione: ${file.lastModified ?: "Corrente"}\nAnno Fiscale: ${file.anno ?: "2025"}\nProtocollo Telematico: PFC-${file.key.hashCode().toString().takeLast(8)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            // Mock Document Table / Body Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("DESCRIZIONE TRIBUTO / VOCE", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("IMPORTO A DEBITO", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Codice Tributo 6001 - Versamento IVA Mensile", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("€ 1.250,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Codice Tributo 1040 - Ritenute d'Acconto Professionisti", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("€ 450,00", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("TOTALE DA VERSARE / SALDO FINALE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = GeoPrimary)
                                        Text("€ 1.700,00", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GeoPrimary)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Attestazione di conformità: Documento elaborato telematicamente tramite i sistemi dello Studio PFC. Si attesta la corrispondenza dei dati tributari con le risultanze contabili trasmesse all'Agenzia delle Entrate.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }
                }

                // Page Navigation Bar at bottom
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = { if (currentPage > 1) currentPage-- },
                            enabled = currentPage > 1
                        ) {
                            Icon(Icons.Filled.ArrowBackIos, contentDescription = "Pagina Precedente", tint = if (currentPage > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "Pagina $currentPage di $totalPages",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        IconButton(
                            onClick = { if (currentPage < totalPages) currentPage++ },
                            enabled = currentPage < totalPages
                        ) {
                            Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Pagina Successiva", tint = if (currentPage < totalPages) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

