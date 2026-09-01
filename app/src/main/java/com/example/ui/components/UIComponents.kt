package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PfcTopBar(
    title: String = "Portale PFC",
    subtitle: String? = null,
    unreadNotifCount: Int = 0,
    userInitials: String = "PF",
    onNotifClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand Group with Monogram Emblem & Gold Ring
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(PfcMidnight, GeoPrimary, PfcSapphire)
                                )
                            )
                            .padding(1.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.5.dp))
                                .background(PfcMidnight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PFC",
                                color = PfcGold,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.3).sp
                            )
                            Surface(
                                color = PfcGold.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "CLIENTI",
                                    color = PfcGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Actions Group
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Notifications Button with Animated Glowing Badge
                    Surface(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable { onNotifClick() }
                            .testTag("notif_button"),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = CircleShape
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (unreadNotifCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Notifiche",
                                tint = if (unreadNotifCount > 0) PfcGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )

                            if (unreadNotifCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 2.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(PfcDanger),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadNotifCount > 9) "9+" else unreadNotifCount.toString(),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Profile Avatar with Luxury Gold Ring
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PfcGold, PfcGoldLight, PfcGoldDark)
                                )
                            )
                            .padding(2.dp)
                            .clickable { onProfileClick() }
                            .testTag("profile_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(PfcMidnight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userInitials.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.lowercase().trim()) {
        "nuovo" -> Triple(PfcSuccessSoft, PfcSuccess, "● NUOVO")
        "visto" -> Triple(GeoPrimaryContainer.copy(alpha = 0.7f), GeoPrimary, "👁 VISTO")
        "scaricato" -> Triple(PfcGoldContainer, PfcGoldDark, "✓ SCARICATO")
        "preferito" -> Triple(PfcGoldContainer, PfcGoldDark, "★ PREFERITO")
        "richiede_upload" -> Triple(PfcGoldContainer, PfcGoldDark, "Richiede Upload")
        "risposto" -> Triple(PfcSuccessSoft, PfcSuccess, "✓ Risposto")
        "scadenza" -> Triple(PfcDangerSoft, PfcDanger, "In Scadenza")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, status.uppercase())
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
        )
    }
}

@Composable
fun CategoryPill(
    category: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when {
        category.contains("QR", ignoreCase = true) -> Pair(PfcInfoSoft, PfcInfo)
        category.contains("Certificato", ignoreCase = true) -> Pair(PfcGoldContainer, PfcGoldDark)
        category.contains("Visura", ignoreCase = true) -> Pair(GeoPrimaryContainer, GeoOnPrimaryContainer)
        category.contains("Identit", ignoreCase = true) -> Pair(PfcSuccessSoft, PfcSuccess)
        category.contains("IBAN", ignoreCase = true) -> Pair(PfcPurpleSoft, PfcPurple)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.25f))
    ) {
        Text(
            text = category,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun FileFormatIcon(
    fileName: String,
    modifier: Modifier = Modifier
) {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val (bgColor, iconVector, badgeText, iconTint) = when (ext) {
        "pdf" -> Quad(Color(0xFFFEF2F2), Icons.Filled.PictureAsPdf, "PDF", Color(0xFFDC2626))
        "xml", "p7m" -> Quad(Color(0xFFEEF2FF), Icons.Filled.Code, "XML", Color(0xFF4F46E5))
        "zip", "rar" -> Quad(Color(0xFFFFFBEB), Icons.Filled.FolderZip, "ZIP", Color(0xFFD97706))
        "png", "jpg", "jpeg" -> Quad(Color(0xFFECFDF5), Icons.Filled.Image, "IMG", Color(0xFF059669))
        "doc", "docx", "xls", "xlsx" -> Quad(Color(0xFFF0F9FF), Icons.Filled.Description, "DOC", Color(0xFF0284C7))
        else -> Quad(Color(0xFFF8FAFC), Icons.Filled.InsertDriveFile, "FILE", Color(0xFF64748B))
    }

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(bgColor)
            .border(1.dp, iconTint.copy(alpha = 0.2f), RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = badgeText,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EmptyStateView(
    icon: ImageVector = Icons.Outlined.FolderOpen,
    title: String,
    description: String? = null,
    actionLabel: String? = null,
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(GeoPrimaryContainer, PfcGoldContainer)
                    )
                )
                .border(1.dp, PfcGold.copy(alpha = 0.3f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GeoPrimary,
                modifier = Modifier.size(38.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
                lineHeight = 20.sp
            )
        }

        if (!actionLabel.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = GeoPrimary),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    height: Int = 76
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
    )
}
