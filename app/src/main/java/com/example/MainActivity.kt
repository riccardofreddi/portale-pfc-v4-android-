package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notification.LocalNotificationHelper
import com.example.ui.components.PfcTopBar
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PfcViewModel

class MainActivity : ComponentActivity() {

    private var pfcViewModel: PfcViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PfcViewModel = viewModel()
            pfcViewModel = viewModel
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppContent(viewModel = viewModel, onActivityIntent = { processIntent(intent, viewModel) })
            }
        }
        processIntent(intent, null)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pfcViewModel?.let { processIntent(intent, it) }
    }

    private fun processIntent(intent: Intent?, vm: PfcViewModel?) {
        if (intent == null || vm == null) return

        val targetTab = if (intent.hasExtra(LocalNotificationHelper.EXTRA_TARGET_TAB)) {
            intent.getIntExtra(LocalNotificationHelper.EXTRA_TARGET_TAB, 0)
        } else null

        val year = intent.getStringExtra(LocalNotificationHelper.EXTRA_YEAR)
        val folder = intent.getStringExtra(LocalNotificationHelper.EXTRA_FOLDER)
        val docKey = intent.getStringExtra(LocalNotificationHelper.EXTRA_DOC_KEY)
        val msgId = intent.getStringExtra(LocalNotificationHelper.EXTRA_MSG_ID)
        val showNotifSheet = intent.getBooleanExtra(LocalNotificationHelper.EXTRA_SHOW_NOTIF_SHEET, false)

        if (targetTab != null || showNotifSheet || msgId != null || folder != null) {
            vm.handleNotificationIntent(targetTab, year, folder, docKey, msgId, showNotifSheet)
        }
    }
}

@Composable
fun MainAppContent(viewModel: PfcViewModel, onActivityIntent: () -> Unit) {
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

    // Notification Reminder Settings
    val isRemindersEnabled by viewModel.isRemindersEnabled.collectAsState()
    val isRemindDocumentsEnabled by viewModel.isRemindDocumentsEnabled.collectAsState()
    val isRemindMessagesEnabled by viewModel.isRemindMessagesEnabled.collectAsState()
    val isRemindDeadlinesEnabled by viewModel.isRemindDeadlinesEnabled.collectAsState()
    val reminderIntervalMin by viewModel.reminderIntervalMin.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Runtime Permission Launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.updateNotificationPermissionStatus()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        onActivityIntent()
    }

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
                        userInitials = (currentUser?.name ?: "PF").take(2).uppercase(),
                        onNotifClick = { viewModel.setShowNotifSheet(true) },
                        onProfileClick = { viewModel.setShowSettingsSheet(true) }
                    )
                },
                bottomBar = {
                    val attiviList by viewModel.attiviMessaggi.collectAsState()
                    val unreadMsgCount = attiviList.count { !it.letto }

                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp,
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_nav_bar")
                    ) {
                        val navItemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GeoPrimary,
                            selectedTextColor = GeoPrimary,
                            indicatorColor = PfcGoldContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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
                            colors = navItemColors
                        )

                        // 1: Messaggi
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.setTab(1) },
                            icon = {
                                BadgedBox(badge = {
                                    if (unreadMsgCount > 0) {
                                        Badge(
                                            containerColor = GeoPrimary,
                                            contentColor = Color.White
                                        ) {
                                            Text(unreadMsgCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (selectedTab == 1) Icons.Filled.Chat else Icons.Outlined.Chat,
                                        contentDescription = "Messaggi"
                                    )
                                }
                            },
                            label = {
                                Text(
                                    "Messaggi",
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            },
                            colors = navItemColors
                        )

                        // 2: Cassetto
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.setTab(2) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == 2) Icons.Filled.CloudUpload else Icons.Outlined.CloudUpload,
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
                            colors = navItemColors
                        )

                        // 3: Attivita
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
                            colors = navItemColors
                        )
                    }
                },
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "tab_transition"
                    ) { target ->
                        when (target) {
                            0 -> {
                                val years by viewModel.years.collectAsState()
                                val selectedYear by viewModel.selectedYear.collectAsState()
                                val cartelle by viewModel.cartelle.collectAsState()
                                val selectedCartella by viewModel.selectedCartella.collectAsState()
                                val files by viewModel.files.collectAsState()
                                val filesLoading by viewModel.filesLoading.collectAsState()
                                val searchQuery by viewModel.searchQuery.collectAsState()
                                val searchResults by viewModel.searchResults.collectAsState()
                                val isSearching by viewModel.isSearching.collectAsState()
                                val filterFavoritesOnly by viewModel.filterFavoritesOnly.collectAsState()
                                val selectedBatchKeys by viewModel.selectedBatchKeys.collectAsState()
                                val archivioViewMode by viewModel.archivioViewMode.collectAsState()

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
                                    archivioViewMode = archivioViewMode,
                                    onSetArchivioViewMode = { viewModel.setArchivioViewMode(it) },
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
                                    onDownloadSingle = { viewModel.downloadDocument(it) }
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
                                    onSubmitUploadFile = { msgId, file ->
                                        viewModel.submitUploadReplyRealFile(msgId, file)
                                    },
                                    onMarkAllAsRead = { viewModel.markAllMessaggiAsRead() },
                                    onToggleRead = { viewModel.toggleReadMessage(it) }
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
                                        viewModel.downloadCassettoDocument(file)
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
                    onNotificaClick = { viewModel.onNotificaClicked(it) },
                    onMarkAsRead = { viewModel.markNotificaLetta(it) },
                    onMarkAllAsRead = { viewModel.markAllNotificheLette() },
                    onClearRead = { viewModel.clearNotificheLette() }
                )
            }

            // Settings Bottom Sheet (with local reminders & diagnostics)
            if (showSettingsSheet) {
                SettingsBottomSheet(
                    user = currentUser,
                    isDarkMode = isDarkMode,
                    fcmStatus = fcmStatus,
                    fcmTesting = fcmTesting,
                    isRemindersEnabled = isRemindersEnabled,
                    isRemindDocumentsEnabled = isRemindDocumentsEnabled,
                    isRemindMessagesEnabled = isRemindMessagesEnabled,
                    isRemindDeadlinesEnabled = isRemindDeadlinesEnabled,
                    reminderIntervalMin = reminderIntervalMin,
                    hasNotificationPermission = hasNotificationPermission,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.updateNotificationPermissionStatus()
                        }
                    },
                    onToggleReminders = { viewModel.setRemindersEnabled(it) },
                    onToggleRemindDocuments = { viewModel.setRemindDocumentsEnabled(it) },
                    onToggleRemindMessages = { viewModel.setRemindMessagesEnabled(it) },
                    onToggleRemindDeadlines = { viewModel.setRemindDeadlinesEnabled(it) },
                    onSetReminderInterval = { viewModel.setReminderInterval(it) },
                    onTestDocumentNotification = { viewModel.triggerTestDocumentNotification() },
                    onTestMessageNotification = { viewModel.triggerTestMessageNotification() },
                    onTestDeadlineNotification = { viewModel.triggerTestDeadlineNotification() },
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
                        viewModel.downloadDocument(file)
                    },
                    onToggleFavorite = { viewModel.toggleFavorite(file) }
                )
            }
        }
    }
}
