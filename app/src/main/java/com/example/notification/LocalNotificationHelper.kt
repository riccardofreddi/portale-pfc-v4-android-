package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

object LocalNotificationHelper {

    const val CHANNEL_DOCUMENTS = "pfc_channel_documents"
    const val CHANNEL_MESSAGES = "pfc_channel_messages"
    const val CHANNEL_DEADLINES = "pfc_channel_deadlines"

    const val EXTRA_TARGET_TAB = "extra_target_tab"
    const val EXTRA_YEAR = "extra_year"
    const val EXTRA_FOLDER = "extra_folder"
    const val EXTRA_DOC_KEY = "extra_doc_key"
    const val EXTRA_MSG_ID = "extra_msg_id"
    const val EXTRA_SHOW_NOTIF_SHEET = "extra_show_notif_sheet"

    private const val NOTIF_COLOR = 0xFF6750A4.toInt()

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return

            // Channel: Fiscal Documents & F24
            val docChannel = NotificationChannel(
                CHANNEL_DOCUMENTS,
                "Documenti Fiscali & F24",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avvisi per nuovi modelli F24, bilanci e documenti caricati dallo Studio PFC"
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel: Messages & Requests from Studio
            val msgChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messaggi & Richieste Studio",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Comunicazioni dirette dallo Studio e richieste di invio documenti aziendali"
                enableLights(true)
                lightColor = Color.MAGENTA
                enableVibration(true)
                setShowBadge(true)
            }

            // Channel: Fiscal Deadlines & Reminders
            val deadlineChannel = NotificationChannel(
                CHANNEL_DEADLINES,
                "Scadenze Fiscali & Promemoria",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Promemoria automatici per versamenti F24, scadenze tributarie e adempimenti"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(docChannel, msgChannel, deadlineChannel))
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Show notification for a new fiscal document (e.g. Modello F24 or Bilancio)
     */
    fun showNewDocumentNotification(
        context: Context,
        docTitle: String,
        folderName: String,
        year: String,
        docKey: String,
        notificationId: Int = 1001 + (docKey.hashCode() % 10000)
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_TAB, 0)
            putExtra(EXTRA_YEAR, year)
            putExtra(EXTRA_FOLDER, folderName)
            putExtra(EXTRA_DOC_KEY, docKey)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DOCUMENTS)
            .setSmallIcon(R.drawable.ic_stat_document)
            .setContentTitle("Nuovo Documento: $folderName")
            .setContentText(docTitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$docTitle\n\nDisponibile nella cartella '$folderName' ($year). Tocca per consultare o scaricare.")
                    .setSummaryText("Studio PFC • Archivio Fiscale")
            )
            .setSubText("Studio PFC")
            .setColor(NOTIF_COLOR)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stat_document,
                "Apri Archivio",
                pendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted on Android 13+
        }
    }

    /**
     * Show notification for a new message or document request from Studio
     */
    fun showNewMessageNotification(
        context: Context,
        messageId: String,
        title: String,
        corpo: String,
        requiresUpload: Boolean = false,
        notificationId: Int = 2001 + (messageId.hashCode() % 10000)
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_TAB, 1)
            putExtra(EXTRA_MSG_ID, messageId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val actionText = if (requiresUpload) "Carica File Richiesto" else "Leggi Messaggio"

        val builder = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_stat_message)
            .setContentTitle(if (requiresUpload) "Richiesta Documenti dallo Studio" else "Nuovo Messaggio dallo Studio")
            .setContentText(title)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$title\n\n$corpo")
                    .setSummaryText("Studio PFC • Comunicazioni")
            )
            .setSubText("Studio PFC")
            .setColor(NOTIF_COLOR)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stat_message,
                actionText,
                pendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }

    /**
     * Show notification for upcoming fiscal deadline
     */
    fun showDeadlineReminderNotification(
        context: Context,
        deadlineTitle: String,
        scadenzaDate: String,
        detail: String,
        notificationId: Int = 3001
    ) {
        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TARGET_TAB, 0)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_DEADLINES)
            .setSmallIcon(R.drawable.ic_stat_document)
            .setContentTitle("Promemoria Scadenza: $scadenzaDate")
            .setContentText(deadlineTitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$deadlineTitle\n$detail\n\nScadenza fissata per il: $scadenzaDate. Accedi all'Archivio per verificare il modello F24 predisposto.")
                    .setSummaryText("Studio PFC • Scadenze")
            )
            .setSubText("Scadenza Fiscale")
            .setColor(0xFFBA1A1A.toInt()) // Red accent
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stat_document,
                "Verifica F24",
                pendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    /**
     * Show general summary notification
     */
    fun showSummaryNotification(
        context: Context,
        newDocsCount: Int,
        unreadMsgsCount: Int,
        notificationId: Int = 4001
    ) {
        if (newDocsCount == 0 && unreadMsgsCount == 0) return

        createNotificationChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SHOW_NOTIF_SHEET, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val msgSummary = buildString {
            if (newDocsCount > 0) append("$newDocsCount nuovi documenti fiscali ")
            if (newDocsCount > 0 && unreadMsgsCount > 0) append("e ")
            if (unreadMsgsCount > 0) append("$unreadMsgsCount messaggi non letti dallo Studio")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_DOCUMENTS)
            .setSmallIcon(R.drawable.ic_stat_pfc)
            .setContentTitle("Portale PFC: Aggiornamenti Disponibili")
            .setContentText(msgSummary)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Sono presenti aggiornamenti nel tuo Portale PFC:\n• $msgSummary\n\nTocca per aprire le notifiche.")
            )
            .setColor(NOTIF_COLOR)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    fun cancelAll(context: Context) {
        NotificationManagerCompat.from(context).cancelAll()
    }
}
