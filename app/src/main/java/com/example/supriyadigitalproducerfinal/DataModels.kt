package com.example.supriyadigitalproducerfinal

import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// ─────────────────────────────────────────────────────────────────
// HELPER FUNCTIONS
// ─────────────────────────────────────────────────────────────────

fun Event.deepCopy(): Event {
    val gson = Gson()
    return gson.fromJson(gson.toJson(this), Event::class.java)
}

/**
 * BUG-8 FIX: DatePicker always returns UTC-midnight millis.
 * Formatting with UTC timezone prevents ±1-day drift in IST (UTC+5:30).
 */
fun formatDateFromMillis(millis: Long): String =
    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        .apply { timeZone = TimeZone.getTimeZone("UTC") }
        .format(Date(millis))

fun parseDateToUtcMillis(dateStr: String): Long =
    try {
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .parse(dateStr)?.time ?: System.currentTimeMillis()
    } catch (_: Exception) { System.currentTimeMillis() }

// ─────────────────────────────────────────────────────────────────
// DATA CLASSES — field names unchanged; Firestore documents untouched
// ─────────────────────────────────────────────────────────────────

data class MasterItem(
    val id: Long = 0,
    var name: String = "",
    var cost: Double = 0.0
)

data class MasterTaker(
    val id: Long = 0,
    var name: String = "",
    var defaultTotalDue: Double = 0.0,
    var defaultPhone: String = ""
)

data class TakerEventSummary(
    val eventId: Long,
    val eventTitle: String,
    val dayTitle: String,
    val dayDate: String,
    val totalDue: Double,
    val amountPaid: Double
)

data class Item(
    val id: Long = 0,
    var name: String = "",
    var cost: Double = 0.0,
    var quantity: Int = 0
)

data class Taker(
    val id: Long = 0,
    var name: String = "",
    var totalDue: Double = 0.0,
    var amountPaid: Double = 0.0
)

data class Payment(
    val id: Long = 0,
    var title: String = "",
    var amount: Double = 0.0,
    var date: String = ""
)

data class Day(
    val id: Long = 0,
    var title: String = "",
    var date: String = "",
    var items: MutableList<Item> = mutableListOf(),
    var takers: MutableList<Taker> = mutableListOf()
)

data class InternalExpense(
    val id: Long = 0,
    var title: String = "",
    var amount: Double = 0.0,
    var date: String = ""
)

data class Event(
    val id: Long = 0,
    var title: String = "",
    var date: String = "",
    var customerName: String = "",
    var customerPhone: String = "",
    var days: MutableList<Day> = mutableListOf(),
    var eventItems: MutableList<Item> = mutableListOf(),
    var discount: Double = 0.0,
    var payments: MutableList<Payment> = mutableListOf(),
    var internalExpenses: MutableList<InternalExpense> = mutableListOf(),
    var isConfirmed: Boolean = false
)

data class AlbumSettings(
    var baseCost: Double = 10000.0,
    var defaultSheets: Int = 30,
    var extraSheetCost: Double = 250.0
)

// Analytics data classes
data class MonthlyStats(
    val month: String,
    val revenue: Double,
    val takerPayouts: Double,
    val netIncome: Double
)

data class TakerPayout(
    val takerName: String,
    val eventTitle: String,
    val dayTitle: String,
    val dayDate: String,
    val amountDue: Double,
    val amountPaid: Double
)

// Internal navigation helpers
internal data class EditingTakerInfo(val taker: Taker, val eventId: Long, val dayId: Long)
internal enum class SortOrder { NONE, ASCENDING, DESCENDING, CONFIRMED_ONLY, UNCONFIRMED_ONLY }
