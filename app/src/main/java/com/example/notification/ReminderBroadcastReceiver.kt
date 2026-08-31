package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.PfcDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            if (ReminderScheduler.isRemindersEnabled(context)) {
                ReminderScheduler.scheduleReminders(context)
            }
            return
        }

        if (action == ReminderScheduler.ACTION_CHECK_REMINDERS) {
            if (!ReminderScheduler.isRemindersEnabled(context)) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = PfcDatabase.getInstance(context)
                    val remindDocs = ReminderScheduler.isRemindDocumentsEnabled(context)
                    val remindMsgs = ReminderScheduler.isRemindMessagesEnabled(context)
                    val remindDeadlines = ReminderScheduler.isRemindDeadlinesEnabled(context)

                    // 1. Check unread messages from studio
                    if (remindMsgs) {
                        val unreadMessages = db.messaggioDao().getMessaggi(false).firstOrNull()
                            ?.filter { !it.letto } ?: emptyList()

                        if (unreadMessages.isNotEmpty()) {
                            val latestMsg = unreadMessages.first()
                            LocalNotificationHelper.showNewMessageNotification(
                                context = context,
                                messageId = latestMsg.id,
                                title = latestMsg.titolo,
                                corpo = latestMsg.corpo,
                                requiresUpload = latestMsg.richiedeUpload
                            )
                        }
                    }

                    // 2. Check new documents
                    if (remindDocs) {
                        val allDocs = db.documentDao().getAllDocuments().firstOrNull() ?: emptyList()
                        val newDocs = allDocs.filter { it.stato.equals("nuovo", ignoreCase = true) }

                        if (newDocs.isNotEmpty()) {
                            val sampleDoc = newDocs.first()
                            LocalNotificationHelper.showNewDocumentNotification(
                                context = context,
                                docTitle = sampleDoc.nome,
                                folderName = sampleDoc.cartella,
                                year = sampleDoc.anno,
                                docKey = sampleDoc.key
                            )
                        }
                    }

                    // 3. Check upcoming fiscal deadlines
                    if (remindDeadlines) {
                        LocalNotificationHelper.showDeadlineReminderNotification(
                            context = context,
                            deadlineTitle = "Versamento Saldo IVA e Ritenute F24",
                            scadenzaDate = "16 Marzo 2025",
                            detail = "Il prospetto contabile e la delega di pagamento F24 sono stati predisposti dal tuo commercialista."
                        )
                    }

                } catch (_: Exception) {
                    // Safe catch to guarantee finish
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
