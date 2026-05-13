package com.example.supriyadigitalproducerfinal

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.example.supriyadigitalproducerfinal.ui.theme.BalanceDue
import com.example.supriyadigitalproducerfinal.ui.theme.BalancePaid
import java.util.Locale
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.ui.graphics.Color as ComposeColor

// ─────────────────────────────────────────────────────────────────
// TAKERS DASHBOARD
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakersDashboardScreen(
    events: List<Event>,
    masterTakers: List<MasterTaker>,
    onTakerUpdated: (Long, Long, Taker) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showFinderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Takers Dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showFinderDialog = true }) {
                        Icon(Icons.Filled.PersonSearch, "Find Free Taker")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            TabRow(selectedTabIndex) {
                listOf("By Day", "By Taker").forEachIndexed { i, title ->
                    Tab(selected = selectedTabIndex == i, onClick = { selectedTabIndex = i }, text = { Text(title) })
                }
            }
            when (selectedTabIndex) {
                0 -> GroupedByDayTakersView(events, masterTakers, onTakerUpdated)
                1 -> GroupedByTakerView(events, masterTakers, onTakerUpdated)
            }
        }
    }

    if (showFinderDialog) {
        FreeTakerFinderDialog(events = events, masterTakers = masterTakers, onDismiss = { showFinderDialog = false })
    }
}

// ─────────────────────────────────────────────────────────────────
// GROUPED BY DAY VIEW
// ─────────────────────────────────────────────────────────────────

@Composable
fun GroupedByDayTakersView(events: List<Event>, masterTakers: List<MasterTaker>, onTakerUpdated: (Long, Long, Taker) -> Unit) {
    var editingTakerInfo by remember { mutableStateOf<EditingTakerInfo?>(null) }
    val eventsWithTakers = remember(events.toList()) { events.filter { e -> e.days.any { it.takers.isNotEmpty() } } }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (eventsWithTakers.isEmpty()) {
            item { EmptyState(Icons.Outlined.Groups, "No takers yet", "Add takers inside event days to see them here.", modifier = Modifier.fillParentMaxWidth()) }
        }
        eventsWithTakers.forEach { event ->
            item(key = "header_${event.id}") { SectionHeader(event.title) }
            items(event.days.filter { it.takers.isNotEmpty() }, key = { it.id }) { day ->
                ExpandableDayTakerCard(event, day, onEditTaker = { taker -> editingTakerInfo = EditingTakerInfo(taker, event.id, day.id) })
            }
        }
    }

    if (editingTakerInfo != null) {
        AddEditTakerDialog(
            taker          = editingTakerInfo!!.taker,
            masterTakers   = masterTakers,
            onDismiss      = { editingTakerInfo = null },
            onConfirm      = { updatedTaker -> onTakerUpdated(editingTakerInfo!!.eventId, editingTakerInfo!!.dayId, updatedTaker); editingTakerInfo = null },
            onUpdateMasterTaker = null
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// GROUPED BY TAKER VIEW
// ─────────────────────────────────────────────────────────────────

@Composable
fun GroupedByTakerView(events: List<Event>, masterTakers: List<MasterTaker>, onTakerUpdated: (Long, Long, Taker) -> Unit) {
    // BUG-FIX: case-insensitive lookup so phone shows regardless of name capitalisation
    val masterTakersMap = remember(masterTakers) { masterTakers.associateBy { it.name.lowercase() } }
    val allTakersGrouped = remember(events.toList(), masterTakers) {
        try {
            events.flatMap { event ->
                event.days.flatMap { day ->
                    day.takers.map { taker ->
                        Triple(taker.name, masterTakersMap[taker.name.lowercase()]?.defaultPhone ?: "",
                            TakerEventSummary(event.id, event.title, day.title, day.date, taker.totalDue, taker.amountPaid))
                    }
                }
            }.groupBy { it.first }.mapValues { (_, summaries) ->
                Triple(summaries.sumOf { it.third.totalDue }, summaries.sumOf { it.third.amountPaid },
                    Pair(summaries.first().second, summaries.map { it.third }))
            }.toList().sortedBy { it.first }
        } catch (e: Exception) { Log.e("GroupedByTakerView", "Error", e); emptyList() }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (allTakersGrouped.isEmpty()) {
            item { EmptyState(Icons.Outlined.Groups, "No takers yet", "Add takers to event days to see them here.", modifier = Modifier.fillParentMaxWidth()) }
        }
        items(allTakersGrouped) { (takerName, summary) ->
            val (totalDue, totalPaid, phoneAndSummaries) = summary
            val (phone, eventSummaries) = phoneAndSummaries
            ExpandableTakerSummaryCard(takerName, totalDue - totalPaid, phone, eventSummaries, LocalContext.current)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// TAKER SUMMARY TEXT — fixed box-drawing formatting
// ─────────────────────────────────────────────────────────────────

private fun generateTakerSummaryText(takerName: String, totalBalance: Double, eventSummaries: List<TakerEventSummary>): String {
    val totalDue  = eventSummaries.sumOf { it.totalDue }
    val totalPaid = eventSummaries.sumOf { it.amountPaid }
    fun money(d: Double) = "₹${String.format(Locale.getDefault(), "%,.2f", d)}"
    // FIX: only show events with outstanding balance; if all clear, show all
    val unpaidSummaries  = eventSummaries.filter { it.totalDue - it.amountPaid > 0.0 }
    val displaySummaries = if (unpaidSummaries.isNotEmpty()) unpaidSummaries else eventSummaries
    val details = displaySummaries.joinToString("\n\n") { s ->
        val bal = s.totalDue - s.amountPaid
        "*${s.eventTitle}*\n" +
                "  ${s.dayTitle} · ${s.dayDate}\n" +
                "  Due : ${money(s.totalDue)}\n" +
                "  Paid: ${money(s.amountPaid)}\n" +
                "  Bal : ${money(bal)}"
    }
    val footer = if (totalBalance <= 0) "✅ All dues cleared. Thank you!" else "⚠️ Balance pending: ${money(totalBalance)}"
    return """*Supriya Digital Studio*
*Taker Financial Summary*

*Taker:* $takerName

*BOOKING DETAILS*
$details

*Total Due :* ${money(totalDue)}
*Amount Paid:* ${money(totalPaid)}
*Outstanding:* ${money(totalBalance)}

$footer

_Shared via Supriya Digital Producer App._""".trimIndent()
}

// ─────────────────────────────────────────────────────────────────
// TARGETED TAKER MESSAGE — only that taker's own data
// Used for the "Send to Taker" SMS/share button on TakerItemCard
// ─────────────────────────────────────────────────────────────────

private fun buildTargetedTakerMessage(
    taker: Taker,
    eventTitle: String,
    dayTitle: String,
    dayDate: String
): String {
    val balance = taker.totalDue - taker.amountPaid
    fun money(d: Double) = "₹${String.format(Locale.getDefault(), "%,.2f", d)}"
    return """Hello *${taker.name}*,

This is a reminder from *Supriya Digital Studio* regarding your upcoming assignment.

*Event :* $eventTitle
*Day   :* $dayTitle
*Date  :* $dayDate

*Amount Due :* ${money(taker.totalDue)}
*Amount Paid:* ${money(taker.amountPaid)}
*Balance    :* ${money(balance)}

${if (balance <= 0) "✅ Your dues are fully cleared. Thank you!" else "⚠️ Please ensure the balance amount is ready on the day of the event."}

_Regards,_
_Supriya Digital Studio_
_Ph: 9246789966_""".trimIndent()
}

// ─────────────────────────────────────────────────────────────────
// EXPANDABLE TAKER SUMMARY CARD
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableTakerSummaryCard(takerName: String, totalBalance: Double, takerPhone: String, eventSummaries: List<TakerEventSummary>, context: Context) {
    var expanded        by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    val summaryText = remember(takerName, totalBalance, eventSummaries) { generateTakerSummaryText(takerName, totalBalance, eventSummaries) }
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager }

    ElevatedCard(modifier = Modifier.fillMaxWidth().animateContentSize().combinedClickable(onClick = { expanded = !expanded }, onLongClick = { showContextMenu = true })) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    InitialsAvatar(name = takerName, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(takerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (takerPhone.isNotBlank()) {
                                Spacer(Modifier.width(6.dp))
                                IconButton(onClick = { try { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$takerPhone".toUri())) } catch (_: Exception) {} }, Modifier.size(24.dp)) {
                                    Icon(Icons.Filled.Phone, "Call", tint = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                        Text("Balance: ₹${String.format(Locale.getDefault(), "%,.2f", totalBalance)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (totalBalance > 0) BalanceDue else BalancePaid)
                    }
                }
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                eventSummaries.forEach { summary ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(10.dp)) {
                            Text(summary.eventTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("${summary.dayTitle} (${summary.dayDate})", style = MaterialTheme.typography.bodySmall)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Due: ₹${String.format(Locale.getDefault(), "%,.0f", summary.totalDue)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Paid: ₹${String.format(Locale.getDefault(), "%,.0f", summary.amountPaid)}", style = MaterialTheme.typography.bodySmall, color = BalancePaid)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContextMenu) {
        val cleanPhone = takerPhone.filter { it.isDigit() || it == '+' }
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text("Share — $takerName") },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // WhatsApp direct if phone available
                    if (cleanPhone.isNotBlank()) {
                        TextButton(onClick = {
                            try {
                                val wa = Intent(Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://wa.me/$cleanPhone?text=${java.net.URLEncoder.encode(summaryText, "UTF-8")}")
                                    setPackage("com.whatsapp")
                                }
                                context.startActivity(wa)
                            } catch (_: Exception) {
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, summaryText) }, "Share"))
                            }
                            showContextMenu = false
                        }) {
                            Icon(Icons.Filled.Send, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Send via WhatsApp")
                        }
                    }
                    TextButton(onClick = { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, summaryText) }, "Share")); showContextMenu = false }) {
                        Icon(Icons.Filled.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Share via Other App")
                    }
                    TextButton(onClick = { clipboardManager?.setPrimaryClip(android.content.ClipData.newPlainText("Taker Summary", summaryText)); Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show(); showContextMenu = false }) {
                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Copy")
                    }
                }
            },
            dismissButton = { TextButton(onClick = { showContextMenu = false }) { Text("Cancel") } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// EXPANDABLE DAY TAKER CARD
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableDayTakerCard(event: Event, day: Day, onEditTaker: (Taker) -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    ElevatedCard(Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }, Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text("${day.title} (${day.date})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(event.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
                Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
            }
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                day.takers.forEach { taker -> TakerItemCard(taker = taker, onEditClick = { onEditTaker(taker) }); Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// TAKER PAYMENT SCREEN — with smart per-taker send button
// BUG-2 FIX: localTakers copy, never mutates state directly
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakerPaymentScreen(
    day: Day,
    eventTitle: String = "",  // BUG-7 FIX: needed for targeted taker messages
    masterTakers: List<MasterTaker>,
    onBack: () -> Unit,
    onSaveTakers: (dayId: Long, updatedTakers: List<Taker>) -> Unit,
    onMasterTakerUpdated: (MasterTaker) -> Unit
) {
    var showTakerDialog by remember { mutableStateOf<Taker?>(null) }
    var takerToDelete   by remember { mutableStateOf<Taker?>(null) }
    var showSendDialog  by remember { mutableStateOf<Taker?>(null) }
    val context = LocalContext.current

    val localTakers = remember(day.id) { mutableStateListOf<Taker>().also { it.addAll(day.takers) } }
    LaunchedEffect(day.takers.toList()) {
        val incoming = day.takers.toList()
        if (localTakers.map { it.id }.toSet() != incoming.map { it.id }.toSet()) { localTakers.clear(); localTakers.addAll(incoming) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${day.title} – Takers") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTakerDialog = Taker(System.currentTimeMillis()) }) { Icon(Icons.Filled.Add, "Add Taker") }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (localTakers.isEmpty()) {
                item { EmptyState(Icons.Outlined.PersonAdd, "No takers", "Tap '+' to add a taker for this day.", modifier = Modifier.fillParentMaxWidth()) }
            }
            items(localTakers, key = { it.id }) { taker ->
                // Enhanced TakerItemCard with Send button
                TakerPaymentItemCard(
                    taker        = taker,
                    date         = day.date,
                    onEditClick  = { showTakerDialog = taker },
                    onDeleteClick = { takerToDelete = taker },
                    onSendClick  = { showSendDialog = taker }
                )
            }
        }
    }

    // Send targeted message dialog
    if (showSendDialog != null) {
        val taker      = showSendDialog!!
        // FIX: case-insensitive phone lookup to match master list
        val phone      = masterTakers.find { it.name.equals(taker.name, ignoreCase = true) }?.defaultPhone ?: ""
        val cleanPhone = phone.filter { it.isDigit() || it == '+' }
        val notifStore = remember { NotificationSettingsStore(context) }
        val message    = buildTargetedTakerMessage(taker, eventTitle, day.title, day.date)

        AlertDialog(
            onDismissRequest = { showSendDialog = null },
            title = { Text("Send to ${taker.name}") },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose how to send the reminder to ${taker.name}.", style = MaterialTheme.typography.bodyMedium)
                    if (cleanPhone.isNotBlank()) {
                        Text("Phone: $phone", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("⚠️ No phone number saved. Add it in Master Takers list.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // WhatsApp direct — preferred channel; opens WhatsApp pre-filled with number
                    if (cleanPhone.isNotBlank()) {
                        androidx.compose.material3.Button(
                            onClick = {
                                try {
                                    val wa = Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("https://wa.me/$cleanPhone?text=${java.net.URLEncoder.encode(message, "UTF-8")}")
                                        setPackage("com.whatsapp")
                                    }
                                    context.startActivity(wa)
                                    notifStore.logSuccess("WhatsApp Opened", "Message pre-filled for ${taker.name} ($phone)")
                                } catch (_: Exception) {
                                    // WhatsApp not installed — fall back to share sheet
                                    val fallback = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message)
                                    }
                                    context.startActivity(Intent.createChooser(fallback, "Send to ${taker.name}"))
                                    notifStore.logInfo("Share Fallback", "WhatsApp not installed; opened share sheet for ${taker.name}")
                                }
                                showSendDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Icon(Icons.Filled.Send, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Send via WhatsApp") }
                    }
                    // SMS — only if phone available and SMS toggle is on
                    if (cleanPhone.isNotBlank() && notifStore.smsEnabled) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                try {
                                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                        context.getSystemService(android.telephony.SmsManager::class.java)
                                    else @Suppress("DEPRECATION") android.telephony.SmsManager.getDefault()
                                    if (smsManager == null) { Toast.makeText(context, "SmsManager unavailable.", Toast.LENGTH_SHORT).show(); return@OutlinedButton }
                                    val parts = smsManager.divideMessage(message)
                                    smsManager.sendMultipartTextMessage(cleanPhone, null, parts, null, null)
                                    notifStore.logSuccess("Taker SMS Sent", "Reminder sent to ${taker.name} ($phone)")
                                    Toast.makeText(context, "SMS sent to ${taker.name}!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    notifStore.logError("Taker SMS Failed", "Could not send to ${taker.name}: ${e.message}")
                                    Toast.makeText(context, "SMS failed: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                                showSendDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Icon(Icons.Filled.Send, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Send SMS") }
                    }
                    // Generic share sheet (fallback for any other app)
                    androidx.compose.material3.TextButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Send to ${taker.name}"))
                            showSendDialog = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Icon(Icons.Filled.Share, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Share via Other App") }
                }
            },
            dismissButton = { TextButton(onClick = { showSendDialog = null }) { Text("Cancel") } }
        )
    }

    if (showTakerDialog != null) {
        AddEditTakerDialog(
            taker = showTakerDialog!!, masterTakers = masterTakers,
            onDismiss = { showTakerDialog = null },
            onConfirm = { updatedTaker ->
                val idx = localTakers.indexOfFirst { it.id == updatedTaker.id }
                if (idx != -1) localTakers[idx] = updatedTaker else localTakers.add(updatedTaker)
                onSaveTakers(day.id, localTakers.toList()); showTakerDialog = null
            },
            onUpdateMasterTaker = onMasterTakerUpdated
        )
    }
    if (takerToDelete != null) {
        ConfirmDeleteDialog("Remove Taker", "Remove '${takerToDelete!!.name}' from this day?",
            onDismiss = { takerToDelete = null },
            onConfirm = { localTakers.removeIf { it.id == takerToDelete!!.id }; onSaveTakers(day.id, localTakers.toList()); takerToDelete = null })
    }
}

// ─────────────────────────────────────────────────────────────────
// TAKER PAYMENT ITEM CARD — enhanced with Send button
// ─────────────────────────────────────────────────────────────────

@Composable
fun TakerPaymentItemCard(taker: Taker, date: String? = null, onEditClick: () -> Unit, onDeleteClick: () -> Unit, onSendClick: () -> Unit) {
    val balance = taker.totalDue - taker.amountPaid
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            InitialsAvatar(name = taker.name, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(taker.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                date?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("Balance: ₹${String.format(Locale.getDefault(), "%,.2f", balance)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (balance > 0) BalanceDue else BalancePaid)
            }
            // Send targeted message button
            IconButton(onClick = onSendClick) {
                Icon(Icons.Filled.Send, "Send reminder to ${taker.name}", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEditClick) {
                Icon(Icons.Outlined.Edit, "Edit Taker")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// ALL TAKERS SUMMARY — BUG-14 FIX: remember(event)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTakersSummaryScreen(event: Event, onBack: () -> Unit, onAddTakerToDays: (String, Double, Double, List<Long>) -> Unit) {
    var showAddTakerDialog by remember { mutableStateOf(false) }
    val daysWithTakers = remember(event) { event.days.filter { it.takers.isNotEmpty() } }
    val grandTotalDue  = remember(event) { event.days.sumOf { d -> d.takers.sumOf { it.totalDue } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("${event.title} – Takers", style = MaterialTheme.typography.titleMedium); Text("Total Due: ₹${String.format(Locale.getDefault(), "%,.2f", grandTotalDue)}", style = MaterialTheme.typography.bodySmall) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddTakerDialog = true }) { Icon(Icons.Filled.Add, "Add Taker") } }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (daysWithTakers.isEmpty()) {
                item { EmptyState(Icons.Outlined.Groups, "No takers yet", "Add takers to individual days.", modifier = Modifier.fillParentMaxWidth()) }
            }
            items(daysWithTakers) { day ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${day.title} (${day.date})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        day.takers.forEach { taker -> TakerItemCard(taker = taker, onEditClick = {}); Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
    if (showAddTakerDialog) {
        AddTakerToEventDialog(event = event, onDismiss = { showAddTakerDialog = false }, onConfirm = { n, td, ap, dayIds -> onAddTakerToDays(n, td, ap, dayIds); showAddTakerDialog = false })
    }
}

// ─────────────────────────────────────────────────────────────────
// FREE TAKER FINDER DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeTakerFinderDialog(events: List<Event>, masterTakers: List<MasterTaker>, onDismiss: () -> Unit) {
    var selectedDate    by remember { mutableStateOf(formatDateFromMillis(System.currentTimeMillis())) }
    var showDatePicker  by remember { mutableStateOf(false) }
    val datePickerState = androidx.compose.material3.rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    val busyTakerNames = remember(selectedDate, events) {
        events.flatMap { event -> event.days.filter { it.date == selectedDate }.flatMap { day -> day.takers.map { it.name } } }.toSet()
    }
    val freeTakers = remember(busyTakerNames, masterTakers) { masterTakers.filter { it.name !in busyTakerNames } }
    val busyTakers = remember(busyTakerNames, events, selectedDate) {
        events.flatMap { event -> event.days.filter { it.date == selectedDate }.flatMap { day -> day.takers.map { taker -> Triple(taker.name, event.title, day.title) } } }.distinctBy { it.first }
    }

    if (showDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { selectedDate = formatDateFromMillis(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton  = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { androidx.compose.material3.DatePicker(state = datePickerState) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PersonSearch, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Taker Availability", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.OutlinedTextField(value = selectedDate, onValueChange = {}, label = { Text("Check Availability On") }, readOnly = true, modifier = Modifier.fillMaxWidth(), trailingIcon = { Icon(Icons.Filled.CalendarToday, "Pick Date", modifier = Modifier.clickable { showDatePicker = true }) })
                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = BalancePaid.copy(alpha = 0.15f), modifier = Modifier.size(28.dp)) { Box(contentAlignment = Alignment.Center) { Text("${freeTakers.size}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BalancePaid) } }
                            Spacer(Modifier.width(8.dp))
                            Text("Available Takers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BalancePaid)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (freeTakers.isEmpty()) {
                        item { Text("All takers are booked on this date.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp)) }
                    } else {
                        items(freeTakers) { taker ->
                            Surface(shape = RoundedCornerShape(10.dp), color = BalancePaid.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    InitialsAvatar(name = taker.name, size = 36.dp); Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(taker.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        if (taker.defaultPhone.isNotBlank()) Text(taker.defaultPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusChip("Free", BalancePaid)
                                }
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp)); HorizontalDivider(); Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = BalanceDue.copy(alpha = 0.15f), modifier = Modifier.size(28.dp)) { Box(contentAlignment = Alignment.Center) { Text("${busyTakers.size}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BalanceDue) } }
                            Spacer(Modifier.width(8.dp)); Text("Booked Takers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BalanceDue)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (busyTakers.isEmpty()) {
                        item { Text("No takers booked on this date.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        items(busyTakers) { (takerName, eventTitle, dayTitle) ->
                            Surface(shape = RoundedCornerShape(10.dp), color = BalanceDue.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    InitialsAvatar(name = takerName, size = 36.dp); Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(takerName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("$dayTitle · $eventTitle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    StatusChip("Booked", BalanceDue)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}