package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CachedCassettoEntity
import com.example.data.model.FileItem
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CassettoScreen(
    cassettoFiles: List<CachedCassettoEntity>,
    showAddDialog: Boolean,
    fileToRename: CachedCassettoEntity?,
    onOpenAddDialog: () -> Unit,
    onCloseAddDialog: () -> Unit,
    onAddDocument: (name: String, category: String) -> Unit,
    onOpenFileToRename: (CachedCassettoEntity) -> Unit,
    onCloseRenameDialog: () -> Unit,
    onConfirmRename: (key: String, newName: String) -> Unit,
    onDeleteDocument: (CachedCassettoEntity) -> Unit,
    onOpenFilePreview: (FileItem) -> Unit,
    onDownloadDocument: (CachedCassettoEntity) -> Unit
) {
    var selectedCategoryFilter by remember { mutableStateOf("Tutti") }
    var fileToDelete by remember { mutableStateOf<CachedCassettoEntity?>(null) }

    val categories = listOf("Tutti", "QR Code P.IVA", "Certificato P.IVA", "Visura Camerale", "Doc. Identità", "IBAN", "Altro")

    val filteredFiles = remember(cassettoFiles, selectedCategoryFilter) {
        if (selectedCategoryFilter == "Tutti") cassettoFiles
        else cassettoFiles.filter { it.categoria.contains(selectedCategoryFilter, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Vault Hero Card
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PfcNavyDark, PfcNavyMid)
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = PfcAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "CASSETTO PERSONALE",
                                        color = PfcAmber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "I tuoi file sempre protetti",
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cassettoFiles.size} documenti archiviati in sicurezza",
                                    color = PfcSlateLight,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = onOpenAddDialog,
                                colors = ButtonDefaults.buttonColors(containerColor = PfcAmber),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_to_cassetto_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aggiungi", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Category Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategoryFilter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PfcNavyDark,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Files List
        if (filteredFiles.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.FolderShared,
                title = "Nessun documento trovato",
                description = "Carica i tuoi documenti personali (QR P.IVA, Visura, IBAN) toccando il pulsante 'Aggiungi'.",
                actionLabel = "Carica Nuovo Documento",
                onActionClick = onOpenAddDialog
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredFiles, key = { it.key }) { file ->
                    CassettoFileCard(
                        file = file,
                        onPreview = {
                            onOpenFilePreview(
                                FileItem(
                                    nome = file.nome,
                                    key = file.key,
                                    size = file.size,
                                    sizeStr = file.sizeStr,
                                    lastModified = file.lastModified,
                                    cartella = "Cassetto Personale"
                                )
                            )
                        },
                        onDownload = { onDownloadDocument(file) },
                        onRename = { onOpenFileToRename(file) },
                        onDelete = { fileToDelete = file }
                    )
                }
            }
        }
    }

    // Add Document Dialog
    if (showAddDialog) {
        AddCassettoDialog(
            onDismiss = onCloseAddDialog,
            onAdd = onAddDocument
        )
    }

    // Rename Document Dialog
    if (fileToRename != null) {
        RenameCassettoDialog(
            currentName = fileToRename.nome,
            onDismiss = onCloseRenameDialog,
            onConfirm = { newName -> onConfirmRename(fileToRename.key, newName) }
        )
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            icon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = PfcDanger) },
            title = { Text("Eliminare documento?") },
            text = { Text("Sei sicuro di voler eliminare \"${fileToDelete!!.nome}\" dal tuo cassetto personale?") },
            confirmButton = {
                Button(
                    onClick = {
                        val f = fileToDelete!!
                        fileToDelete = null
                        onDeleteDocument(f)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PfcDanger)
                ) {
                    Text("Elimina", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Annulla", color = PfcSlate)
                }
            }
        )
    }
}

@Composable
fun CassettoFileCard(
    file: CachedCassettoEntity,
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onPreview() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    FileFormatIcon(fileName = file.nome)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = file.nome,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = file.sizeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = PfcSlate
                            )
                            Text(text = "•", color = PfcSlateLight)
                            Text(
                                text = file.lastModified ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = PfcSlate
                            )
                        }
                    }
                }

                CategoryPill(category = file.categoria)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(6.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPreview,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, modifier = Modifier.size(16.dp), tint = PfcAmber)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Anteprima", color = PfcAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row {
                    IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Download, contentDescription = "Scarica", tint = PfcSlate, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Rinomina", tint = PfcSlate, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Elimina", tint = PfcDanger, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AddCassettoDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, category: String) -> Unit
) {
    var documentName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("QR Code P.IVA") }
    val categories = listOf("QR Code P.IVA", "Certificato P.IVA", "Visura Camerale", "Doc. Identità", "IBAN", "Altro")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Carica Documento nel Cassetto", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = documentName,
                    onValueChange = { documentName = it },
                    label = { Text("Nome Documento (es. Visura CCIAA 2025.pdf)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "CATEGORIA DOCUMENTO",
                    style = MaterialTheme.typography.labelSmall,
                    color = PfcSlateLight,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { cat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            RadioButton(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                colors = RadioButtonDefaults.colors(selectedColor = PfcAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(cat, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (documentName.endsWith(".pdf", ignoreCase = true)) documentName else "$documentName.pdf"
                    onAdd(finalName, selectedCategory)
                },
                enabled = documentName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PfcAmber)
            ) {
                Text("Salva Documento", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = PfcSlate)
            }
        }
    )
}

@Composable
fun RenameCassettoDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rinomina Documento") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nuovo Nome File") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PfcAmber)
            ) {
                Text("Rinomina", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = PfcSlate)
            }
        }
    )
}
