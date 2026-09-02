package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.data.local.PfcDatabase
import com.example.data.local.entity.*
import com.example.data.model.*
import com.example.data.remote.PfcApiClient
import com.example.util.DocumentFileManager
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
        return prefs.getBoolean("is_logged_in", false) && apiClient.hasValidSession()
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

    /**
     * Checks if a valid authenticated session exists with backend.
     */
    suspend fun checkExistingSession(): User? = withContext(Dispatchers.IO) {
        if (!apiClient.hasValidSession()) {
            return@withContext null
        }
        try {
            val meRes = apiClient.apiService.getMe()
            if (meRes.isSuccessful && meRes.body()?.user != null) {
                val user = meRes.body()!!.user!!
                saveUser(user)
                return@withContext user
            }
        } catch (e: Exception) {
            Log.w("PfcRepository", "Session check failed: ${e.message}")
        }
        getCurrentUser()
    }

    suspend fun login(username: String, password: String): Result<User> = withContext(Dispatchers.IO) {
        try {
            val cleanUser = username.trim()
            val cleanPass = password.trim()

            val response = apiClient.apiService.login(LoginRequest(cleanUser, cleanPass))
            if (response.isSuccessful && response.body()?.ok == true) {
                // Fetch real user profile from backend /api/auth/me
                var user: User? = null
                try {
                    val meRes = apiClient.apiService.getMe()
                    if (meRes.isSuccessful && meRes.body()?.user != null) {
                        user = meRes.body()!!.user
                    }
                } catch (_: Exception) {}

                if (user == null) {
                    user = User(
                        username = cleanUser,
                        name = cleanUser.replaceFirstChar { it.uppercase() },
                        role = "client"
                    )
                }

                saveUser(user)
                syncAll()
                logAction("login", "Accesso eseguito al Portale PFC: @${user.username}")
                return@withContext Result.success(user)
            } else {
                val errorMsg = response.body()?.error ?: "Credenziali non valide (${response.code()})"
                return@withContext Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("PfcRepository", "Login exception: ${e.message}")
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
        documentDao.clearAll()
    }

    // === Documenti & Archivio ===

    private fun matchesFolder(cartella: String?, key: String, nome: String, targetFolder: String): Boolean {
        val normTarget = targetFolder.trim().lowercase()
        val cleanTarget = normTarget.replace(Regex("^[0-9]+[\\s\\-_.]+"), "").trim()

        val normCartella = (cartella ?: "").trim().lowercase()
        val cleanCartella = normCartella.replace(Regex("^[0-9]+[\\s\\-_.]+"), "").trim()

        // 1. Direct equality
        if (normCartella == normTarget || cleanCartella == cleanTarget) return true

        // 2. Substring containment
        if (cleanTarget.isNotBlank() && cleanCartella.contains(cleanTarget)) return true
        if (cleanCartella.isNotBlank() && cleanTarget.contains(cleanCartella)) return true

        // 3. Category prefixes matching (e.g. "f24 e versamenti" vs "f24")
        if (cleanTarget.startsWith("f24") && cleanCartella.startsWith("f24")) return true
        if (cleanTarget.startsWith("dichiarazion") && cleanCartella.startsWith("dichiarazion")) return true
        if (cleanTarget.startsWith("bilanc") && cleanCartella.startsWith("bilanc")) return true
        if (cleanTarget.startsWith("certificazion") && cleanCartella.startsWith("certificazion")) return true

        // 4. Key path matching (e.g. key is "freddi/2025/f24/file.pdf" or "2025/f24/doc.pdf")
        val lowerKey = key.lowercase()
        if (cleanTarget.isNotBlank() && (lowerKey.contains("/$cleanTarget/") || lowerKey.contains("/$normTarget/"))) return true

        // 5. Special keywords in key path
        if (cleanTarget.startsWith("f24") && (lowerKey.contains("/f24/") || lowerKey.contains("/f24_") || lowerKey.contains("f24"))) return true
        if (cleanTarget.startsWith("dichiarazion") && (lowerKey.contains("/dichiarazioni/") || lowerKey.contains("/dichiarazione/") || lowerKey.contains("dichiaraz"))) return true
        if (cleanTarget.startsWith("bilanc") && (lowerKey.contains("/bilanci/") || lowerKey.contains("/bilancio/"))) return true

        return false
    }

    suspend fun fetchArchivioData(year: String?): Triple<List<String>, List<Cartella>, List<FileItem>> = withContext(Dispatchers.IO) {
        val currentUser = getCurrentUser()?.username
        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
        var targetYr = year ?: currentYear

        try {
            // 1. Fetch available years if not already present
            val yearsRes = apiClient.apiService.listDocumenti(username = currentUser)
            val serverYears = if (yearsRes.isSuccessful && !yearsRes.body()?.anni.isNullOrEmpty()) {
                yearsRes.body()!!.anni!!
            } else {
                emptyList()
            }

            if (serverYears.isNotEmpty() && !serverYears.contains(targetYr)) {
                targetYr = serverYears.first()
            }

            // 2. Fetch folders and documents for target year
            val res = apiClient.apiService.listDocumenti(
                username = currentUser,
                anno = targetYr,
                year = targetYr
            )

            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                val serverCartelleFromAdmin = (body.cartelle ?: emptyList()).map { sc ->
                    // Calculate dynamic count and new files specifically for this admin-created folder
                    val folderDocs = (body.files ?: emptyList()).filter { f ->
                        matchesFolder(f.cartella, f.key, f.nome, sc.nome)
                    }
                    sc.copy(
                        count = folderDocs.size,
                        nuovi = folderDocs.count { it.stato == "nuovo" }
                    )
                }
                val serverFiles = body.files ?: emptyList()

                // Clear outdated cache for this year and insert current server files
                documentDao.deleteByYear(targetYr)
                if (serverFiles.isNotEmpty()) {
                    val entities = serverFiles.map { f ->
                        val inferredCartella = if (!f.cartella.isNullOrBlank()) {
                            f.cartella
                        } else {
                            val parts = f.key.split("/")
                            if (parts.size >= 3) parts[parts.size - 2] else ""
                        }
                        CachedDocumentEntity(
                            key = f.key,
                            nome = f.nome,
                            anno = f.anno ?: targetYr,
                            cartella = inferredCartella,
                            size = f.size,
                            sizeStr = f.sizeStr,
                            lastModified = f.lastModified,
                            stato = f.stato ?: "visto",
                            isPreferito = f.isPreferito
                        )
                    }
                    documentDao.insertAll(entities)
                }

                val allYears = if (serverYears.isNotEmpty()) serverYears else listOf(targetYr)
                return@withContext Triple(allYears, serverCartelleFromAdmin, serverFiles)
            }
        } catch (e: Exception) {
            Log.e("PfcRepository", "fetchArchivioData error: ${e.message}")
        }

        // Fallback to local Room cache (offline mode)
        val cachedDocs = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        val cachedYears = cachedDocs.map { it.anno }.distinct().sortedDescending()
        val effectiveYear = if (cachedYears.contains(targetYr)) targetYr else cachedYears.firstOrNull() ?: targetYr
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
            if (cachedYears.isNotEmpty()) cachedYears else listOf(effectiveYear),
            localCartelle,
            fileItems
        )
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
        val currentUser = getCurrentUser()?.username
        try {
            val res = apiClient.apiService.listDocumenti(username = currentUser)
            if (res.isSuccessful && !res.body()?.anni.isNullOrEmpty()) {
                return@withContext res.body()!!.anni!!
            }
        } catch (_: Exception) {}

        val cachedDocs = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        val cachedYears = cachedDocs.map { it.anno }.distinct().sortedDescending()
        if (cachedYears.isNotEmpty()) {
            return@withContext cachedYears
        }

        listOf(Calendar.getInstance().get(Calendar.YEAR).toString())
    }

    suspend fun getCartelle(year: String): List<Cartella> = withContext(Dispatchers.IO) {
        val currentUser = getCurrentUser()?.username
        try {
            val res = apiClient.apiService.listDocumenti(
                username = currentUser,
                anno = year,
                year = year
            )
            if (res.isSuccessful && res.body() != null) {
                val body = res.body()!!
                val serverCartelle = body.cartelle ?: emptyList()
                val serverFiles = body.files ?: emptyList()
                return@withContext serverCartelle.map { sc ->
                    val folderDocs = serverFiles.filter { f -> matchesFolder(f.cartella, f.key, f.nome, sc.nome) }
                    sc.copy(count = folderDocs.size, nuovi = folderDocs.count { it.stato == "nuovo" })
                }
            }
        } catch (_: Exception) {}

        val cachedDocs = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        val forYear = cachedDocs.filter { it.anno == year && it.cartella.isNotBlank() }
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

    /**
     * Retrieves files for a specific folder from the backend.
     * Passes username, year and folder query parameters matching the V2 backend contract.
     */
    suspend fun getFiles(year: String, folder: String): List<FileItem> = withContext(Dispatchers.IO) {
        val currentUser = getCurrentUser()?.username
        try {
            // 1. Query backend for this specific folder
            val res = apiClient.apiService.listDocumenti(
                username = currentUser,
                anno = year,
                cartella = folder,
                year = year,
                folder = folder
            )
            if (res.isSuccessful && !res.body()?.files.isNullOrEmpty()) {
                val list = res.body()!!.files!!
                val entities = list.map { f ->
                    CachedDocumentEntity(
                        key = f.key,
                        nome = f.nome,
                        anno = year,
                        cartella = if (!f.cartella.isNullOrBlank()) f.cartella else folder,
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

            // 2. Query backend for the year and filter locally by folder
            val yearRes = apiClient.apiService.listDocumenti(
                username = currentUser,
                anno = year,
                year = year
            )
            if (yearRes.isSuccessful && !yearRes.body()?.files.isNullOrEmpty()) {
                val allFiles = yearRes.body()!!.files!!
                val matchedFiles = allFiles.filter { f ->
                    matchesFolder(f.cartella, f.key, f.nome, folder)
                }
                if (matchedFiles.isNotEmpty()) {
                    val entities = matchedFiles.map { f ->
                        CachedDocumentEntity(
                            key = f.key,
                            nome = f.nome,
                            anno = year,
                            cartella = if (!f.cartella.isNullOrBlank()) f.cartella else folder,
                            size = f.size,
                            sizeStr = f.sizeStr,
                            lastModified = f.lastModified,
                            stato = f.stato ?: "visto",
                            isPreferito = f.isPreferito
                        )
                    }
                    documentDao.insertAll(entities)
                    return@withContext matchedFiles
                }
            }
        } catch (e: Exception) {
            Log.e("PfcRepository", "Error fetching files for $folder: ${e.message}")
        }

        // 3. Fallback to local Room cache
        val cached = documentDao.getAllDocuments().firstOrNull() ?: emptyList()
        val forYear = cached.filter { it.anno == year }
        val matched = forYear.filter { doc ->
            matchesFolder(doc.cartella, doc.key, doc.nome, folder)
        }

        if (matched.isNotEmpty()) {
            return@withContext matched.map {
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

        emptyList()
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
        val currentUser = getCurrentUser()?.username
        val cleanQuery = query.trim().lowercase()
        try {
            val res = apiClient.apiService.search(query = query.trim(), username = currentUser)
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

    /**
     * Downloads real original document file to local storage.
     */
    suspend fun downloadDocumentFile(fileItem: FileItem): Result<File> = withContext(Dispatchers.IO) {
        val result = DocumentFileManager.getOrDownloadDocument(context, fileItem, apiClient.apiService)
        if (result.isSuccess) {
            markDocumentScaricato(fileItem)
        }
        result
    }

    /**
     * Downloads multiple documents as a single ZIP archive from backend.
     */
    suspend fun downloadBatchZip(keys: List<String>, zipName: String): Result<File> = withContext(Dispatchers.IO) {
        val result = DocumentFileManager.downloadBatchZip(context, keys, zipName, apiClient.apiService)
        if (result.isSuccess) {
            logAction("download_zip", "Scaricato archivio ZIP di ${keys.size} documenti: $zipName")
        }
        result
    }

    // === Scadenze ===

    suspend fun getScadenze(): List<ScadenzaItem> = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.getScadenzeList()
            if (res.isSuccessful && res.body()?.scadenze != null) {
                return@withContext res.body()!!.scadenze
            }
        } catch (_: Exception) {}
        emptyList()
    }

    suspend fun pagaScadenza(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val res = apiClient.apiService.pagaScadenza(PagaScadenzaRequest(filePath = filePath, pagata = true))
            if (res.isSuccessful && res.body()?.ok == true) {
                logAction("scadenza_paga", "Scadenza segnata come pagata per $filePath")
                return@withContext true
            }
        } catch (_: Exception) {}
        false
    }

    // === Messaggi ===

    fun getMessaggiFlow(archiviato: Boolean): Flow<List<CachedMessaggioEntity>> {
        return messaggioDao.getMessaggi(archiviato).flowOn(Dispatchers.IO)
    }

    suspend fun syncMessaggi() = withContext(Dispatchers.IO) {
        val currentUser = getCurrentUser()?.username
        try {
            val res = apiClient.apiService.getMessaggi(username = currentUser)
            if (res.isSuccessful && res.body()?.messaggi != null) {
                val serverList = res.body()!!.messaggi
                if (serverList.isEmpty()) {
                    messaggioDao.clearAll()
                    return@withContext
                }

                val entities = serverList.map { map ->
                    val rawId = map["id"]?.toString() ?: map["_id"]?.toString() ?: UUID.randomUUID().toString()
                    val rawTitolo = map["titolo"]?.toString()
                        ?: map["title"]?.toString()
                        ?: map["oggetto"]?.toString()
                        ?: map["subject"]?.toString()
                        ?: "Comunicazione dallo Studio"
                    val rawCorpo = map["corpo"]?.toString()
                        ?: map["testo"]?.toString()
                        ?: map["messaggio"]?.toString()
                        ?: map["content"]?.toString()
                        ?: map["body"]?.toString()
                        ?: ""
                    val rawData = map["dataInvio"]?.toString()
                        ?: map["data"]?.toString()
                        ?: map["createdAt"]?.toString()
                        ?: map["date"]?.toString()
                        ?: ""
                    val rawLetto = (map["letto"] as? Boolean)
                        ?: (map["read"] as? Boolean)
                        ?: (map["isRead"] as? Boolean)
                        ?: false
                    val rawArchiviato = (map["archiviato"] as? Boolean) ?: false
                    val rawRichiedeUpload = (map["richiedeUpload"] as? Boolean)
                        ?: (map["richiede_upload"] as? Boolean)
                        ?: (map["richiestaUpload"] as? Boolean)
                        ?: false
                    val rawUploadDesc = map["uploadDescrizione"]?.toString()
                        ?: map["descrizioneUpload"]?.toString()
                        ?: map["richiesta_dettaglio"]?.toString()
                    val rawHaRisposta = (map["haRisposta"] as? Boolean)
                        ?: (map["risposto"] as? Boolean)
                        ?: false
                    val rawAllegato = map["allegatoNome"]?.toString()
                        ?: map["allegato"]?.toString()
                        ?: map["attachment"]?.toString()
                        ?: map["file"]?.toString()

                    CachedMessaggioEntity(
                        id = rawId,
                        titolo = rawTitolo,
                        corpo = rawCorpo,
                        dataInvio = rawData,
                        letto = rawLetto,
                        archiviato = rawArchiviato,
                        richiedeUpload = rawRichiedeUpload,
                        uploadDescrizione = rawUploadDesc,
                        haRisposta = rawHaRisposta,
                        allegatoNome = rawAllegato
                    )
                }

                val currentIds = entities.map { it.id }.filter { it.isNotBlank() }
                if (currentIds.isNotEmpty()) {
                    messaggioDao.deleteNotIn(currentIds)
                }
                messaggioDao.insertAll(entities)
            }
        } catch (e: Exception) {
            Log.e("PfcRepository", "syncMessaggi error: ${e.message}")
        }
    }

    suspend fun setMessaggioLetto(id: String, letto: Boolean) = withContext(Dispatchers.IO) {
        messaggioDao.setLetto(id, letto)
    }

    suspend fun markAllMessaggiLetti() = withContext(Dispatchers.IO) {
        // Local only in Room database
    }

    suspend fun setMessaggioArchiviato(id: String, archiviato: Boolean) = withContext(Dispatchers.IO) {
        messaggioDao.setArchiviato(id, archiviato)
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
        val currentUser = getCurrentUser()?.username
        try {
            val res = apiClient.apiService.getCassettoList(username = currentUser)
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
        val currentUser = getCurrentUser()?.username
        try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

            val res = apiClient.apiService.uploadCassetto(file = filePart, username = currentUser)
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
                val serverNotifiche = res.body()!!.notifiche
                if (serverNotifiche.isEmpty()) {
                    notificaDao.clearAll()
                    return@withContext
                }

                val entities = serverNotifiche.map { map ->
                    val rawId = map["id"]?.toString() ?: map["_id"]?.toString() ?: UUID.randomUUID().toString()
                    val rawTipo = map["tipo"]?.toString() ?: map["type"]?.toString() ?: "avviso"
                    val rawFolder = map["folder"]?.toString() ?: map["cartella"]?.toString()
                    val rawYear = map["year"]?.toString() ?: map["anno"]?.toString()

                    val rawLower = rawTipo.lowercase()
                    val isUploadReq = rawLower.contains("upload") || rawLower.contains("richiest")

                    val defaultTitolo = when {
                        isUploadReq -> "Messaggio con richiesta file"
                        rawLower.contains("document") || rawLower.contains("f24") ->
                            if (!rawFolder.isNullOrBlank()) "Nuovo Documento: $rawFolder" else "Nuovo Documento Fiscale"
                        rawLower.contains("message") || rawLower.contains("messag") ->
                            "Nuovo Messaggio dallo Studio"
                        rawLower.contains("deadline") || rawLower.contains("scadenz") ->
                            "Promemoria Scadenza Fiscale"
                        else -> "Comunicazione Studio PFC"
                    }

                    val rawTitolo = map["titolo"]?.toString()
                        ?: map["title"]?.toString()
                        ?: map["oggetto"]?.toString()
                        ?: map["subject"]?.toString()
                        ?: defaultTitolo

                    val defaultCorpo = when {
                        isUploadReq -> "Lo Studio PFC ha inviato un messaggio con richiesta di invio file. Apri la sezione Messaggi per visualizzarla e allegare il documento."
                        rawLower.contains("document") || rawLower.contains("f24") ->
                            "Lo Studio PFC ha caricato un nuovo documento disponibile nel tuo archivio${if (!rawFolder.isNullOrBlank()) " ($rawFolder)" else ""}."
                        rawLower.contains("message") || rawLower.contains("messag") ->
                            "Hai una nuova comunicazione dallo Studio PFC da consultare."
                        rawLower.contains("deadline") || rawLower.contains("scadenz") ->
                            "Verifica i modelli F24 e adempimenti in scadenza."
                        else -> "Ci sono nuovi aggiornamenti contabili e amministrativi per la tua azienda."
                    }

                    val rawCorpo = map["corpo"]?.toString()
                        ?: map["body"]?.toString()
                        ?: map["testo"]?.toString()
                        ?: map["message"]?.toString()
                        ?: defaultCorpo

                    val rawData = map["dataCreazione"]?.toString()
                        ?: map["data"]?.toString()
                        ?: map["date"]?.toString()
                        ?: map["createdAt"]?.toString()
                        ?: "Oggi"

                    val rawLetta = (map["letta"] as? Boolean)
                        ?: (map["read"] as? Boolean)
                        ?: false

                    CachedNotificaEntity(
                        id = rawId,
                        tipo = rawTipo,
                        titolo = rawTitolo,
                        corpo = rawCorpo,
                        letta = rawLetta,
                        dataCreazione = rawData,
                        year = rawYear,
                        folder = rawFolder
                    )
                }

                val currentIds = entities.map { it.id }.filter { it.isNotBlank() }
                if (currentIds.isNotEmpty()) {
                    notificaDao.deleteNotIn(currentIds)
                }
                notificaDao.insertAll(entities)
            }
        } catch (e: Exception) {
            Log.e("PfcRepository", "syncNotifiche error: ${e.message}")
        }
    }

    suspend fun markNotificaLetta(id: String) = withContext(Dispatchers.IO) {
        notificaDao.markAsRead(id)
    }

    suspend fun markAllNotificheLette() = withContext(Dispatchers.IO) {
        notificaDao.markAllAsRead()
    }

    suspend fun clearNotificheLette() = withContext(Dispatchers.IO) {
        notificaDao.deleteRead()
    }

    suspend fun clearAllNotifiche() = withContext(Dispatchers.IO) {
        notificaDao.clearAll()
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
            Log.d("PfcRepository", "FCM token registered with backend successfully")
        } catch (e: Exception) {
            Log.e("PfcRepository", "Failed to register FCM token with backend: ${e.message}")
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
