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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Cartella
import com.example.data.model.FileItem
import com.example.data.model.SearchResult
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchivioScreen(
    years: List<String>,
    selectedYear: String,
    cartelle: List<Cartella>,
    selectedCartella: Cartella?,
    files: List<FileItem>,
    filesLoading: Boolean,
    searchQuery: String,
    searchResults: List<SearchResult>,
    filterFavoritesOnly: Boolean,
    selectedBatchKeys: Set<String>,
    onSelectYear: (String) -> Unit,
    onSelectCartella: (Cartella?) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleFavorite: (FileItem) -> Unit,
    onToggleFilterFavorites: () -> Unit,
    onOpenFilePreview: (FileItem) -> Unit,
    onToggleBatchKey: (String) -> Unit,
    onSelectAllBatch: () -> Unit,
    onClearBatch: () -> Unit,
    onDownloadBatch: () -> Unit,
    onDownloadSingle: (FileItem) -> Unit
) {
    val isBatchMode = selectedBatchKeys.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Filter Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cerca in tutti i documenti...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Cerca",
                            tint = PfcSlate
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Pulisci")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_document_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PfcAmber,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Year Selector Chips + Favorites Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(years) { yr ->
                            val isSelected = yr == selectedYear
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectYear(yr) },
                                label = {
                                    Text(
                                        text = yr,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PfcAmber,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Favorite Filter Button
                    IconButton(
                        onClick = onToggleFilterFavorites,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (filterFavoritesOnly) PfcAmberSoft else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (filterFavoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Filtra preferiti",
                            tint = if (filterFavoritesOnly) PfcAmber else PfcSlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Batch Selection Header (when active)
        AnimatedVisibility(
            visible = isBatchMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = PfcNavyMid,
                modifier = Modifier.fillMaxWidth()
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(onClick = onClearBatch) {
                            Icon(Icons.Filled.Close, contentDescription = "Annulla", tint = Color.White)
                        }
                        Text(
                            text = "${selectedBatchKeys.size} selezionati",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = onSelectAllBatch) {
                            Text("Tutti", color = PfcAmber, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDownloadBatch,
                            colors = ButtonDefaults.buttonColors(containerColor = PfcAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scarica", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Main Content Area
        Box(modifier = Modifier.weight(1f)) {
            when {
                // 1. Search Active View
                searchQuery.isNotBlank() -> {
                    if (searchResults.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.SearchOff,
                            title = "Nessun documento trovato",
                            description = "Nessun risultato corrisponde a \"$searchQuery\"."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "${searchResults.size} RISULTATI TROVATI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PfcSlateLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            items(searchResults) { result ->
                                val fakeFile = FileItem(
                                    nome = result.nome,
                                    key = result.key,
                                    size = result.size,
                                    sizeStr = result.sizeStr,
                                    anno = result.anno,
                                    cartella = result.cartella
                                )
                                DocumentFileRow(
                                    file = fakeFile,
                                    isBatchSelected = false,
                                    isBatchMode = false,
                                    onFileClick = { onOpenFilePreview(fakeFile) },
                                    onToggleFavorite = { onToggleFavorite(fakeFile) },
                                    onToggleBatch = {},
                                    onDownload = { onDownloadSingle(fakeFile) }
                                )
                            }
                        }
                    }
                }

                // 2. Inside a Folder: Files List
                selectedCartella != null -> {
                    val displayedFiles = if (filterFavoritesOnly) {
                        files.filter { it.isPreferito }
                    } else files

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Folder Breadcrumb Navigation
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCartella(null) }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ArrowBack,
                                    contentDescription = "Indietro",
                                    tint = PfcAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Archivio $selectedYear",
                                    color = PfcSlate,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "•",
                                    color = PfcSlateLight
                                )
                                Text(
                                    text = selectedCartella.nome,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        if (filesLoading) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                repeat(5) { ShimmerItem(height = 70) }
                            }
                        } else if (displayedFiles.isEmpty()) {
                            EmptyStateView(
                                icon = if (filterFavoritesOnly) Icons.Outlined.StarBorder else Icons.Outlined.FolderOpen,
                                title = if (filterFavoritesOnly) "Nessun preferito in questa cartella" else "Cartella vuota",
                                description = "Non sono presenti documenti per questa selezione."
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(displayedFiles) { file ->
                                    val isSelected = selectedBatchKeys.contains(file.key)
                                    DocumentFileRow(
                                        file = file,
                                        isBatchSelected = isSelected,
                                        isBatchMode = isBatchMode,
                                        onFileClick = { onOpenFilePreview(file) },
                                        onToggleFavorite = { onToggleFavorite(file) },
                                        onToggleBatch = { onToggleBatchKey(file.key) },
                                        onDownload = { onDownloadSingle(file) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Root View: Folders List
                else -> {
                    if (cartelle.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Outlined.FolderOff,
                            title = "Nessuna cartella trovata per il $selectedYear",
                            description = "Seleziona un altro anno dal selettore in alto."
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CARTELLE FISCALI $selectedYear",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PfcSlateLight,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "${cartelle.size} sezioni",
                                        fontSize = 12.sp,
                                        color = PfcSlate
                                    )
                                }
                            }

                            items(cartelle) { cartella ->
                                CartellaCard(
                                    cartella = cartella,
                                    onClick = { onSelectCartella(cartella) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartellaCard(
    cartella: Cartella,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("folder_card_${cartella.nome}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PfcAmberSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = PfcAmberDark,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cartella.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
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
                            text = "${cartella.count ?: 0} documenti",
                            style = MaterialTheme.typography.bodySmall,
                            color = PfcSlate
                        )

                        if ((cartella.nuovi ?: 0) > 0) {
                            StatusBadge(status = "nuovo")
                        }

                        if (cartella.hasScadenza == true && !cartella.scadenzaData.isNullOrBlank()) {
                            StatusBadge(status = "scadenza")
                        }
                    }
                }
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = PfcSlateLight,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun DocumentFileRow(
    file: FileItem,
    isBatchSelected: Boolean,
    isBatchMode: Boolean,
    onFileClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleBatch: () -> Unit,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                if (isBatchMode) onToggleBatch() else onFileClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isBatchSelected) PfcAmberSoft else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (isBatchMode) {
                    Checkbox(
                        checked = isBatchSelected,
                        onCheckedChange = { onToggleBatch() },
                        colors = CheckboxDefaults.colors(checkedColor = PfcAmber)
                    )
                } else {
                    FileFormatIcon(fileName = file.nome)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = file.nome,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (file.stato == "nuovo") {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(PfcSuccess)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        if (file.sizeStr.isNotBlank()) {
                            Text(
                                text = file.sizeStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = PfcSlate
                            )
                        }

                        if (!file.lastModified.isNullOrBlank()) {
                            Text(
                                text = "•  ${file.lastModified}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PfcSlate
                            )
                        }

                        if (!file.cartella.isNullOrBlank()) {
                            Text(
                                text = "•  ${file.cartella}",
                                style = MaterialTheme.typography.bodySmall,
                                color = PfcAmberDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Quick Actions: Star + Download
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (file.isPreferito) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Preferito",
                        tint = if (file.isPreferito) PfcAmber else PfcSlateLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = "Scarica",
                        tint = PfcSlate,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
