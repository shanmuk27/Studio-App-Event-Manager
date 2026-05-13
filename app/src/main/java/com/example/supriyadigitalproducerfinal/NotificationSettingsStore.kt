package com.example.supriyadigitalproducerfinal

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// ALERT ENTRY — one log entry in the Alerts page
// ─────────────────────────────────────────────────────────────────

enum class AlertType { SUCCESS, ERROR, INFO, WARNING }

data class AlertEntry(
    val id: Long = System.currentTimeMillis(),
    val type: AlertType = AlertType.INFO,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun formattedTime(): String =
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(timestamp))
}

// ─────────────────────────────────────────────────────────────────
// NOTIFICATION SETTINGS STORE
// Persists all notification-related settings and the alert log
// ─────────────────────────────────────────────────────────────────

class NotificationSettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("NotificationSettings", Context.MODE_PRIVATE)
    private val gson  = Gson()

    companion object {
        const val KEY_SMS_ENABLED       = "sms_enabled"
        const val KEY_PHONE_NUMBER      = "reminder_phone"
        const val KEY_REMINDER_HOUR     = "reminder_hour"
        const val KEY_REMINDER_MINUTE   = "reminder_minute"
        const val KEY_ALERT_LOG         = "alert_log"
        const val MAX_ALERT_LOG_SIZE    = 100

        const val DEFAULT_PHONE         = "9246789966"
        const val DEFAULT_HOUR          = 8    // 8:00 AM
        const val DEFAULT_MINUTE        = 0
    }

    // ── SMS toggle ───────────────────────────────────────────────

    var smsEnabled: Boolean
        get()      = prefs.getBoolean(KEY_SMS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_SMS_ENABLED, value) }

    // ── Phone number ─────────────────────────────────────────────

    var reminderPhone: String
        get()      = prefs.getString(KEY_PHONE_NUMBER, DEFAULT_PHONE) ?: DEFAULT_PHONE
        set(value) = prefs.edit { putString(KEY_PHONE_NUMBER, value) }

    // ── Reminder time ────────────────────────────────────────────

    var reminderHour: Int
        get()      = prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_HOUR)
        set(value) = prefs.edit { putInt(KEY_REMINDER_HOUR, value) }

    var reminderMinute: Int
        get()      = prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_MINUTE)
        set(value) = prefs.edit { putInt(KEY_REMINDER_MINUTE, value) }

    fun formattedReminderTime(): String {
        val h   = reminderHour
        val m   = reminderMinute
        val ampm = if (h < 12) "AM" else "PM"
        val hh  = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(hh, m, ampm)
    }

    // ── Alert log ────────────────────────────────────────────────

    fun loadAlerts(): MutableList<AlertEntry> {
        val json = prefs.getString(KEY_ALERT_LOG, null) ?: return mutableListOf()
        return try {
            // BUG-6 FIX: parse entry-by-entry so a single corrupted record
            // (null/unknown AlertType) does not wipe the entire list
            val arr = com.google.gson.JsonParser.parseString(json).asJsonArray
            arr.mapNotNull { element ->
                try { gson.fromJson(element, AlertEntry::class.java) }
                catch (_: Exception) { null }
            }.toMutableList()
        } catch (_: Exception) { mutableListOf() }
    }

    fun addAlert(entry: AlertEntry) {
        val list = loadAlerts()
        list.add(0, entry)                          // newest first
        if (list.size > MAX_ALERT_LOG_SIZE) list.subList(MAX_ALERT_LOG_SIZE, list.size).clear()
        prefs.edit { putString(KEY_ALERT_LOG, gson.toJson(list)) }
    }

    fun clearAlerts() {
        prefs.edit { remove(KEY_ALERT_LOG) }
    }

    // ── Convenience log helpers ──────────────────────────────────

    fun logSuccess(title: String, message: String) =
        addAlert(AlertEntry(type = AlertType.SUCCESS, title = title, message = message))

    fun logError(title: String, message: String) =
        addAlert(AlertEntry(type = AlertType.ERROR, title = title, message = message))

    fun logInfo(title: String, message: String) =
        addAlert(AlertEntry(type = AlertType.INFO, title = title, message = message))

    fun logWarning(title: String, message: String) =
        addAlert(AlertEntry(type = AlertType.WARNING, title = title, message = message))
}
