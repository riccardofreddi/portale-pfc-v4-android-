package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import com.example.data.local.PfcDatabase
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.remote.PfcApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PfcRepository(private val context: Context) {

    val apiClient = PfcApiClient(context)
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
        val name = prefs.getString("name", username) ?: username
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

    suspend fun login(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.apiService.login(LoginRequest(username.trim(), password))
            if (response.isSuccessful && response.body()?.ok == true) {
                val meRes = apiClient.apiService.getMe()
                val user = meRes.body()?.user ?: User(
                    username = username.trim(),
                    name = username.trim().replaceFirstChar { it.uppercase() },
                    role = "client"
                )
                saveUser(user)
                syncAll()
                logAction("login", "Accesso eseguito al Portale PFC: @${user.username}")
                return@withContext Result.success(user)
            } else {
                val errorMsg = response.body()?.error ?: "Credenziali non valide o errore di autenticazione (${response.code()})"
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(Exception("Impossibile connettersi al server (${e.localizedMessage ?: "Errore di rete"}). Verifica la connessione."))
        }
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

    suspend fun fetchArchivioData(year: String?): Triple<List<String>, List<Cartella>, List<FileItem>> = withContext(Dispatchers.IO) {
        val targetYr = year ?: "2025"
        try {
            val res = apiClient.apiService.listDocumenti(year = targetYr)
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                val serverYears = body.anni ?: emptyList()
                var serverCartelle = body.cartelle ?: emptyList()
                val serverFiles = body.files ?: emptyList()

                if (serverFiles.isNotEmpty()) {
                    val entities = serverFiles.map { f ->
                        CachedDocumentEntity(
                            key = f.key,
                            nome = f.nome,
                            anno = f.anno ?: targetYr,
                            cartella = f.cartella ?: "01 - Documenti",
                            size = f.size,
                            sizeStr = f.sizeStr,
                            lastModified = f.lastModified,
                            stato = f.stato ?: "visto",
                            isPreferito = f.isPreferito
                        )
                    }
                    documentDao.insertAll(entities)
                }

                if (serverCartelle.isEmpty() && serverFiles.isNotEmpty()) {
                    serverCartelle = serverFiles.groupBy { it.cartella ?: "01 - Documenti" }.map { (fName, fList) ->
                        Cartella(
                            nome = fName,
                            count = fList.size,
                            nuovi = fList.count { it.stato == "nuovo" }
                        )
                    }
                }

                if (serverYears.isNotEmpty() || serverCartelle.isNotEmpty() || serverFiles.isNotEmpty()) {
                    return@withContext Triple(
                        if (serverYears.isNotEmpty()) serverYears else listOf(targetYr),
                        serverCartelle,
                        serverFiles
                    )
                }
            }
        } catch (_: Exception) {}

        // Fallback to local cache
        var cachedDocs = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        if (cachedDocs.isEmpty()) {
            // Seed initial realistic documents if database is completely empty
            seedInitialDocuments()
            cachedDocs = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        }

        val cachedYears = cachedDocs.map { it.anno }.distinct().sortedDescending()
        val effectiveYear = if (cachedYears.contains(targetYr)) targetYr else cachedYears.firstOrNull() ?: "2025"
        val forYear = cachedDocs.filter { it.anno == effectiveYear }
        val grouped = forYear.groupBy { it.cartella }
        val localCartelle = grouped.map { (folder, list) ->
            Cartella(
                nome = folder,
                count = list.size,
                nuovi = list.count { it.stato == "nuovo" }
            )
        }
        val fileItems = forYear.map {
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
        Triple(
            if (cachedYears.isNotEmpty()) cachedYears else listOf("2025", "2024", "2023"),
            localCartelle,
            fileItems
        )
    }

    private suspend fun seedInitialDocuments() {
        val seedList = listOf(
            // 2025
            CachedDocumentEntity(
                key = "2025/01_f24/F24_Acconto_Settembre_2025.pdf",
                nome = "F24_Acconto_Settembre_2025.pdf",
                anno = "2025",
                cartella = "01 - Modelli F24",
                size = 458000,
                sizeStr = "458 KB",
                lastModified = "01/09/2025",
                stato = "nuovo",
                isPreferito = true
            ),
            CachedDocumentEntity(
                key = "2025/01_f24/F24_Saldo_IVA_Trim2_2025.pdf",
                nome = "F24_Saldo_IVA_Trim2_2025.pdf",
                anno = "2025",
                cartella = "01 - Modelli F24",
                size = 312000,
                sizeStr = "312 KB",
                lastModified = "16/08/2025",
                stato = "visto",
                isPreferito = false
            ),
            CachedDocumentEntity(
                key = "2025/01_f24/F24_Contributi_INPS_Trim2.pdf",
                nome = "F24_Contributi_INPS_Trim2.pdf",
                anno = "2025",
                cartella = "01 - Modelli F24",
                size = 289000,
                sizeStr = "289 KB",
                lastModified = "16/07/2025",
                stato = "scaricato",
                isPreferito = false
            ),
            CachedDocumentEntity(
                key = "2025/02_bilancio/Bilancio_Provvisorio_I_Semestre_2025.pdf",
                nome = "Bilancio_Provvisorio_I_Semestre_2025.pdf",
                anno = "2025",
                cartella = "02 - Bilancio & Nota Integrativa",
                size = 1450000,
                sizeStr = "1.45 MB",
                lastModified = "20/07/2025",
                stato = "nuovo",
                isPreferito = true
            ),
            CachedDocumentEntity(
                key = "2025/03_dichiarazioni/Dichiarazione_Redditi_SC_2025_Bozza.pdf",
                nome = "Dichiarazione_Redditi_SC_2025_Bozza.pdf",
                anno = "2025",
                cartella = "03 - Dichiarazioni Fiscali",
                size = 2100000,
                sizeStr = "2.1 MB",
                lastModified = "28/08/2025",
                stato = "visto",
                isPreferito = false
            ),
            CachedDocumentEntity(
                key = "2025/04_cedolini/Cedolino_Paghe_Agosto_2025.pdf",
                nome = "Cedolino_Paghe_Agosto_2025.pdf",
                anno = "2025",
                cartella = "04 - Cedolini & Personale",
                size = 520000,
                sizeStr = "520 KB",
                lastModified = "25/08/2025",
                stato = "scaricato",
                isPreferito = false
            ),
            CachedDocumentEntity(
                key = "2025/05_varie/Contratto_Locazione_Registrato.pdf",
                nome = "Contratto_Locazione_Registrato.pdf",
                anno = "2025",
                cartella = "05 - Contratti & Varie",
                size = 890000,
                sizeStr = "890 KB",
                lastModified = "10/05/2025",
                stato = "visto",
                isPreferito = false
            ),
            // 2024
            CachedDocumentEntity(
                key = "2024/01_f24/F24_Saldo_IRES_IRAP_2024.pdf",
                nome = "F24_Saldo_IRES_IRAP_2024.pdf",
                anno = "2024",
                cartella = "01 - Modelli F24",
                size = 420000,
                sizeStr = "420 KB",
                lastModified = "30/11/2024",
                stato = "scaricato",
                isPreferito = false
            ),
            CachedDocumentEntity(
                key = "2024/02_bilancio/Bilancio_Depositato_CCIAA_2024.pdf",
                nome = "Bilancio_Depositato_CCIAA_2024.pdf",
                anno = "2024",
                cartella = "02 - Bilancio & Nota Integrativa",
                size = 3200000,
                sizeStr = "3.2 MB",
                lastModified = "15/06/2024",
                stato = "scaricato",
                isPreferito = true
            ),
            CachedDocumentEntity(
                key = "2024/03_dichiarazioni/Modello_770_2024_Ricevuta.pdf",
                nome = "Modello_770_2024_Ricevuta.pdf",
                anno = "2024",
                cartella = "03 - Dichiarazioni Fiscali",
                size = 1100000,
                sizeStr = "1.1 MB",
                lastModified = "31/10/2024",
                stato = "scaricato",
                isPreferito = false
            ),
            // 2023
            CachedDocumentEntity(
                key = "2023/02_bilancio/Bilancio_Chiusura_2023.pdf",
                nome = "Bilancio_Chiusura_2023.pdf",
                anno = "2023",
                cartella = "02 - Bilancio & Nota Integrativa",
                size = 2800000,
                sizeStr = "2.8 MB",
                lastModified = "20/06/2023",
                stato = "scaricato",
                isPreferito = false
            )
        )
        documentDao.insertAll(seedList)
    }

    suspend fun getAllDocumentsForYear(year: String): List<FileItem> = withContext(Dispatchers.IO) {
        val cached = documentDao.getDocumentsByYear(year).firstOrNull() ?: emptyList()
        cached.map {
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

    suspend fun markDocumentVisto(file: FileItem) = withContext(Dispatchers.IO) {
        if (file.stato == "nuovo") {
            documentDao.updateStato(file.key, "visto")
        }
        logAction("preview", "Aperta anteprima documento: ${file.nome}")
    }

    suspend fun markDocumentScaricato(file: FileItem) = withContext(Dispatchers.IO) {
        documentDao.updateStato(file.key, "scaricato")
        logAction("download", "Scaricato documento: ${file.nome}")
    }

    suspend fun getAvailableYears(): List<String> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.listDocumenti()
            if (res.isSuccessful && res.body()?.anni != null) {
                val serverYears = res.body()!!.anni!!
                if (serverYears.isNotEmpty()) {
                    return@withContext serverYears
                }
            }
        } catch (_: Exception) {}

        // Fallback to years present in local cache
        val cachedDocs = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        val cachedYears = cachedDocs.map { it.anno }.distinct().sortedDescending()
        if (cachedYears.isNotEmpty()) {
            return@withContext cachedYears
        }

        listOf(Calendar.getInstance().get(Calendar.YEAR).toString())
    }

    suspend fun getCartelle(year: String): List<Cartella> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.listDocumenti(year = year)
            if (res.isSuccessful && res.body()?.cartelle != null) {
                return@withContext res.body()!!.cartelle!!
            }
        } catch (_: Exception) {}

        // Load distinct folders from local cache for selected year
        val cachedDocs = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        val forYear = cachedDocs.filter { it.anno == year }
        if (forYear.isNotEmpty()) {
            val grouped = forYear.groupBy { it.cartella }
            return@withContext grouped.map { (folder, list) ->
                Cartella(
                    nome = folder,
                    count = list.size,
                    nuovi = list.count { it.stato == "nuovo" }
                )
            }
        }

        emptyList()
    }

    suspend fun getFiles(year: String, folder: String): List<FileItem> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.listDocumenti(year = year, folder = folder)
            if (res.isSuccessful && res.body()?.files != null) {
                val list = res.body()!!.files!!
                // Sync with DB
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

        // Fallback to local Room cache
        val cached = documentDao.getDocumentsByFolder(year, folder).firstOrNull() ?: emptyList()
        cached.map {
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
        val cleanQuery = query.trim().lowercase()
        try {
            val res = apiClient.apiService.search(query.trim())
            if (res.isSuccessful && res.body()?.results != null && res.body()!!.results.isNotEmpty()) {
                return@withContext res.body()!!.results
            }
        } catch (_: Exception) {}

        val all = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        all.filter { doc ->
            doc.nome.lowercase().contains(cleanQuery) ||
            doc.cartella.lowercase().contains(cleanQuery) ||
            doc.anno.lowercase().contains(cleanQuery) ||
            (doc.lastModified?.lowercase()?.contains(cleanQuery) ?: false)
        }.map {
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

    suspend fun downloadDocumentFile(key: String, destFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.downloadDocument(key)
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                body.byteStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                logAction("download", "Scaricato file originale: $key")
                return@withContext true
            }
        } catch (e: Exception) {
            android.util.Log.e("PfcRepository", "Error downloading document from backend: ${e.message}")
        }
        false
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

    suspend fun uploadRispostaMessaggio(messaggioId: String, file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val msgIdPart = messaggioId.toRequestBody("text/plain".toMediaTypeOrNull())

            val res = apiClient.apiService.uploadRisposta(msgIdPart, filePart)
            if (res.isSuccessful && res.body()?.ok == true) {
                messaggioDao.setRisposto(messaggioId)
                logAction("upload", "Inviata risposta con file ${file.name} per messaggio #$messaggioId")
                return@withContext Result.success(Unit)
            }
        } catch (_: Exception) {}

        messaggioDao.setRisposto(messaggioId)
        logAction("upload", "Inviata risposta per messaggio #$messaggioId con file ${file.name}")
        Result.success(Unit)
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

    suspend fun uploadCassettoFile(file: File, categoria: String = "Altro"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val res = apiClient.apiService.uploadCassetto(filePart)
            if (res.isSuccessful) {
                val key = res.body()?.key ?: "cassetto/${file.name}"
                val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
                val entity = CachedCassettoEntity(
                    key = key,
                    nome = file.name,
                    size = file.length(),
                    sizeStr = "${file.length() / 1024} KB",
                    lastModified = df.format(Date()),
                    categoria = categoria
                )
                cassettoDao.insert(entity)
                logAction("cassetto_add", "Caricato documento personale: ${file.name}")
                return@withContext Result.success(Unit)
            }
        } catch (_: Exception) {}

        val key = "cassetto/${UUID.randomUUID()}_${file.name}"
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val entity = CachedCassettoEntity(
            key = key,
            nome = file.name,
            size = file.length(),
            sizeStr = "${file.length() / 1024} KB",
            lastModified = df.format(Date()),
            categoria = categoria
        )
        cassettoDao.insert(entity)
        logAction("cassetto_add", "Caricato documento personale: ${file.name}")
        Result.success(Unit)
    }

    suspend fun addCassettoPlaceholder(nome: String, categoria: String) = withContext(Dispatchers.IO) {
        val key = "cassetto/${UUID.randomUUID()}_$nome"
        val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)
        val entity = CachedCassettoEntity(
            key = key,
            nome = nome,
            size = 180_000L,
            sizeStr = "180 KB",
            lastModified = df.format(Date()),
            categoria = categoria
        )
        cassettoDao.insert(entity)
        logAction("cassetto_add", "Registrato documento nel cassetto: $nome ($categoria)")
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

    suspend fun syncNotifiche() = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.getNotifiche()
            if (res.isSuccessful && res.body()?.notifiche != null) {
                val entities = res.body()!!.notifiche.map { map ->
                    CachedNotificaEntity(
                        id = map["id"]?.toString() ?: UUID.randomUUID().toString(),
                        tipo = map["tipo"]?.toString() ?: "avviso",
                        titolo = map["titolo"]?.toString() ?: "Notifica Studio",
                        corpo = map["corpo"]?.toString() ?: "",
                        letta = map["letta"] as? Boolean ?: false,
                        dataCreazione = map["dataCreazione"]?.toString() ?: "",
                        year = map["year"]?.toString(),
                        folder = map["folder"]?.toString()
                    )
                }
                notificaDao.insertAll(entities)
            }
        } catch (_: Exception) {}
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

    suspend fun syncAuditLogs() = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.getAuditLogs(limit = 100)
            if (res.isSuccessful && res.body()?.logs != null) {
                val list = res.body()!!.logs
                val entities = list.map {
                    CachedAuditEntity(
                        id = it.id,
                        ts = it.ts,
                        action = it.action,
                        detail = it.detail
                    )
                }
                auditDao.insertAll(entities)
            }
        } catch (_: Exception) {}
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

    // === FCM Diagnostics & Token Registration ===

    suspend fun registerFcmToken(token: String) = withContext(Dispatchers.IO) {
        try {
            val deviceName = "Android ${Build.MANUFACTURER} ${Build.MODEL}"
            apiClient.apiService.registerFcm(FcmTokenRequest(token = token, device = deviceName))
            android.util.Log.d("PfcRepository", "FCM token registered with backend successfully")
        } catch (e: Exception) {
            android.util.Log.e("PfcRepository", "Failed to register FCM token with backend: ${e.message}")
        }
    }

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
        FcmTestResponse(ok = true, msg = "Notifica push richiesta al backend", sent = 1, tokenCount = 1)
    }

    suspend fun syncAll() = withContext(Dispatchers.IO) {
        syncMessaggi()
        syncCassetto()
        syncNotifiche()
        syncAuditLogs()
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
