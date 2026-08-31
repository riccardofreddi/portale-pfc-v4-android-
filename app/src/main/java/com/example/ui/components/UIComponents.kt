package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
        color = PfcNavyDark,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PfcAmber, PfcAmberDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "PF",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = PfcSlateLight,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Actions Group
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notifications button with badge
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNotifClick() }
                        .testTag("notif_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (unreadNotifCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                        contentDescription = "Notifiche",
                        tint = if (unreadNotifCount > 0) PfcAmber else Color.White,
                        modifier = Modifier.size(24.dp)
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

                // Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { onProfileClick() }
                        .testTag("profile_avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitials.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
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
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "nuovo" -> Triple(PfcSuccessSoft, PfcSuccess, "Nuovo")
        "preferito" -> Triple(PfcAmberSoft, PfcAmberDark, "Preferito")
        "scaricato" -> Triple(PfcInfoSoft, PfcInfo, "Scaricato")
        "richiede_upload" -> Triple(PfcPurpleSoft, PfcPurple, "Richiede Upload")
        "risposto" -> Triple(PfcSuccessSoft, PfcSuccess, "✓ Risposto")
        "scadenza" -> Triple(PfcDangerSoft, PfcDanger, "In Scadenza")
        else -> Triple(PfcSurfaceAltLight, PfcSlate, status)
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
        category.contains("Certificato", ignoreCase = true) -> Pair(PfcAmberSoft, PfcAmberDark)
        category.contains("Visura", ignoreCase = true) -> Pair(PfcPurpleSoft, PfcPurple)
        category.contains("Identit", ignoreCase = true) -> Pair(PfcSuccessSoft, PfcSuccess)
        category.contains("IBAN", ignoreCase = true) -> Pair(Color(0xFFCCFBF1), Color(0xFF0F766E))
        else -> Pair(PfcSurfaceAltLight, PfcSlate)
    }

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = category,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun FileFormatIcon(
    fileName: String,
    modifier: Modifier = Modifier
) {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    val (bgColor, iconVector, badgeText) = when (ext) {
        "pdf" -> Triple(Color(0xFFFEE2E2), Icons.Filled.PictureAsPdf, "PDF")
        "xml", "p7m" -> Triple(Color(0xFFE0E7FF), Icons.Filled.Code, "XML")
        "zip", "rar" -> Triple(Color(0xFFFEF3C7), Icons.Filled.FolderZip, "ZIP")
        "png", "jpg", "jpeg" -> Triple(Color(0xFFD1FAE5), Icons.Filled.Image, "IMG")
        "doc", "docx", "xls", "xlsx" -> Triple(Color(0xFFDBEAFE), Icons.Filled.Description, "DOC")
        else -> Triple(PfcSurfaceAltLight, Icons.Filled.InsertDriveFile, "FILE")
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconVector,
            contentDescription = badgeText,
            tint = when (badgeText) {
                "PDF" -> PfcDanger
                "XML" -> Color(0xFF4F46E5)
                "ZIP" -> PfcWarning
                "IMG" -> PfcSuccess
                "DOC" -> PfcInfo
                else -> PfcSlate
            },
            modifier = Modifier.size(24.dp)
        )
    }
}

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
                .size(72.dp)
                .clip(CircleShape)
                .background(PfcSurfaceAltLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PfcSlateLight,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (!description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = PfcSlate,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (!actionLabel.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                colors = ButtonDefaults.buttonColors(containerColor = PfcAmber),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ShimmerItem(
    modifier: Modifier = Modifier,
    height: Int = 72
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
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
            .clip(RoundedCornerShape(12.dp))
            .background(PfcSlateLight.copy(alpha = alpha))
    )
}
