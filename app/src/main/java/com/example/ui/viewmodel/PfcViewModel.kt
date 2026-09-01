package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.repository.PfcRepository
import com.example.notification.LocalNotificationHelper
import com.example.notification.ReminderScheduler
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class PfcViewModel(application: Application) : AndroidViewModel(application) {

    val repository = PfcRepository(application)
    private val prefs = application.getSharedPreferences("pfc_app_settings", Context.MODE_PRIVATE)

    // === Onboarding & Auth State ===
    private val _onboardingDone = MutableStateFlow(prefs.getBoolean("onboarding_done", false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(repository.getCurrentUser())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(repository.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _loginLoading = MutableStateFlow(false)
    val loginLoading: StateFlow<Boolean> = _loginLoading.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // === Navigation State ===
    private val _selectedTab = MutableStateFlow(0) // 0: Archivio, 1: Messaggi, 2: Cassetto, 3: Attivita
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // === Sheet & Dialog State ===
    private val _showNotifSheet = MutableStateFlow(false)
    val showNotifSheet: StateFlow<Boolean> = _showNotifSheet.asStateFlow()

    private val _showSettingsSheet = MutableStateFlow(false)
    val showSettingsSheet: StateFlow<Boolean> = _showSettingsSheet.asStateFlow()

    private val _previewFile = MutableStateFlow<FileItem?>(null)
    val previewFile: StateFlow<FileItem?> = _previewFile.asStateFlow()

    private val _showAddCassetto = MutableStateFlow(false)
    val showAddCassetto: StateFlow<Boolean> = _showAddCassetto.asStateFlow()

    private val _cassettoFileToRename = MutableStateFlow<CachedCassettoEntity?>(null)
    val cassettoFileToRename: StateFlow<CachedCassettoEntity?> = _cassettoFileToRename.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // === Archivio State ===
    private val _years = MutableStateFlow<List<String>>(listOf("2025"))
    val years: StateFlow<List<String>> = _years.asStateFlow()

    private val _selectedYear = MutableStateFlow("2025")
    val selectedYear: StateFlow<String> = _selectedYear.asStateFlow()

    private val _cartelle = MutableStateFlow<List<Cartella>>(emptyList())
    val cartelle: StateFlow<List<Cartella>> = _cartelle.asStateFlow()

    private val _selectedCartella = MutableStateFlow<Cartella?>(null)
    val selectedCartella: StateFlow<Cartella?> = _selectedCartella.asStateFlow()

    private val _files = MutableStateFlow<List<FileItem>>(emptyList())
    val files: StateFlow<List<FileItem>> = _files.asStateFlow()

    private val _filesLoading = MutableStateFlow(false)
    val filesLoading: StateFlow<Boolean> = _filesLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _filterFavoritesOnly = MutableStateFlow(false)
    val filterFavoritesOnly: StateFlow<Boolean> = _filterFavoritesOnly.asStateFlow()

    private val _selectedBatchKeys = MutableStateFlow<Set<String>>(emptySet())
    val selectedBatchKeys: StateFlow<Set<String>> = _selectedBatchKeys.asStateFlow()

    private val _archivioViewMode = MutableStateFlow(0) // 0: Cartelle, 1: Tutti i File
    val archivioViewMode: StateFlow<Int> = _archivioViewMode.asStateFlow()

    // === Messaggi State ===
    private val _messaggiTab = MutableStateFlow(0) // 0: Attivi, 1: Archiviati
    val messaggiTab: StateFlow<Int> = _messaggiTab.asStateFlow()

    val attiviMessaggi: StateFlow<List<CachedMessaggioEntity>> = repository.getMessaggiFlow(false)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archiviatiMessaggi: StateFlow<List<CachedMessaggioEntity>> = repository.getMessaggiFlow(true)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _expandedMsgId = MutableStateFlow<String?>(null)
    val expandedMsgId: StateFlow<String?> = _expandedMsgId.asStateFlow()

    // === Cassetto State ===
    val cassettoFiles: StateFlow<List<CachedCassettoEntity>> = repository.getCassettoFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // === Notifiche State ===
    val notifiche: StateFlow<List<CachedNotificaEntity>> = repository.getNotificheFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotifCount: StateFlow<Int> = notifiche.map { list ->
        list.count { !it.letta }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // === Attività / Audit State ===
    val auditLogs: StateFlow<List<CachedAuditEntity>> = repository.getAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // === FCM Push State ===
    private val _fcmStatus = MutableStateFlow<FcmStatusResponse?>(null)
    val fcmStatus: StateFlow<FcmStatusResponse?> = _fcmStatus.asStateFlow()

    private val _fcmTesting = MutableStateFlow(false)
    val fcmTesting: StateFlow<Boolean> = _fcmTesting.asStateFlow()

    // === Dark Mode Preference ===
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // === Local Notification & Reminder Settings ===
    private val _isRemindersEnabled = MutableStateFlow(ReminderScheduler.isRemindersEnabled(application))
    val isRemindersEnabled: StateFlow<Boolean> = _isRemindersEnabled.asStateFlow()

    private val _isRemindDocumentsEnabled = MutableStateFlow(ReminderScheduler.isRemindDocumentsEnabled(application))
    val isRemindDocumentsEnabled: StateFlow<Boolean> = _isRemindDocumentsEnabled.asStateFlow()

    private val _isRemindMessagesEnabled = MutableStateFlow(ReminderScheduler.isRemindMessagesEnabled(application))
    val isRemindMessagesEnabled: StateFlow<Boolean> = _isRemindMessagesEnabled.asStateFlow()

    private val _isRemindDeadlinesEnabled = MutableStateFlow(ReminderScheduler.isRemindDeadlinesEnabled(application))
    val isRemindDeadlinesEnabled: StateFlow<Boolean> = _isRemindDeadlinesEnabled.asStateFlow()

    private val _reminderIntervalMin = MutableStateFlow(ReminderScheduler.getIntervalMinutes(application))
    val reminderIntervalMin: StateFlow<Int> = _reminderIntervalMin.asStateFlow()

    private val _hasNotificationPermission = MutableStateFlow(LocalNotificationHelper.areNotificationsEnabled(application))
    val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    init {
        LocalNotificationHelper.createNotificationChannels(application)
        if (ReminderScheduler.isRemindersEnabled(application)) {
            ReminderScheduler.scheduleReminders(application)
        }

        // Register FCM device token on backend
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        viewModelScope.launch(Dispatchers.IO) {
                            repository.registerFcmToken(token)
                        }
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.i("PfcViewModel", "Firebase push not active on emulator/device: ${e.message}")
                    }
            } catch (e: Throwable) {
                android.util.Log.i("PfcViewModel", "Firebase messaging init skipped: ${e.message}")
            }
        }

        if (_isLoggedIn.value) {
            syncAllData()
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_done", true).apply()
        _onboardingDone.value = true
    }

    fun login(user: String, pass: String) {
        if (user.isBlank() || pass.isBlank()) {
            _loginError.value = "Compila tutti i campi"
            return
        }

        viewModelScope.launch {
            _loginLoading.value = true
            _loginError.value = null

            val result = repository.login(user, pass)
            result.onSuccess { u ->
                _currentUser.value = u
                _isLoggedIn.value = true
                _loginLoading.value = false
                
                // Re-register FCM token on server with authenticated user session
                try {
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        viewModelScope.launch(Dispatchers.IO) {
                            repository.registerFcmToken(token)
                        }
                    }
                } catch (_: Throwable) {}

                syncAllData()
                showSnackbar("Benvenuto, ${u.name}!")
            }.onFailure { err ->
                _loginLoading.value = false
                _loginError.value = err.message ?: "Accesso fallito"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _isLoggedIn.value = false
            _currentUser.value = null
            _showSettingsSheet.value = false
            _selectedTab.value = 0
            showSnackbar("Disconnessione effettuata")
        }
    }

    fun syncAllData() {
        viewModelScope.launch {
            refreshArchivio()
            repository.syncMessaggi()
            repository.syncCassetto()
            repository.syncNotifiche()
            repository.syncAuditLogs()
        }
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
        viewModelScope.launch {
            when (index) {
                0 -> refreshArchivio()
                1 -> repository.syncMessaggi()
                2 -> repository.syncCassetto()
                3 -> repository.syncAuditLogs()
            }
        }
    }

    fun setShowNotifSheet(show: Boolean) {
        _showNotifSheet.value = show
        if (show) {
            viewModelScope.launch {
                repository.syncNotifiche()
            }
        }
    }

    fun setShowSettingsSheet(show: Boolean) {
        _showSettingsSheet.value = show
        if (show) {
            loadFcmStatus()
        }
    }

    fun setPreviewFile(file: FileItem?) {
        _previewFile.value = file
        if (file != null) {
            viewModelScope.launch {
                repository.markDocumentVisto(file)
                _files.value = _files.value.map {
                    if (it.key == file.key && it.stato == "nuovo") it.copy(stato = "visto") else it
                }
                val updatedFolders = repository.getCartelle(_selectedYear.value)
                if (updatedFolders.isNotEmpty()) _cartelle.value = updatedFolders
            }
        }
    }

    fun downloadDocument(file: FileItem) {
        viewModelScope.launch {
            repository.markDocumentScaricato(file)
            _files.value = _files.value.map {
                if (it.key == file.key) it.copy(stato = "scaricato") else it
            }
            val updatedFolders = repository.getCartelle(_selectedYear.value)
            if (updatedFolders.isNotEmpty()) _cartelle.value = updatedFolders
            showSnackbar("Download avviato: ${file.nome}")
        }
    }

    fun setArchivioViewMode(mode: Int) {
        _archivioViewMode.value = mode
        _selectedCartella.value = null
        _selectedBatchKeys.value = emptySet()
        if (mode == 1) {
            loadAllDocumentsForYear(_selectedYear.value)
        } else {
            refreshArchivio()
        }
    }

    fun loadAllDocumentsForYear(year: String) {
        viewModelScope.launch {
            _filesLoading.value = true
            val fList = repository.getAllDocumentsForYear(year)
            _files.value = fList
            _filesLoading.value = false
        }
    }

    fun setShowAddCassetto(show: Boolean) {
        _showAddCassetto.value = show
    }

    fun setCassettoFileToRename(file: CachedCassettoEntity?) {
        _cassettoFileToRename.value = file
    }

    fun showSnackbar(msg: String) {
        _snackbarMessage.value = msg
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        prefs.edit().putBoolean("dark_mode", next).apply()
    }

    // === Archivio Actions ===

    fun selectYear(year: String) {
        _selectedYear.value = year
        _selectedCartella.value = null
        _selectedBatchKeys.value = emptySet()
        refreshArchivio(forcedYear = year)
    }

    fun selectCartella(cartella: Cartella?) {
        _selectedCartella.value = cartella
        _selectedBatchKeys.value = emptySet()
        if (cartella != null) {
            loadFilesForCartella(_selectedYear.value, cartella.nome)
        }
    }

    fun refreshArchivio(forcedYear: String? = null) {
        viewModelScope.launch {
            _filesLoading.value = true
            val targetYr = forcedYear ?: _selectedYear.value.ifEmpty { null }
            val (serverYears, folders, _) = repository.fetchArchivioData(targetYr)

            if (serverYears.isNotEmpty()) {
                _years.value = serverYears
                if (_selectedYear.value.isEmpty() || !serverYears.contains(_selectedYear.value)) {
                    _selectedYear.value = if (targetYr != null && serverYears.contains(targetYr)) targetYr else serverYears.first()
                }
            }
            _cartelle.value = folders

            val currentFolder = _selectedCartella.value
            if (currentFolder != null && folders.any { it.nome == currentFolder.nome }) {
                loadFilesForCartella(_selectedYear.value, currentFolder.nome)
            } else {
                _filesLoading.value = false
            }
        }
    }

    private fun loadFilesForCartella(year: String, folder: String) {
        viewModelScope.launch {
            _filesLoading.value = true
            val fList = repository.getFiles(year, folder)
            _files.value = fList
            _filesLoading.value = false
        }
    }

    fun toggleFavorite(file: FileItem) {
        viewModelScope.launch {
            val newFav = repository.togglePreferito(file)
            _files.value = _files.value.map {
                if (it.key == file.key) it.copy(isPreferito = newFav) else it
            }
            showSnackbar(if (newFav) "Aggiunto ai preferiti" else "Rimosso dai preferiti")
        }
    }

    fun toggleFilterFavoritesOnly() {
        _filterFavoritesOnly.value = !_filterFavoritesOnly.value
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        if (q.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
        } else {
            _isSearching.value = true
            viewModelScope.launch {
                val results = repository.searchDocuments(q)
                _searchResults.value = results
                _isSearching.value = false
            }
        }
    }

    fun toggleSelectBatchKey(key: String) {
        val current = _selectedBatchKeys.value.toMutableSet()
        if (current.contains(key)) current.remove(key) else current.add(key)
        _selectedBatchKeys.value = current
    }

    fun selectAllFiles() {
        val allKeys = _files.value.map { it.key }.toSet()
        _selectedBatchKeys.value = if (_selectedBatchKeys.value.size == allKeys.size) emptySet() else allKeys
    }

    fun clearBatchSelection() {
        _selectedBatchKeys.value = emptySet()
    }

    fun downloadBatchSelected() {
        val keys = _selectedBatchKeys.value
        val count = keys.size
        viewModelScope.launch {
            keys.forEach { key ->
                val file = _files.value.find { it.key == key }
                if (file != null) repository.markDocumentScaricato(file)
            }
            _files.value = _files.value.map {
                if (keys.contains(it.key)) it.copy(stato = "scaricato") else it
            }
            repository.logAction("download_batch", "Scaricati $count documenti in blocco")
            clearBatchSelection()
            showSnackbar("$count documenti salvati con successo")
        }
    }

    // === Messaggi Actions ===

    fun setMessaggiTab(tab: Int) {
        _messaggiTab.value = tab
    }

    fun toggleExpandMessage(id: String) {
        _expandedMsgId.value = if (_expandedMsgId.value == id) null else id
        if (_expandedMsgId.value == id) {
            viewModelScope.launch {
                repository.setMessaggioLetto(id, true)
            }
        }
    }

    fun toggleArchiveMessage(msg: CachedMessaggioEntity) {
        viewModelScope.launch {
            repository.setMessaggioArchiviato(msg.id, !msg.archiviato)
            showSnackbar(if (!msg.archiviato) "Messaggio archiviato" else "Messaggio ripristinato")
        }
    }

    fun submitUploadReply(msgId: String, fileName: String) {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val cacheFile = File(app.cacheDir, fileName).apply {
                if (!exists()) writeText("Allegato risposta messaggio #$msgId")
            }
            repository.uploadRispostaMessaggio(msgId, cacheFile)
            showSnackbar("Risposta e file inviati allo studio con successo!")
        }
    }

    // === Cassetto Actions ===

    fun addCassettoDocument(name: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val app = getApplication<Application>()
            val cacheFile = File(app.cacheDir, name).apply {
                if (!exists()) writeText("Documento personale cassetto: $name")
            }
            repository.uploadCassettoFile(cacheFile, category)
            _showAddCassetto.value = false
            showSnackbar("Documento caricato nel cassetto!")
        }
    }

    fun renameCassettoDocument(key: String, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameCassettoFile(key, newName.trim())
            _cassettoFileToRename.value = null
            showSnackbar("Documento rinominato con successo")
        }
    }

    fun deleteCassettoDocument(file: CachedCassettoEntity) {
        viewModelScope.launch {
            repository.deleteCassettoFile(file.key, file.nome)
            showSnackbar("Documento eliminato dal cassetto")
        }
    }

    // === Notifiche Actions ===

    fun markNotificaLetta(id: String) {
        viewModelScope.launch {
            repository.markNotificaLetta(id)
        }
    }

    fun markAllNotificheLette() {
        viewModelScope.launch {
            repository.markAllNotificheLette()
            showSnackbar("Tutte le notifiche segnate come lette")
        }
    }

    fun clearNotificheLette() {
        viewModelScope.launch {
            repository.clearNotificheLette()
            showSnackbar("Notifiche eliminate")
        }
    }

    // === FCM Push Diagnostics ===

    private fun loadFcmStatus() {
        viewModelScope.launch {
            val status = repository.getFcmStatus()
            _fcmStatus.value = status
        }
    }

    fun sendTestPushNotification() {
        viewModelScope.launch {
            _fcmTesting.value = true
            val res = repository.sendTestFcm()
            _fcmTesting.value = false
            showSnackbar(res.msg ?: "Notifica push richiesta al backend")
        }
    }

    // === Local Notification Reminders & Controls ===

    fun updateNotificationPermissionStatus() {
        val app = getApplication<Application>()
        _hasNotificationPermission.value = LocalNotificationHelper.areNotificationsEnabled(app)
    }

    fun setRemindersEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        ReminderScheduler.setRemindersEnabled(app, enabled)
        _isRemindersEnabled.value = enabled
        showSnackbar(if (enabled) "Promemoria automatici attivati" else "Promemoria automatici disattivati")
    }

    fun setRemindDocumentsEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        ReminderScheduler.setRemindDocumentsEnabled(app, enabled)
        _isRemindDocumentsEnabled.value = enabled
    }

    fun setRemindMessagesEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        ReminderScheduler.setRemindMessagesEnabled(app, enabled)
        _isRemindMessagesEnabled.value = enabled
    }

    fun setRemindDeadlinesEnabled(enabled: Boolean) {
        val app = getApplication<Application>()
        ReminderScheduler.setRemindDeadlinesEnabled(app, enabled)
        _isRemindDeadlinesEnabled.value = enabled
    }

    fun setReminderInterval(minutes: Int) {
        val app = getApplication<Application>()
        ReminderScheduler.setIntervalMinutes(app, minutes)
        _reminderIntervalMin.value = minutes
        showSnackbar("Frequenza impostata su: ${getIntervalLabel(minutes)}")
    }

    fun triggerTestDocumentNotification() {
        val app = getApplication<Application>()
        LocalNotificationHelper.showNewDocumentNotification(
            context = app,
            docTitle = "Modello F24 - Versamento Tributi e Contributi",
            folderName = "F24 e Versamenti",
            year = _selectedYear.value,
            docKey = "${_selectedYear.value}/F24/F24_Versamento.pdf"
        )
        showSnackbar("Notifica locale 'Nuovo Documento' inviata!")
    }

    fun triggerTestMessageNotification() {
        val app = getApplication<Application>()
        LocalNotificationHelper.showNewMessageNotification(
            context = app,
            messageId = "msg-${System.currentTimeMillis()}",
            title = "Comunicazione dallo Studio PFC",
            corpo = "Gentile cliente, sono disponibili nuovi documenti nella tua area riservata.",
            requiresUpload = false
        )
        showSnackbar("Notifica locale 'Messaggio Studio' inviata!")
    }

    fun triggerTestDeadlineNotification() {
        val app = getApplication<Application>()
        LocalNotificationHelper.showDeadlineReminderNotification(
            context = app,
            deadlineTitle = "Scadenza Tributaria F24",
            scadenzaDate = "16 del mese",
            detail = "Verifica gli adempimenti predisposti dal tuo commercialista nell'archivio."
        )
        showSnackbar("Notifica locale 'Promemoria Scadenza' inviata!")
    }

    fun getIntervalLabel(minutes: Int): String {
        return when (minutes) {
            15 -> "Ogni 15 Minuti"
            60 -> "Ogni Ora"
            360 -> "Ogni 6 Ore"
            1440 -> "Una volta al Giorno"
            else -> "$minutes Minuti"
        }
    }

    fun triggerPeriodicCheckNow() {
        val app = getApplication<Application>()
        ReminderScheduler.triggerImmediateCheck(app)
        syncAllData()
        showSnackbar("Sincronizzazione e verifica promemoria eseguita!")
    }

    fun handleNotificationIntent(
        targetTab: Int?,
        year: String?,
        folder: String?,
        docKey: String?,
        msgId: String?,
        showNotifSheet: Boolean
    ) {
        if (showNotifSheet) {
            _showNotifSheet.value = true
        }
        if (targetTab != null) {
            _selectedTab.value = targetTab
            if (targetTab == 0 && year != null) {
                _selectedYear.value = year
                if (folder != null) {
                    val targetCartella = _cartelle.value.find { it.nome == folder } ?: Cartella(nome = folder, count = 1, nuovi = 1)
                    _selectedCartella.value = targetCartella
                    loadFilesForCartella(year, folder)
                }
            } else if (targetTab == 1 && msgId != null) {
                _messaggiTab.value = 0
                _expandedMsgId.value = msgId
            }
        }
    }
}
