package com.example.notification

import android.util.Log
import com.example.data.local.PfcDatabase
import com.example.data.local.entity.CachedDocumentEntity
import com.example.data.local.entity.CachedMessaggioEntity
import com.example.data.local.entity.CachedNotificaEntity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PfcFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notification = remoteMessage.notification

        val title = notification?.title ?: data["title"] ?: data["titolo"] ?: "Portale PFC"
        val body = notification?.body ?: data["body"] ?: data["corpo"] ?: data["message"] ?: "Nuovo aggiornamento disponibile dallo Studio PFC"
        val type = data["type"] ?: data["tipo"] ?: "general"
        val year = data["year"] ?: data["anno"] ?: "2025"
        val folder = data["folder"] ?: data["cartella"] ?: "Dichiarazioni"
        val docKey = data["docKey"] ?: data["key"] ?: data["doc_key"] ?: "$year/$folder/${System.currentTimeMillis()}.pdf"
        val msgId = data["msgId"] ?: data["id"] ?: data["msg_id"] ?: "msg-${System.currentTimeMillis()}"

        // Save incoming event into local Room database for offline persistence
        saveNotificationToDb(type, title, body, year, folder, docKey, msgId)

        // Trigger system notification banner with deep links
        when (type.lowercase()) {
            "document", "documento", "documento_nuovo", "f24", "bilancio" -> {
                LocalNotificationHelper.showNewDocumentNotification(
                    context = applicationContext,
                    docTitle = data["docName"] ?: data["nome"] ?: title,
                    folderName = folder,
                    year = year,
                    docKey = docKey
                )
            }
            "message", "messaggio", "messaggio_nuovo", "comunicazione" -> {
                val requiresUpload = data["requiresUpload"]?.toBoolean() ?: (data["richiedeUpload"]?.toBoolean() ?: false)
                LocalNotificationHelper.showNewMessageNotification(
                    context = applicationContext,
                    messageId = msgId,
                    title = title,
                    corpo = body,
                    requiresUpload = requiresUpload
                )
            }
            "deadline", "scadenza", "scadenza_fiscale" -> {
                val scadenzaDate = data["date"] ?: data["scadenza"] ?: "16 del mese"
                LocalNotificationHelper.showDeadlineReminderNotification(
                    context = applicationContext,
                    deadlineTitle = title,
                    scadenzaDate = scadenzaDate,
                    detail = body
                )
            }
            else -> {
                // Fallback for general notification
                LocalNotificationHelper.showNewMessageNotification(
                    context = applicationContext,
                    messageId = msgId,
                    title = title,
                    corpo = body,
                    requiresUpload = false
                )
            }
        }
    }

    private fun saveNotificationToDb(
        type: String,
        title: String,
        body: String,
        year: String,
        folder: String,
        docKey: String,
        msgId: String
    ) {
        serviceScope.launch {
            try {
                val db = PfcDatabase.getInstance(applicationContext)
                val notifDao = db.notificaDao()

                val notifEntity = CachedNotificaEntity(
                    id = "push-${System.currentTimeMillis()}",
                    tipo = type,
                    titolo = title,
                    corpo = body,
                    letta = false,
                    dataCreazione = "Adesso (Push)",
                    year = year,
                    folder = folder
                )
                notifDao.insert(notifEntity)

                // If it's a document, ensure cached document exists
                if (type.contains("doc") || type.contains("f24")) {
                    val docDao = db.documentDao()
                    val newDoc = CachedDocumentEntity(
                        key = docKey,
                        nome = title,
                        anno = year,
                        cartella = folder,
                        size = 245000L,
                        sizeStr = "245 KB",
                        lastModified = "Oggi",
                        stato = "nuovo",
                        isPreferito = false
                    )
                    docDao.insert(newDoc)
                }

                // If it's a message, insert into cached messages
                if (type.contains("msg") || type.contains("messagg")) {
                    val msgDao = db.messaggioDao()
                    val newMsg = CachedMessaggioEntity(
                        id = msgId,
                        titolo = title,
                        corpo = body,
                        dataInvio = "Oggi",
                        letto = false,
                        archiviato = false,
                        richiedeUpload = false,
                        uploadDescrizione = null,
                        haRisposta = false,
                        allegatoNome = null
                    )
                    msgDao.insert(newMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error caching push notification into Room", e)
            }
        }
    }

    companion object {
        private const val TAG = "PfcFirebaseMsgService"
    }
}
