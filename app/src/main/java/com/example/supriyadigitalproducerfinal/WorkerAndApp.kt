package com.example.supriyadigitalproducerfinal

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────────
// APPLICATION CLASS
// ─────────────────────────────────────────────────────────────────

class ProducerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleEventReminders()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "EVENT_REMINDER_CHANNEL_ID",
                "Event Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Daily reminders for upcoming events" }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    // PUBLIC so rescheduleReminder() helper can call it after settings change
    fun scheduleEventReminders() {
        val store        = NotificationSettingsStore(this)
        val targetHour   = store.reminderHour
        val targetMinute = store.reminderMinute

        // Calculate initial delay so first run fires AT the user-chosen time
        val now    = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // If target time has already passed today → schedule for tomorrow
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        val initialDelayMs = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<EventReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()

        // REPLACE so re-scheduling picks up any new time the user has set
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "eventReminderWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
        Log.i("ProducerApp", "Reminder scheduled at ${store.formattedReminderTime()}, first run in ${initialDelayMs / 60000} min")
    }
}

// ─────────────────────────────────────────────────────────────────
// EVENT REMINDER WORKER
// BUG-1  FIX: reads events from Firestore, not dead SharedPreferences
// BUG-10 FIX: SmsManager null-checked before use
// NEW:  respects SMS toggle + dynamic phone number + reminder time
//       logs every outcome to NotificationSettingsStore (AlertsScreen)
// ─────────────────────────────────────────────────────────────────

class EventReminderWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val store   = NotificationSettingsStore(context)
        val db      = com.google.firebase.firestore.FirebaseFirestore.getInstance()

        // BUG-1 FIX: fetch live data from Firestore
        val events: List<Event> = try {
            db.collection("events").get().await()
                .documents.mapNotNull { it.toObject(Event::class.java) }
        } catch (e: Exception) {
            val msg = "Could not fetch events: ${e.message}"
            Log.e("EventReminderWorker", msg, e)
            store.logError("Reminder Failed", msg)
            return Result.retry()
        }

        val sdf         = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val todayCal    = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }
        val tomorrowStr = sdf.format((todayCal.clone() as Calendar).also { it.add(Calendar.DAY_OF_YEAR, 1) }.time)
        val dayAfterStr = sdf.format((todayCal.clone() as Calendar).also { it.add(Calendar.DAY_OF_YEAR, 2) }.time)

        val tomorrowEvents = mutableListOf<String>()
        val dayAfterEvents = mutableListOf<String>()

        fun fmt(takers: List<Taker>) =
            if (takers.isEmpty()) "None"
            else takers.joinToString(", ") { "${it.name} (Due ₹${it.totalDue}, Paid ₹${it.amountPaid})" }

        // BUG-5 FIX: only add main event entry if none of its days match the target date
        // (prevents duplicate lines when main event date == a sub-day date)
        events.forEach { event ->
            val eventDayDatesTomorrow = event.days.any { it.date == tomorrowStr }
            val eventDayDatesDayAfter = event.days.any { it.date == dayAfterStr }
            if (event.date == tomorrowStr && !eventDayDatesTomorrow)
                tomorrowEvents.add("Event: ${event.title}\nClient: ${event.customerName}")
            else if (event.date == dayAfterStr && !eventDayDatesDayAfter)
                dayAfterEvents.add("Event: ${event.title}\nClient: ${event.customerName}")
            event.days.forEach { day ->
                if (day.date == tomorrowStr) tomorrowEvents.add("Day: ${day.title} (${event.title})\nTakers: ${fmt(day.takers)}")
                else if (day.date == dayAfterStr) dayAfterEvents.add("Day: ${day.title} (${event.title})\nTakers: ${fmt(day.takers)}")
            }
        }

        if (tomorrowEvents.isEmpty() && dayAfterEvents.isEmpty()) {
            store.logInfo("Daily Check Complete", "No upcoming events for tomorrow or day after. ${events.size} event(s) checked.")
            return Result.success()
        }

        val sb = StringBuilder("Supriya Digital Reminder:\n\n")
        if (tomorrowEvents.isNotEmpty()) { sb.append("--- TOMORROW ---\n"); tomorrowEvents.forEach { sb.append(it).append("\n\n") } }
        if (dayAfterEvents.isNotEmpty()) { sb.append("--- DAY AFTER ---\n"); dayAfterEvents.forEach { sb.append(it).append("\n\n") } }
        val msg         = sb.toString().trim()
        val totalEvents = tomorrowEvents.size + dayAfterEvents.size

        // Always send in-app notification
        sendNotification(context, msg)
        store.logSuccess("Reminder Notification Sent", "$totalEvents upcoming event(s) found. Notification delivered.")

        // SMS — only if toggle is ON
        if (store.smsEnabled) {
            val phone = store.reminderPhone
            if (phone.isBlank()) {
                store.logWarning("SMS Skipped", "No phone number set. Add one in Settings → Notification Settings.")
            } else {
                val ok = sendSms(context, phone, msg)
                if (ok) store.logSuccess("SMS Sent", "Sent to $phone — $totalEvents event(s) covered.")
                else    store.logError("SMS Failed", "Could not send to $phone. Check permissions or SIM.")
            }
        } else {
            store.logInfo("SMS Skipped", "SMS reminders are turned off in Notification Settings.")
        }

        return Result.success()
    }

    private fun sendSms(context: Context, phone: String, message: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("EventReminder", "SEND_SMS permission not granted"); return false
        }
        return try {
            // BUG-10 FIX: null-check SmsManager
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                context.getSystemService(android.telephony.SmsManager::class.java)
            else @Suppress("DEPRECATION") android.telephony.SmsManager.getDefault()

            if (smsManager == null) { Log.e("EventReminder", "SmsManager null (dual-SIM?)"); return false }
            smsManager.sendMultipartTextMessage(phone, null, smsManager.divideMessage(message), null, null)
            true
        } catch (e: Exception) { Log.e("EventReminder", "SMS failed", e); false }
    }

    private fun sendNotification(context: Context, fullMessage: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            1001,
            NotificationCompat.Builder(context, "EVENT_REMINDER_CHANNEL_ID")
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Upcoming Events Alert")
                .setStyle(NotificationCompat.BigTextStyle().bigText(fullMessage))
                .setContentText("You have upcoming events. Expand to view.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true).build()
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// HELPERS — call these after user changes settings
// ─────────────────────────────────────────────────────────────────

/**
 * Re-schedules the periodic worker with the latest time from NotificationSettingsStore.
 * Call this after the user saves a new reminder time.
 */
fun rescheduleReminder(context: Context) {
    (context.applicationContext as? ProducerApplication)?.scheduleEventReminders() ?: run {
        val store      = NotificationSettingsStore(context)
        val now        = Calendar.getInstance()
        val target     = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, store.reminderHour); set(Calendar.MINUTE, store.reminderMinute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        val delayMs = target.timeInMillis - now.timeInMillis
        val request = PeriodicWorkRequestBuilder<EventReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("eventReminderWork", ExistingPeriodicWorkPolicy.REPLACE, request)
    }
}

/**
 * Fires a one-off immediate reminder check — used by the test button in Settings.
 */
fun triggerTestReminder(context: Context) {
    WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<EventReminderWorker>().build())
}
