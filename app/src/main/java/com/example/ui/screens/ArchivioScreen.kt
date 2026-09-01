package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
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
    archivioViewMode: Int = 0,
    onSetArchivioViewMode: (Int) -> Unit = {},
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
        // Search & Filter Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp
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
                    placeholder = {
                        Text(
                            "Cerca per nome, data o tipo...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Cerca",
                            tint = PfcGoldDark
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    Icons.Filled.Clear,
                                    contentDescription = "Pulisci",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )

                // Year Selector Chips + Favorites Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(PfcGold)
                                            )
                                        }
                                        Text(
                                            text = yr,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GeoPrimary,
                                    selectedLabelColor = Color.White,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = if (isSelected) BorderStroke(1.dp, PfcGold.copy(alpha = 0.6f)) else null
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
                            .background(if (filterFavoritesOnly) PfcGoldContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .border(1.dp, if (filterFavoritesOnly) PfcGold.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = if (filterFavoritesOnly) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Filtra preferiti",
                            tint = if (filterFavoritesOnly) PfcGoldDark else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // View Mode Tabs (Cartelle vs Tutti i Documenti)
                if (searchQuery.isBlank() && selectedCartella == null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tab Cartelle
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSetArchivioViewMode(0) },
                            color = if (archivioViewMode == 0) GeoPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = if (archivioViewMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Cartelle (${cartelle.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (archivioViewMode == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (archivioViewMode == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Tab Tutti i Documenti
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSetArchivioViewMode(1) },
                            color = if (archivioViewMode == 1) GeoPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = if (archivioViewMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tutti i File (${files.size})",
                                    fontSize = 12.sp,
                                    fontWeight = if (archivioViewMode == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (archivioViewMode == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                            Text("Tutti", color = PfcGoldLight, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onDownloadBatch,
                            colors = ButtonDefaults.buttonColors(containerColor = PfcGold),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, tint = PfcMidnight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scarica", color = PfcMidnight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${searchResults.size} RISULTATI TROVATI",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
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
                        // Folder Breadcrumb Header
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectCartella(null) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = CircleShape,
                                    color = GeoPrimaryContainer
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.ArrowBack,
                                            contentDescription = "Indietro",
                                            tint = GeoOnPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Archivio $selectedYear",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = selectedCartella.nome,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
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

                // 3. Tutti i File View (when ViewMode is 1)
                archivioViewMode == 1 -> {
                    val displayedFiles = if (filterFavoritesOnly) {
                        files.filter { it.isPreferito }
                    } else files

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header summary row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TUTTI I DOCUMENTI ($selectedYear)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "${displayedFiles.size} file totali",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
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
                                title = if (filterFavoritesOnly) "Nessun preferito per il $selectedYear" else "Nessun documento trovato per il $selectedYear",
                                description = "Seleziona un altro anno o visualizza le cartelle fiscali."
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

                // 4. Root View: Folders List with Luxury Hero Banner (when ViewMode is 0)
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
                            // Spectacular Hero Banner Card with 3D Image & Glass Info
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(8.dp, shape = RoundedCornerShape(24.dp)),
                                    shape = RoundedCornerShape(24.dp),
                                    border = BorderStroke(1.dp, PfcGold.copy(alpha = 0.4f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(185.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_pfc_hero),
                                            contentDescription = "Studio PFC Banner",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )

                                        // Dark luxury overlay with gradient
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(
                                                            PfcMidnight.copy(alpha = 0.65f),
                                                            PfcMidnight.copy(alpha = 0.92f)
                                                        )
                                                    )
                                                )
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(18.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    color = PfcGold.copy(alpha = 0.25f),
                                                    shape = RoundedCornerShape(8.dp),
                                                    border = BorderStroke(1.dp, PfcGold.copy(alpha = 0.6f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(PfcGold)
                                                        )
                                                        Text(
                                                            text = "ESERCIZIO $selectedYear",
                                                            color = PfcGoldLight,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            letterSpacing = 0.8.sp
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    color = Color.White.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = "CONFORMITÀ CAD",
                                                        color = Color.White,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 0.5.sp,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = "Archivio Fiscale & Societario",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = (-0.3).sp
                                                )
                                                Text(
                                                    text = "Documentazione contabile, bilanci e dichiarazioni con protocollo Entratel.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    modifier = Modifier.padding(top = 2.dp),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val totalNuovi = cartelle.sumOf { it.nuovi ?: 0 }
                                                Text(
                                                    text = "${cartelle.size} sezioni • ${if (totalNuovi > 0) "$totalNuovi nuovi" else "Tutti letti"}",
                                                    color = if (totalNuovi > 0) PfcGold else PfcGoldLight,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )

                                                Surface(
                                                    color = PfcGold,
                                                    shape = RoundedCornerShape(50),
                                                    modifier = Modifier.clickable {
                                                        if (cartelle.isNotEmpty()) onSelectCartella(cartelle.first())
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Esplora",
                                                            color = PfcMidnight,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp
                                                        )
                                                        Icon(
                                                            imageVector = Icons.Filled.ArrowForward,
                                                            contentDescription = null,
                                                            tint = PfcMidnight,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
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
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
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
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(GeoPrimaryContainer, PfcGoldContainer)
                            )
                        )
                        .border(1.dp, PfcGold.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = GeoPrimary,
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
                        modifier = Modifier.padding(top = 4.dp)
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

            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
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
            containerColor = if (isBatchSelected) GeoPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isBatchSelected) GeoPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
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
                    }

                    // Metadata Row: Size, Date, Folder
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Client Backend Status Badges: NUOVO / VISTO / SCARICATO
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 5.dp)
                    ) {
                        val statoStr = file.stato ?: "visto"
                        StatusBadge(status = statoStr)

                        if (file.isPreferito) {
                            StatusBadge(status = "preferito")
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
                        tint = if (file.isPreferito) PfcGoldDark else MaterialTheme.colorScheme.onSurfaceVariant,
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
