package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock

object ReminderScheduler {

    const val PREFS_NAME = "pfc_reminder_prefs"
    const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    const val KEY_REMIND_DOCUMENTS = "remind_documents"
    const val KEY_REMIND_MESSAGES = "remind_messages"
    const val KEY_REMIND_DEADLINES = "remind_deadlines"
    const val KEY_REMINDER_INTERVAL_MIN = "reminder_interval_min" // 15, 60, 360, 1440

    const val ACTION_CHECK_REMINDERS = "com.example.ACTION_CHECK_REMINDERS"
    private const val REQUEST_CODE_ALARM = 9001

    fun isRemindersEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, true)
    }

    fun setRemindersEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REMINDERS_ENABLED, enabled).apply()
        if (enabled) {
            scheduleReminders(context)
        } else {
            cancelReminders(context)
        }
    }

    fun isRemindDocumentsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMIND_DOCUMENTS, true)
    }

    fun setRemindDocumentsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REMIND_DOCUMENTS, enabled).apply()
    }

    fun isRemindMessagesEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMIND_MESSAGES, true)
    }

    fun setRemindMessagesEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REMIND_MESSAGES, enabled).apply()
    }

    fun isRemindDeadlinesEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMIND_DEADLINES, true)
    }

    fun setRemindDeadlinesEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_REMIND_DEADLINES, enabled).apply()
    }

    fun getIntervalMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_REMINDER_INTERVAL_MIN, 60)
    }

    fun setIntervalMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_REMINDER_INTERVAL_MIN, minutes).apply()
        if (isRemindersEnabled(context)) {
            scheduleReminders(context)
        }
    }

    fun scheduleReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_CHECK_REMINDERS
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val intervalMin = getIntervalMinutes(context)
        val intervalMillis = intervalMin * 60 * 1000L

        // Inexact repeating alarm to optimize battery life
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + intervalMillis,
            intervalMillis,
            pendingIntent
        )
    }

    fun cancelReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_CHECK_REMINDERS
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun triggerImmediateCheck(context: Context) {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ACTION_CHECK_REMINDERS
        }
        context.sendBroadcast(intent)
    }
}
