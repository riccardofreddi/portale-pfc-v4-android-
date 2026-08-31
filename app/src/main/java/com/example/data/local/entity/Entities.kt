package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_documents")
data class CachedDocumentEntity(
    @PrimaryKey val key: String,
    val nome: String,
    val anno: String,
    val cartella: String,
    val size: Long = 0L,
    val sizeStr: String = "",
    val lastModified: String? = null,
    val stato: String = "visto", // nuovo, visto, scaricato, preferito
    val isPreferito: Boolean = false
)

@Entity(tableName = "cached_messaggi")
data class CachedMessaggioEntity(
    @PrimaryKey val id: String,
    val titolo: String,
    val corpo: String,
    val dataInvio: String,
    val letto: Boolean = false,
    val archiviato: Boolean = false,
    val richiedeUpload: Boolean = false,
    val uploadDescrizione: String? = null,
    val haRisposta: Boolean = false,
    val allegatoNome: String? = null
)

@Entity(tableName = "cached_cassetto")
data class CachedCassettoEntity(
    @PrimaryKey val key: String,
    val nome: String,
    val size: Long = 0L,
    val sizeStr: String = "",
    val lastModified: String? = null,
    val categoria: String = "Altro"
)

@Entity(tableName = "cached_notifiche")
data class CachedNotificaEntity(
    @PrimaryKey val id: String,
    val tipo: String,
    val titolo: String,
    val corpo: String? = null,
    val letta: Boolean = false,
    val dataCreazione: String = "",
    val year: String? = null,
    val folder: String? = null
)

@Entity(tableName = "cached_audit")
data class CachedAuditEntity(
    @PrimaryKey val id: String,
    val ts: String,
    val action: String,
    val detail: String = ""
)
