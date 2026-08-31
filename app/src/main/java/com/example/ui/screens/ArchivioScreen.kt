package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Cerca per nome, data (es. 2025, Ottobre) o tipo...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Cerca",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Pulisci", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_document_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeoPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                // Quick Filter Tag Chips (F24, Bilancio, Dichiarazioni, 2025, 2024, Cedolini)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    val quickFilters = listOf("F24", "Dichiarazioni", "Bilancio", "Cedolini", "2025", "2024")
                    items(quickFilters) { filterTag ->
                        val isSelected = searchQuery.equals(filterTag, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) onSearchChange("") else onSearchChange(filterTag)
                            },
                            shape = RoundedCornerShape(8.dp),
                            label = { Text(filterTag, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GeoPrimaryContainer,
                                selectedLabelColor = GeoOnPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

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
                                shape = RoundedCornerShape(50),
                                label = {
                                    Text(
                                        text = yr,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GeoPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Favorite Filter Button
                    IconButton(
                        onClick = onToggleFilterFavorites,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (filterFavoritesOnly) GeoPrimaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = if (filterFavoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Filtra preferiti",
                            tint = if (filterFavoritesOnly) GeoOnPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
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
                color = GeoPrimary,
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
                            Text("Tutti", color = GeoAccentLavender, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDownloadBatch,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = GeoPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scarica", color = GeoPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "${searchResults.size} RISULTATI TROVATI",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
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
                                    tint = GeoPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Archivio $selectedYear",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "•",
                                    color = MaterialTheme.colorScheme.outline
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
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                repeat(5) { ShimmerItem(height = 76) }
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
                                verticalArrangement = Arrangement.spacedBy(12.dp)
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

                // 3. Root View: Folders List with Geometric Hero Card
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
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Geometric Hero Banner
                            item {
                                Surface(
                                    color = GeoPrimaryContainer,
                                    shape = RoundedCornerShape(28.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = "ANNO FISCALE $selectedYear",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GeoOnPrimaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Archivio Documenti Studio",
                                                style = MaterialTheme.typography.titleLarge,
                                                color = GeoOnPrimaryContainer,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Consulta F24, dichiarazioni e bilanci con protocolli telematici verificati.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GeoOnPrimaryContainer.copy(alpha = 0.85f),
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = GeoPrimary,
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.clickable {
                                                    if (cartelle.isNotEmpty()) onSelectCartella(cartelle.first())
                                                }
                                            ) {
                                                Text(
                                                    text = "Esplora sezioni",
                                                    color = Color.White,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                )
                                            }

                                            Surface(
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(50),
                                                border = BorderStroke(1.dp, GeoBorderStrongLight),
                                                modifier = Modifier.clickable { onToggleFilterFavorites() }
                                            ) {
                                                Text(
                                                    text = if (filterFavoritesOnly) "Mostra tutti" else "Solo preferiti",
                                                    color = GeoOnPrimaryContainer,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CARTELLE FISCALI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "${cartelle.size} sezioni",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                        .clip(RoundedCornerShape(14.dp))
                        .background(GeoAccentLavender),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = GeoOnPrimaryContainer,
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
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text(
                            text = "${cartella.count ?: 0} documenti",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                if (isBatchMode) onToggleBatch() else onFileClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isBatchSelected) GeoSecondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isBatchSelected) GeoPrimary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                        colors = CheckboxDefaults.colors(checkedColor = GeoPrimary)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!file.lastModified.isNullOrBlank()) {
                            Text(
                                text = "•  ${file.lastModified}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!file.cartella.isNullOrBlank()) {
                            Text(
                                text = "•  ${file.cartella}",
                                style = MaterialTheme.typography.bodySmall,
                                color = GeoPrimary,
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
                        tint = if (file.isPreferito) GeoPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
