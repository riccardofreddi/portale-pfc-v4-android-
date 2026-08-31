package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.PfcTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PfcViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PfcViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PfcViewModel) {
    val onboardingDone by viewModel.onboardingDone.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val loginLoading by viewModel.loginLoading.collectAsState()
    val loginError by viewModel.loginError.collectAsState()

    val selectedTab by viewModel.selectedTab.collectAsState()
    val showNotifSheet by viewModel.showNotifSheet.collectAsState()
    val showSettingsSheet by viewModel.showSettingsSheet.collectAsState()
    val previewFile by viewModel.previewFile.collectAsState()
    val showAddCassetto by viewModel.showAddCassetto.collectAsState()
    val cassettoFileToRename by viewModel.cassettoFileToRename.collectAsState()

    val unreadNotifCount by viewModel.unreadNotifCount.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val fcmStatus by viewModel.fcmStatus.collectAsState()
    val fcmTesting by viewModel.fcmTesting.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    when {
        !onboardingDone -> {
            OnboardingScreen(onFinish = { viewModel.completeOnboarding() })
        }
        !isLoggedIn -> {
            LoginScreen(
                loading = loginLoading,
                errorMessage = loginError,
                onLogin = { usr, pwd -> viewModel.login(usr, pwd) }
            )
        }
        else -> {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    val subtitle = when (selectedTab) {
                        0 -> "Archivio Fiscale ${viewModel.selectedYear.collectAsState().value}"
                        1 -> "Comunicazioni Studio"
                        2 -> "Cassetto Personale"
                        3 -> "Registro Attività"
                        else -> currentUser?.name
                    }
                    PfcTopBar(
                        title = "Portale PFC",
                        subtitle = subtitle,
                        unreadNotifCount = unreadNotifCount,
                        userInitials = currentUser?.name ?: "PF",
                        onNotifClick = { viewModel.setShowNotifSheet(true) },
                        onProfileClick = { viewModel.setShowSettingsSheet(true) }
                    )
                },
                bottomBar = {
                    val attiviList by viewModel.attiviMessaggi.collectAsState()
                    val unreadMsgCount = attiviList.count { !it.letto }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar")
                    ) {
                        // 0: Archivio
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.setTab(0) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 0) Icons.Filled.Folder else Icons.Outlined.Folder,
                                    contentDescription = "Archivio"
                                )
                            },
                            label = {
                                Text(
                                    "Archivio",
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PfcAmber,
                                selectedTextColor = PfcAmber,
                                indicatorColor = PfcAmberSoft
                            )
                        )

                        // 1: Messaggi
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setTab(1) },
                            icon = {
                                Box {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Filled.Chat else Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Messaggi"
                                    )
                                    if (unreadMsgCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 4.dp, y = (-2).dp)
                                                .size(14.dp)
                                                .clip(CircleShape)
                                                .background(PfcDanger),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = unreadMsgCount.toString(),
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            },
                            label = {
                                Text(
                                    "Messaggi",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PfcAmber,
                                selectedTextColor = PfcAmber,
                                indicatorColor = PfcAmberSoft
                            )
                        )

                        // 2: Cassetto
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.setTab(2) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.Lock else Icons.Outlined.Lock,
                                    contentDescription = "Cassetto"
                                )
                            },
                            label = {
                                Text(
                                    "Cassetto",
                                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PfcAmber,
                                selectedTextColor = PfcAmber,
                                indicatorColor = PfcAmberSoft
                            )
                        )

                        // 3: Attività
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { viewModel.setTab(3) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 3) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "Attività"
                                )
                            },
                            label = {
                                Text(
                                    "Attività",
                                    fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PfcAmber,
                                selectedTextColor = PfcAmber,
                                indicatorColor = PfcAmberSoft
                            )
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        label = "tab_transition"
                    ) { tab ->
                        when (tab) {
                            0 -> {
                                val years by viewModel.years.collectAsState()
                                val selectedYear by viewModel.selectedYear.collectAsState()
                                val cartelle by viewModel.cartelle.collectAsState()
                                val selectedCartella by viewModel.selectedCartella.collectAsState()
                                val files by viewModel.files.collectAsState()
                                val filesLoading by viewModel.filesLoading.collectAsState()
                                val searchQuery by viewModel.searchQuery.collectAsState()
                                val searchResults by viewModel.searchResults.collectAsState()
                                val filterFavoritesOnly by viewModel.filterFavoritesOnly.collectAsState()
                                val selectedBatchKeys by viewModel.selectedBatchKeys.collectAsState()

                                ArchivioScreen(
                                    years = years,
                                    selectedYear = selectedYear,
                                    cartelle = cartelle,
                                    selectedCartella = selectedCartella,
                                    files = files,
                                    filesLoading = filesLoading,
                                    searchQuery = searchQuery,
                                    searchResults = searchResults,
                                    filterFavoritesOnly = filterFavoritesOnly,
                                    selectedBatchKeys = selectedBatchKeys,
                                    onSelectYear = { viewModel.selectYear(it) },
                                    onSelectCartella = { viewModel.selectCartella(it) },
                                    onSearchChange = { viewModel.setSearchQuery(it) },
                                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                                    onToggleFilterFavorites = { viewModel.toggleFilterFavoritesOnly() },
                                    onOpenFilePreview = { viewModel.setPreviewFile(it) },
                                    onToggleBatchKey = { viewModel.toggleSelectBatchKey(it) },
                                    onSelectAllBatch = { viewModel.selectAllFiles() },
                                    onClearBatch = { viewModel.clearBatchSelection() },
                                    onDownloadBatch = { viewModel.downloadBatchSelected() },
                                    onDownloadSingle = { file ->
                                        viewModel.showSnackbar("Download completato: ${file.nome}")
                                    }
                                )
                            }
                            1 -> {
                                val messaggiTab by viewModel.messaggiTab.collectAsState()
                                val attiviList by viewModel.attiviMessaggi.collectAsState()
                                val archiviatiList by viewModel.archiviatiMessaggi.collectAsState()
                                val expandedId by viewModel.expandedMsgId.collectAsState()

                                MessaggiScreen(
                                    activeTab = messaggiTab,
                                    attiviList = attiviList,
                                    archiviatiList = archiviatiList,
                                    expandedId = expandedId,
                                    onTabChange = { viewModel.setMessaggiTab(it) },
                                    onToggleExpand = { viewModel.toggleExpandMessage(it) },
                                    onToggleArchive = { viewModel.toggleArchiveMessage(it) },
                                    onSubmitUpload = { msgId, file ->
                                        viewModel.submitUploadReply(msgId, file)
                                    }
                                )
                            }
                            2 -> {
                                val cassettoFiles by viewModel.cassettoFiles.collectAsState()

                                CassettoScreen(
                                    cassettoFiles = cassettoFiles,
                                    showAddDialog = showAddCassetto,
                                    fileToRename = cassettoFileToRename,
                                    onOpenAddDialog = { viewModel.setShowAddCassetto(true) },
                                    onCloseAddDialog = { viewModel.setShowAddCassetto(false) },
                                    onAddDocument = { name, cat -> viewModel.addCassettoDocument(name, cat) },
                                    onOpenFileToRename = { viewModel.setCassettoFileToRename(it) },
                                    onCloseRenameDialog = { viewModel.setCassettoFileToRename(null) },
                                    onConfirmRename = { key, newName -> viewModel.renameCassettoDocument(key, newName) },
                                    onDeleteDocument = { viewModel.deleteCassettoDocument(it) },
                                    onOpenFilePreview = { viewModel.setPreviewFile(it) },
                                    onDownloadDocument = { file ->
                                        viewModel.showSnackbar("Scaricato in download: ${file.nome}")
                                    }
                                )
                            }
                            3 -> {
                                val auditLogs by viewModel.auditLogs.collectAsState()
                                AttivitaScreen(auditLogs = auditLogs)
                            }
                        }
                    }
                }
            }

            // Notifications Bottom Sheet
            if (showNotifSheet) {
                val notifiche by viewModel.notifiche.collectAsState()
                NotificheBottomSheet(
                    notifiche = notifiche,
                    onDismiss = { viewModel.setShowNotifSheet(false) },
                    onMarkAsRead = { viewModel.markNotificaLetta(it) },
                    onMarkAllAsRead = { viewModel.markAllNotificheLette() },
                    onClearRead = { viewModel.clearNotificheLette() }
                )
            }

            // Settings Bottom Sheet
            if (showSettingsSheet) {
                SettingsBottomSheet(
                    user = currentUser,
                    isDarkMode = isDarkMode,
                    fcmStatus = fcmStatus,
                    fcmTesting = fcmTesting,
                    onDismiss = { viewModel.setShowSettingsSheet(false) },
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onSendTestPush = { viewModel.sendTestPushNotification() },
                    onLogout = { viewModel.logout() }
                )
            }

            // Document Preview Dialog
            previewFile?.let { file ->
                DocumentPreviewDialog(
                    file = file,
                    onDismiss = { viewModel.setPreviewFile(null) },
                    onDownload = {
                        viewModel.showSnackbar("Download completato: ${file.nome}")
                    }
                )
            }
        }
    }
}
