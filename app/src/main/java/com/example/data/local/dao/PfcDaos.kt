package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM cached_documents ORDER BY lastModified DESC")
    fun getAllDocuments(): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE anno = :anno AND cartella = :cartella")
    fun getDocumentsByFolder(anno: String, cartella: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE anno = :anno ORDER BY lastModified DESC")
    fun getDocumentsByYear(anno: String): Flow<List<CachedDocumentEntity>>

    @Query("SELECT * FROM cached_documents WHERE isPreferito = 1")
    fun getPreferiti(): Flow<List<CachedDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<CachedDocumentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: CachedDocumentEntity)

    @Query("UPDATE cached_documents SET isPreferito = :isPreferito WHERE `key` = :key")
    suspend fun updatePreferito(key: String, isPreferito: Boolean)

    @Query("UPDATE cached_documents SET stato = :stato WHERE `key` = :key")
    suspend fun updateStato(key: String, stato: String)

    @Query("DELETE FROM cached_documents")
    suspend fun clearAll()
}

@Dao
interface MessaggioDao {
    @Query("SELECT * FROM cached_messaggi WHERE archiviato = :archiviato ORDER BY dataInvio DESC")
    fun getMessaggi(archiviato: Boolean): Flow<List<CachedMessaggioEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messaggi: List<CachedMessaggioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messaggio: CachedMessaggioEntity)

    @Query("UPDATE cached_messaggi SET letto = :letto WHERE id = :id")
    suspend fun setLetto(id: String, letto: Boolean)

    @Query("UPDATE cached_messaggi SET archiviato = :archiviato WHERE id = :id")
    suspend fun setArchiviato(id: String, archiviato: Boolean)

    @Query("UPDATE cached_messaggi SET haRisposta = 1 WHERE id = :id")
    suspend fun setRisposto(id: String)

    @Query("DELETE FROM cached_messaggi")
    suspend fun clearAll()
}

@Dao
interface CassettoDao {
    @Query("SELECT * FROM cached_cassetto ORDER BY lastModified DESC")
    fun getCassettoFiles(): Flow<List<CachedCassettoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<CachedCassettoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: CachedCassettoEntity)

    @Query("DELETE FROM cached_cassetto WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("UPDATE cached_cassetto SET nome = :newName WHERE `key` = :key")
    suspend fun rename(key: String, newName: String)

    @Query("DELETE FROM cached_cassetto")
    suspend fun clearAll()
}

@Dao
interface NotificaDao {
    @Query("SELECT * FROM cached_notifiche ORDER BY dataCreazione DESC")
    fun getNotifiche(): Flow<List<CachedNotificaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notifiche: List<CachedNotificaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(notifica: CachedNotificaEntity)

    @Query("UPDATE cached_notifiche SET letta = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE cached_notifiche SET letta = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM cached_notifiche WHERE letta = 1")
    suspend fun deleteRead()

    @Query("DELETE FROM cached_notifiche")
    suspend fun clearAll()
}

@Dao
interface AuditDao {
    @Query("SELECT * FROM cached_audit ORDER BY ts DESC")
    fun getAuditLogs(): Flow<List<CachedAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CachedAuditEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: CachedAuditEntity)

    @Query("DELETE FROM cached_audit")
    suspend fun clearAll()
}
