package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "username") val username: String,
    @Json(name = "name") val name: String,
    @Json(name = "role") val role: String = "client",
    @Json(name = "exemptMaintenance") val exemptMaintenance: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "user") val user: Any? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class MeResponse(
    @Json(name = "user") val user: User? = null
)

// === Documenti / Archivio ===

@JsonClass(generateAdapter = true)
data class FileItem(
    @Json(name = "nome") val nome: String,
    @Json(name = "key") val key: String,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "sizeStr") val sizeStr: String = "",
    @Json(name = "lastModified") val lastModified: String? = null,
    @Json(name = "stato") val stato: String? = "visto", // preferito, nuovo, visto, scaricato
    @Json(name = "isPreferito") val isPreferito: Boolean = false,
    @Json(name = "anno") val anno: String? = null,
    @Json(name = "cartella") val cartella: String? = null
)

@JsonClass(generateAdapter = true)
data class Cartella(
    @Json(name = "nome") val nome: String,
    @Json(name = "count") val count: Int? = 0,
    @Json(name = "nuovi") val nuovi: Int? = 0,
    @Json(name = "hasScadenza") val hasScadenza: Boolean? = false,
    @Json(name = "scadenzaPagata") val scadenzaPagata: Boolean? = false,
    @Json(name = "scadenzaData") val scadenzaData: String? = null
)

@JsonClass(generateAdapter = true)
data class ListResponse(
    @Json(name = "anni") val anni: List<String>? = null,
    @Json(name = "cartelle") val cartelle: List<Cartella>? = null,
    @Json(name = "files") val files: List<FileItem>? = null,
    @Json(name = "r2NotConfigured") val r2NotConfigured: Boolean? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchResult(
    @Json(name = "nome") val nome: String,
    @Json(name = "key") val key: String,
    @Json(name = "anno") val anno: String,
    @Json(name = "cartella") val cartella: String,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "sizeStr") val sizeStr: String = "",
    @Json(name = "score") val score: Double = 0.0
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
    @Json(name = "results") val results: List<SearchResult> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PreferitiResponse(
    @Json(name = "preferiti") val preferiti: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PreferitoToggleRequest(
    @Json(name = "filePath") val filePath: String
)

@JsonClass(generateAdapter = true)
data class PreferitoToggleResponse(
    @Json(name = "ok") val ok: Boolean,
    @Json(name = "isPreferito") val isPreferito: Boolean
)

// === Messaggi ===

@JsonClass(generateAdapter = true)
data class Messaggio(
    @Json(name = "id") val id: String,
    @Json(name = "titolo") val titolo: String = "",
    @Json(name = "corpo") val corpo: String = "",
    @Json(name = "dataInvio") val dataInvio: String = "",
    @Json(name = "letto") val letto: Boolean = false,
    @Json(name = "archiviato") val archiviato: Boolean = false,
    @Json(name = "richiedeUpload") val richiedeUpload: Boolean = false,
    @Json(name = "uploadDescrizione") val uploadDescrizione: String? = null,
    @Json(name = "haRisposta") val haRisposta: Boolean = false,
    @Json(name = "allegatoNome") val allegatoNome: String? = null
)

@JsonClass(generateAdapter = true)
data class MessaggiRawResponse(
    @Json(name = "messaggi") val messaggi: List<Map<String, Any?>> = emptyList()
)

// === Cassetto ===

@JsonClass(generateAdapter = true)
data class CassettoFile(
    @Json(name = "nome") val nome: String,
    @Json(name = "key") val key: String,
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "sizeStr") val sizeStr: String = "",
    @Json(name = "lastModified") val lastModified: String? = null
)

@JsonClass(generateAdapter = true)
data class CassettoListResponse(
    @Json(name = "files") val files: List<CassettoFile> = emptyList()
)

@JsonClass(generateAdapter = true)
data class KeyRequest(
    @Json(name = "key") val key: String
)

@JsonClass(generateAdapter = true)
data class RenameRequest(
    @Json(name = "key") val key: String,
    @Json(name = "newName") val newName: String
)

@JsonClass(generateAdapter = true)
data class GenericOkResponse(
    @Json(name = "ok") val ok: Boolean = true,
    @Json(name = "key") val key: String? = null,
    @Json(name = "nome") val nome: String? = null,
    @Json(name = "newName") val newName: String? = null
)

// === Notifiche ===

@JsonClass(generateAdapter = true)
data class Notifica(
    @Json(name = "id") val id: String,
    @Json(name = "tipo") val tipo: String, // documento_nuovo, messaggio, avviso, richiesta_upload, scadenza, upload_confermato
    @Json(name = "titolo") val titolo: String,
    @Json(name = "corpo") val corpo: String? = null,
    @Json(name = "letta") val letta: Boolean = false,
    @Json(name = "dataCreazione") val dataCreazione: String = "",
    @Json(name = "year") val year: String? = null,
    @Json(name = "folder") val folder: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificheResponse(
    @Json(name = "notifiche") val notifiche: List<Map<String, Any?>> = emptyList()
)

// === Audit ===

@JsonClass(generateAdapter = true)
data class AuditEntry(
    @Json(name = "id") val id: String,
    @Json(name = "ts") val ts: String,
    @Json(name = "action") val action: String,
    @Json(name = "detail") val detail: String = ""
)

@JsonClass(generateAdapter = true)
data class AuditResponse(
    @Json(name = "logs") val logs: List<AuditEntry> = emptyList()
)

// === Push / FCM ===

@JsonClass(generateAdapter = true)
data class FcmStatusResponse(
    @Json(name = "fcmEnabled") val fcmEnabled: Boolean = false,
    @Json(name = "serverProjectId") val serverProjectId: String? = null,
    @Json(name = "userTokens") val userTokens: Int = 0
)

@JsonClass(generateAdapter = true)
data class FcmTestResponse(
    @Json(name = "ok") val ok: Boolean = false,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "sent") val sent: Int? = null,
    @Json(name = "tokenCount") val tokenCount: Int? = null
)

@JsonClass(generateAdapter = true)
data class FcmTokenRequest(
    @Json(name = "token") val token: String,
    @Json(name = "device") val device: String
)
