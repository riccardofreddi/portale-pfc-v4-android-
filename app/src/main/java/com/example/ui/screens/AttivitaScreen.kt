package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CachedAuditEntity
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*

@Composable
fun AttivitaScreen(
    auditLogs: List<CachedAuditEntity>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "REGISTRO ATTIVITÀ & AUDIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tracciamento Operazioni",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tutte le consultazioni, download e accessi registrati sul portale.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (auditLogs.isEmpty()) {
            EmptyStateView(
                icon = Icons.Outlined.History,
                title = "Nessuna attività registrata",
                description = "Le azioni eseguite sul portale verranno elencate in questa cronologia di sicurezza."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(auditLogs, key = { it.id }) { log ->
                    AuditLogRow(log = log)
                }
            }
        }
    }
}

@Composable
fun AuditLogRow(log: CachedAuditEntity) {
    val (icon, bgColor, tintColor, label) = when (log.action.lowercase()) {
        "login" -> Quadruple(Icons.Filled.Login, PfcSuccessSoft, PfcSuccess, "Accesso")
        "logout" -> Quadruple(Icons.Filled.Logout, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, "Disconnessione")
        "download", "download_batch" -> Quadruple(Icons.Filled.Download, GeoPrimaryContainer, GeoOnPrimaryContainer, "Download")
        "preview" -> Quadruple(Icons.Filled.Visibility, PfcAmberSoft, PfcAmberDark, "Anteprima")
        "preferito" -> Quadruple(Icons.Filled.Star, PfcWarningSoft, PfcWarning, "Preferiti")
        "upload", "cassetto_add" -> Quadruple(Icons.Filled.CloudUpload, GeoPrimaryContainer, GeoOnPrimaryContainer, "Caricamento")
        "cassetto_delete" -> Quadruple(Icons.Filled.Delete, PfcDangerSoft, PfcDanger, "Eliminazione")
        "cassetto_rename" -> Quadruple(Icons.Filled.Edit, GeoPrimaryContainer, GeoOnPrimaryContainer, "Rinomina")
        else -> Quadruple(Icons.Filled.Info, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, log.action)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = label,
                            color = tintColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = log.ts,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = log.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

