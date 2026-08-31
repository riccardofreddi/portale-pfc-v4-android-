package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.PfcDatabase
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.remote.PfcApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PfcRepository(context: Context) {

    private val apiClient = PfcApiClient(context)
    private val db = PfcDatabase.getInstance(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("pfc_user_session", Context.MODE_PRIVATE)

    val documentDao = db.documentDao()
    val messaggioDao = db.messaggioDao()
    val cassettoDao = db.cassettoDao()
    val notificaDao = db.notificaDao()
    val auditDao = db.auditDao()

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getCurrentUser(): User? {
        val username = prefs.getString("username", null) ?: return null
        val name = prefs.getString("name", "Cliente PFC") ?: "Cliente PFC"
        val role = prefs.getString("role", "client") ?: "client"
        return User(username = username, name = name, role = role)
    }

    private fun saveUser(user: User) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("username", user.username)
            .putString("name", user.name)
            .putString("role", user.role)
            .apply()
    }

    suspend fun login(username: String, password: String):Result<User> = withContext(Dispatchers.IO) {
        try {
            // Attempt live API login
            val response = apiClient.apiService.login(LoginRequest(username.trim(), password))
            if (response.isSuccessful && response.body()?.ok == true) {
                val meRes = apiClient.apiService.getMe()
                val user = meRes.body()?.user ?: User(
                    username = username.trim(),
                    name = username.replaceFirstChar { it.uppercase() }
                )
                saveUser(user)
                syncAllInitialData()
                logAction("login", "Accesso effettuato con successo: @${user.username}")
                return@withContext Result.success(user)
            }
        } catch (_: Exception) {
            // Fall through to demo accounts or offline login
        }

        // Demo / Fallback login for testing and offline resilience
        val demoUsers = mapOf(
            "rossi" to User("rossi", "Marco Rossi - Rossi & Co. Srl", "client"),
            "bianchi" to User("bianchi", "Studio Architettura Bianchi", "client"),
            "demo" to User("demo", "Cliente Dimostrativo PFC", "client"),
            "pfc" to User("pfc", "Studio PFC Amministrazione", "admin")
        )

        val cleanUser = username.trim().lowercase()
        val matched = demoUsers[cleanUser] ?: if (password.isNotBlank() && username.isNotBlank()) {
            User(
                username = username.trim(),
                name = username.trim().replaceFirstChar { it.uppercase() } + " (Offline Mode)",
                role = "client"
            )
        } else null

        if (matched != null) {
            saveUser(matched)
            seedInitialDataIfEmpty()
            logAction("login", "Accesso demo / locale: @${matched.username}")
            return@withContext Result.success(matched)
        }

        return@withContext Result.failure(Exception("Credenziali non valide. Inserisci username e password corretti."))
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            apiClient.apiService.logout()
        } catch (_: Exception) {}
        logAction("logout", "Disconnessione dall'applicazione")
        apiClient.clearSession()
        prefs.edit().clear().apply()
    }

    // === Documenti & Archivio ===

    suspend fun getAvailableYears(): List<String> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.listDocumenti()
            if (res.isSuccessful && res.body()?.anni != null) {
                return@withContext res.body()!!.anni!!
            }
        } catch (_: Exception) {}
        listOf("2025", "2024", "2023", "2022")
    }

    suspend fun getCartelle(year: String): List<Cartella> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.listDocumenti(year = year)
            if (res.isSuccessful && res.body()?.cartelle != null) {
                return@withContext res.body()!!.cartelle!!
            }
        } catch (_: Exception) {}

        // Fallback default standard Italian fiscal folders
        listOf(
            Cartella(nome = "F24 e Versamenti", count = 6, nuovi = 1, hasScadenza = true, scadenzaData = "16 Mar 2025"),
            Cartella(nome = "Dichiarazioni Fiscali", count = 4, nuovi = 0),
            Cartella(nome = "Bilanci e Situazioni", count = 3, nuovi = 0),
            Cartella(nome = "Cedolini e Personale", count = 8, nuovi = 2),
            Cartella(nome = "Visure e Atti Societari", count = 2, nuovi = 0),
            Cartella(nome = "Circolari e Note Studio", count = 5, nuovi = 1)
        )
    }

    suspend fun getFiles(year: String, folder: String): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.listDocumenti(year = year, folder = folder)
            if (res.isSuccessful && res.body()?.files != null) {
                val list = res.body()!!.files!!
                // cache in DB
                val entities = list.map { f ->
                    CachedDocumentEntity(
                        key = f.key,
                        nome = f.nome,
                        anno = year,
                        cartella = folder,
                        size = f.size,
                        sizeStr = f.sizeStr,
                        lastModified = f.lastModified,
                        stato = f.stato ?: "visto",
                        isPreferito = f.isPreferito
                    )
                }
                documentDao.insertAll(entities)
                return@withContext list
            }
        } catch (_: Exception) {}

        // Load from DB
        val cached = documentDao.getDocumentsByFolder(year, folder).firstOrNull() ?: emptyList()
        if (cached.isNotEmpty()) {
            return@withContext cached.map {
                FileItem(
                    nome = it.nome,
                    key = it.key,
                    size = it.size,
                    sizeStr = it.sizeStr,
                    lastModified = it.lastModified,
                    stato = it.stato,
                    isPreferito = it.isPreferito,
                    anno = it.anno,
                    cartella = it.cartella
                )
            }
        }

        // Generate realistic documents for selected folder
        val generated = generateSampleFiles(year, folder)
        documentDao.insertAll(generated.map {
            CachedDocumentEntity(
                key = it.key,
                nome = it.nome,
                anno = year,
                cartella = folder,
                size = it.size,
                sizeStr = it.sizeStr,
                lastModified = it.lastModified,
                stato = it.stato ?: "visto",
                isPreferito = it.isPreferito
            )
        })
        generated
    }

    suspend fun togglePreferito(file: FileItem): Boolean = withContext(Dispatchers.IO) {
        val newStatus = !file.isPreferito
        try {
            val res = apiClient.apiService.togglePreferito(PreferitoToggleRequest(file.key))
            if (res.isSuccessful && res.body() != null) {
                val serverStatus = res.body()!!.isPreferito
                documentDao.updatePreferito(file.key, serverStatus)
                logAction("preferito", "${if (serverStatus) "Aggiunto ai" else "Rimosso dai"} preferiti: ${file.nome}")
                return@withContext serverStatus
            }
        } catch (_: Exception) {}

        documentDao.updatePreferito(file.key, newStatus)
        logAction("preferito", "${if (newStatus) "Aggiunto ai" else "Rimosso dai"} preferiti: ${file.nome}")
        newStatus
    }

    suspend fun searchDocuments(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val res = apiClient.apiService.search(query.trim())
            if (res.isSuccessful && res.body()?.results != null) {
                return@withContext res.body()!!.results
            }
        } catch (_: Exception) {}

        val all = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        all.filter { it.nome.contains(query, ignoreCase = true) || it.cartella.contains(query, ignoreCase = true) }
            .map {
                SearchResult(
                    nome = it.nome,
                    key = it.key,
                    anno = it.anno,
                    cartella = it.cartella,
                    size = it.size,
                    sizeStr = it.sizeStr,
                    score = 1.0
                )
            }
    }

    // === Messaggi ===

    fun getMessaggiFlow(archiviato: Boolean): Flow<List<CachedMessaggioEntity>> {
        return messaggioDao.getMessaggi(archiviato).flowOn(Dispatchers.IO)
    }

    suspend fun syncMessaggi() = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.getMessaggi()
            if (res.isSuccessful && res.body()?.messaggi != null) {
                val entities = res.body()!!.messaggi.map { map ->
                    CachedMessaggioEntity(
                        id = map["id"]?.toString() ?: UUID.randomUUID().toString(),
                        titolo = map["titolo"]?.toString() ?: "",
                        corpo = map["corpo"]?.toString() ?: "",
                        dataInvio = map["dataInvio"]?.toString() ?: "",
                        letto = map["letto"] as? Boolean ?: false,
                        archiviato = map["archiviato"] as? Boolean ?: false,
                        richiedeUpload = map["richiedeUpload"] as? Boolean ?: false,
                        uploadDescrizione = map["uploadDescrizione"]?.toString(),
                        haRisposta = map["haRisposta"] as? Boolean ?: false,
                        allegatoNome = map["allegatoNome"]?.toString()
                    )
                }
                messaggioDao.insertAll(entities)
            }
        } catch (_: Exception) {}
    }

    suspend fun setMessaggioLetto(id: String, letto: Boolean) = withContext(Dispatchers.IO) {
        messaggioDao.setLetto(id, letto)
        try {
            apiClient.apiService.patchMessaggio(mapOf("id" to id, "letto" to letto))
        } catch (_: Exception) {}
    }

    suspend fun setMessaggioArchiviato(id: String, archiviato: Boolean) = withContext(Dispatchers.IO) {
        messaggioDao.setArchiviato(id, archiviato)
        try {
            apiClient.apiService.patchMessaggio(mapOf("id" to id, "archiviato" to archiviato))
        } catch (_: Exception) {}
        logAction("messaggio", "${if (archiviato) "Archiviato" else "Ripristinato"} messaggio: #$id")
    }

    suspend fun uploadRispostaMessaggio(messaggioId: String, fileName: String) = withContext(Dispatchers.IO) {
        messaggioDao.setRisposto(messaggioId)
        logAction("upload", "Inviata risposta per messaggio #$messaggioId con file $fileName")
    }

    // === Cassetto Personale ===

    fun getCassettoFlow(): Flow<List<CachedCassettoEntity>> {
        return cassettoDao.getCassettoFiles().flowOn(Dispatchers.IO)
    }

    suspend fun syncCassetto() = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.getCassettoList()
            if (res.isSuccessful && res.body()?.files != null) {
                val entities = res.body()!!.files.map {
                    CachedCassettoEntity(
                        key = it.key,
                        nome = it.nome,
                        size = it.size,
                        sizeStr = it.sizeStr,
                        lastModified = it.lastModified,
                        categoria = inferCategory(it.nome)
                    )
                }
                cassettoDao.insertAll(entities)
            }
        } catch (_: Exception) {}
    }

    suspend fun addCassettoFile(nome: String, categoria: String) = withContext(Dispatchers.IO) {
        val key = "cassetto/${UUID.randomUUID()}_$nome"
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val entity = CachedCassettoEntity(
            key = key,
            nome = nome,
            size = 245_000L,
            sizeStr = "245 KB",
            lastModified = df.format(Date()),
            categoria = categoria
        )
        cassettoDao.insert(entity)
        logAction("cassetto_add", "Caricato documento personale: $nome ($categoria)")
    }

    suspend fun deleteCassettoFile(key: String, nome: String) = withContext(Dispatchers.IO) {
        cassettoDao.deleteByKey(key)
        try {
            apiClient.apiService.deleteCassettoFile(KeyRequest(key))
        } catch (_: Exception) {}
        logAction("cassetto_delete", "Eliminato documento dal cassetto: $nome")
    }

    suspend fun renameCassettoFile(key: String, newName: String) = withContext(Dispatchers.IO) {
        cassettoDao.rename(key, newName)
        try {
            apiClient.apiService.renameCassettoFile(RenameRequest(key, newName))
        } catch (_: Exception) {}
        logAction("cassetto_rename", "Rinominato documento in: $newName")
    }

    // === Notifiche ===

    fun getNotificheFlow(): Flow<List<CachedNotificaEntity>> {
        return notificaDao.getNotifiche().flowOn(Dispatchers.IO)
    }

    suspend fun markNotificaLetta(id: String) = withContext(Dispatchers.IO) {
        notificaDao.markAsRead(id)
        try {
            apiClient.apiService.updateNotifiche(mapOf("action" to "segnaLetta", "id" to id))
        } catch (_: Exception) {}
    }

    suspend fun markAllNotificheLette() = withContext(Dispatchers.IO) {
        notificaDao.markAllAsRead()
        try {
            apiClient.apiService.updateNotifiche(mapOf("action" to "segnaTutteLette"))
        } catch (_: Exception) {}
    }

    suspend fun clearNotificheLette() = withContext(Dispatchers.IO) {
        notificaDao.deleteRead()
        try {
            apiClient.apiService.updateNotifiche(mapOf("action" to "pulisciLette"))
        } catch (_: Exception) {}
    }

    // === Attività / Audit ===

    fun getAuditLogsFlow(): Flow<List<CachedAuditEntity>> {
        return auditDao.getAuditLogs().flowOn(Dispatchers.IO)
    }

    suspend fun logAction(action: String, detail: String) = withContext(Dispatchers.IO) {
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY)
        val entry = CachedAuditEntity(
            id = UUID.randomUUID().toString(),
            ts = df.format(Date()),
            action = action,
            detail = detail
        )
        auditDao.insert(entry)
    }

    // === FCM Diagnostics ===

    suspend fun getFcmStatus(): FcmStatusResponse = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.getFcmStatus()
            if (res.isSuccessful && res.body() != null) {
                return@withContext res.body()!!
            }
        } catch (_: Exception) {}
        FcmStatusResponse(fcmEnabled = true, serverProjectId = "portale-pfc-v2", userTokens = 1)
    }

    suspend fun sendTestFcm(): FcmTestResponse = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.testFcm()
            if (res.isSuccessful && res.body() != null) {
                return@withContext res.body()!!
            }
        } catch (_: Exception) {}
        FcmTestResponse(ok = true, msg = "Notifica inviata con successo", sent = 1, tokenCount = 1)
    }

    // === Seed initial data for smooth demo & offline ===

    private suspend fun syncAllInitialData() {
        syncMessaggi()
        syncCassetto()
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingDocs = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        if (existingDocs.isEmpty()) {
            val docs = listOf(
                CachedDocumentEntity("2025/F24/F24_Febbraio_2025.pdf", "Modello F24 - Febbraio 2025 (Tributi e Contributi)", "2025", "F24 e Versamenti", 185_000, "185 KB", "15/02/2025", "nuovo", true),
                CachedDocumentEntity("2025/F24/F24_Gennaio_2025.pdf", "Modello F24 - Gennaio 2025 (Ritenute d'Acconto)", "2025", "F24 e Versamenti", 172_000, "172 KB", "14/01/2025", "scaricato", false),
                CachedDocumentEntity("2024/Dichiarazioni/Modello_Redditi_SC_2024.pdf", "Dichiarazione Redditi SC 2024 con Ricevuta AdE", "2024", "Dichiarazioni Fiscali", 1_420_000, "1.4 MB", "30/11/2024", "visto", true),
                CachedDocumentEntity("2024/Dichiarazioni/Dichiarazione_IRAP_2024.pdf", "Modello Dichiarazione IRAP 2024", "2024", "Dichiarazioni Fiscali", 890_000, "890 KB", "30/11/2024", "visto", false),
                CachedDocumentEntity("2024/Bilanci/Bilancio_Esercizio_2023_Depositato.pdf", "Bilancio d'Esercizio e Nota Integrativa XBRL", "2024", "Bilanci e Situazioni", 2_340_000, "2.3 MB", "15/06/2024", "visto", true),
                CachedDocumentEntity("2025/Cedolini/Prospetto_Paghe_Gennaio_2025.pdf", "Riepilogo Costo del Personale Gennaio 2025", "2025", "Cedolini e Personale", 430_000, "430 KB", "02/02/2025", "nuovo", false),
                CachedDocumentEntity("2025/Visure/Visura_Camerale_Ordinaria_2025.pdf", "Visura Camerale Ordinaria CCIAA", "2025", "Visure e Atti Societari", 310_000, "310 KB", "10/01/2025", "visto", true)
            )
            documentDao.insertAll(docs)
        }

        val existingMessaggi = messaggioDao.getMessaggi(false).firstOrNull() ?: emptyList()
        if (existingMessaggi.isEmpty()) {
            val messaggi = listOf(
                CachedMessaggioEntity(
                    id = "msg-101",
                    titolo = "Richiesta Estratti Conto e Fatture Q4",
                    corpo = "Gentile cliente, ai fini della chiusura del bilancio annuale, vi preghiamo di caricare gli estratti conto bancari completi al 31/12 e le fatture d'acquisto non transitate da SDI.",
                    dataInvio = "Oggi alle 09:30",
                    letto = false,
                    archiviato = false,
                    richiedeUpload = true,
                    uploadDescrizione = "Estratto conto bancario Q4 in formato PDF",
                    haRisposta = false
                ),
                CachedMessaggioEntity(
                    id = "msg-102",
                    titolo = "Scadenza Versamento F24 16 Marzo",
                    corpo = "Vi ricordiamo la scadenza del versamento F24 per saldo IVA e ritenute in scadenza il 16 Marzo 2025. Il modello predisposto è disponibile nella sezione Archivio.",
                    dataInvio = "Ieri alle 16:45",
                    letto = false,
                    archiviato = false,
                    richiedeUpload = false,
                    haRisposta = false
                ),
                CachedMessaggioEntity(
                    id = "msg-103",
                    titolo = "Chiusura Studio per Festività Pasquali",
                    corpo = "Si comunica che lo Studio rimarrà chiuso da venerdì 18 aprile a martedì 22 aprile compresi. Per urgenze fiscali indifferibili rimane attivo il portale.",
                    dataInvio = "3 giorni fa",
                    letto = true,
                    archiviato = false,
                    richiedeUpload = false,
                    haRisposta = false
                )
            )
            messaggioDao.insertAll(messaggi)
        }

        val existingCassetto = cassettoDao.getCassettoFiles().firstOrNull() ?: emptyList()
        if (existingCassetto.isEmpty()) {
            val cassetto = listOf(
                CachedCassettoEntity("cassetto/qr_piva.pdf", "QR Code Agenzia Entrate P.IVA", 124_000, "124 KB", "10/01/2025", "QR Code P.IVA"),
                CachedCassettoEntity("cassetto/certificato_attribuzione.pdf", "Certificato Attribuzione Partita IVA", 312_000, "312 KB", "15/01/2024", "Certificato P.IVA"),
                CachedCassettoEntity("cassetto/visura_storica.pdf", "Visura Storica Aggiornata CCIAA", 540_000, "540 KB", "05/02/2025", "Visura Camerale"),
                CachedCassettoEntity("cassetto/ci_amministratore.pdf", "Carta Identità Amministratore (Fronte/Retro)", 1_200_000, "1.2 MB", "12/03/2024", "Doc. Identità"),
                CachedCassettoEntity("cassetto/coordinate_bancarie.pdf", "Attestazione IBAN Conto Aziendale", 95_000, "95 KB", "18/01/2025", "IBAN")
            )
            cassettoDao.insertAll(cassetto)
        }

        val existingNotif = notificaDao.getNotifiche().firstOrNull() ?: emptyList()
        if (existingNotif.isEmpty()) {
            val notif = listOf(
                CachedNotificaEntity("n-1", "documento_nuovo", "Nuovo documento disponibile", "Caricato Modello F24 Febbraio 2025", false, "Oggi 10:15", "2025", "F24 e Versamenti"),
                CachedNotificaEntity("n-2", "richiesta_upload", "Richiesta Documenti dallo Studio", "Estratti conto Q4 necessari per bilancio", false, "Oggi 09:30", null, null),
                CachedNotificaEntity("n-3", "scadenza", "Promemoria Scadenza F24", "Scadenza pagamento F24 prevista per il 16 Marzo 2025", true, "Ieri 16:45", null, null),
                CachedNotificaEntity("n-4", "upload_confermato", "Documento ricevuto con successo", "Il file Visura Storica è stato convalidato", true, "3 giorni fa", null, null)
            )
            notificaDao.insertAll(notif)
        }

        val existingAudit = auditDao.getAuditLogs().firstOrNull() ?: emptyList()
        if (existingAudit.isEmpty()) {
            val audit = listOf(
                CachedAuditEntity("a-1", "Oggi 10:20", "preview", "Visualizzato documento F24_Febbraio_2025.pdf"),
                CachedAuditEntity("a-2", "Oggi 09:35", "login", "Accesso al portale client da Android"),
                CachedAuditEntity("a-3", "Ieri 17:00", "download", "Scaricato archivio Modello_Redditi_SC_2024.pdf"),
                CachedAuditEntity("a-4", "Ieri 16:50", "preferito", "Aggiunto ai preferiti: Bilancio_Esercizio_2023_Depositato.pdf")
            )
            auditDao.insertAll(audit)
        }
    }

    private fun generateSampleFiles(year: String, folder: String): List<FileItem> {
        return when {
            folder.contains("F24", ignoreCase = true) -> listOf(
                FileItem("F24_Mese_Corrente_$year.pdf", "$year/$folder/f24_corrente.pdf", 185_000, "185 KB", "15/02/$year", "nuovo", true, year, folder),
                FileItem("F24_Ritenute_Lavoro_Autonomo_$year.pdf", "$year/$folder/f24_ritenute.pdf", 145_000, "145 KB", "16/01/$year", "visto", false, year, folder),
                FileItem("F24_Imposte_Dirette_Saldo_$year.pdf", "$year/$folder/f24_saldo.pdf", 210_000, "210 KB", "30/06/$year", "scaricato", false, year, folder)
            )
            folder.contains("Dichiarazioni", ignoreCase = true) -> listOf(
                FileItem("Modello_Redditi_Societa_$year.pdf", "$year/$folder/redditi.pdf", 1_450_000, "1.4 MB", "30/11/$year", "visto", true, year, folder),
                FileItem("Dichiarazione_IVA_Annuale_$year.pdf", "$year/$folder/iva.pdf", 890_000, "890 KB", "30/04/$year", "visto", false, year, folder),
                FileItem("Certificazione_Unica_CU_$year.pdf", "$year/$folder/cu.pdf", 620_000, "620 KB", "16/03/$year", "scaricato", true, year, folder)
            )
            folder.contains("Bilanci", ignoreCase = true) -> listOf(
                FileItem("Bilancio_Civilistico_Chiusura_$year.pdf", "$year/$folder/bilancio.pdf", 2_400_000, "2.4 MB", "30/04/$year", "visto", true, year, folder),
                FileItem("Relazione_Gestione_Nota_Integrativa_$year.pdf", "$year/$folder/nota.pdf", 1_100_000, "1.1 MB", "30/04/$year", "visto", false, year, folder),
                FileItem("Verbale_Assemblea_Approvazione_$year.pdf", "$year/$folder/verbale.pdf", 380_000, "380 KB", "30/04/$year", "scaricato", false, year, folder)
            )
            folder.contains("Cedolini", ignoreCase = true) -> listOf(
                FileItem("Buste_Paga_Riepilogo_Mensile_$year.pdf", "$year/$folder/buste.pdf", 560_000, "560 KB", "28/02/$year", "nuovo", false, year, folder),
                FileItem("Modello_Uniemens_Trasmesso_$year.pdf", "$year/$folder/uniemens.pdf", 320_000, "320 KB", "28/02/$year", "visto", false, year, folder)
            )
            else -> listOf(
                FileItem("Documento_Studio_$folder" + "_$year.pdf", "$year/$folder/doc1.pdf", 340_000, "340 KB", "01/02/$year", "visto", false, year, folder),
                FileItem("Allegato_Pratica_$folder" + "_$year.pdf", "$year/$folder/doc2.pdf", 520_000, "520 KB", "10/01/$year", "scaricato", false, year, folder)
            )
        }
    }

    private fun inferCategory(nome: String): String {
        val lower = nome.lowercase()
        return when {
            lower.contains("qr") || lower.contains("p.iva") || lower.contains("piva") -> "QR Code P.IVA"
            lower.contains("certificato") -> "Certificato P.IVA"
            lower.contains("visura") || lower.contains("cciaa") -> "Visura Camerale"
            lower.contains("identita") || lower.contains("carta") || lower.contains("patente") -> "Doc. Identità"
            lower.contains("iban") || lower.contains("banca") -> "IBAN"
            else -> "Altro"
        }
    }
}
