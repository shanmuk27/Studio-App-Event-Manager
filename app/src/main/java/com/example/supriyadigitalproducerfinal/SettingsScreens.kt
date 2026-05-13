package com.example.supriyadigitalproducerfinal

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

// ─────────────────────────────────────────────────────────────────
// SETTINGS SCREEN — Bug-5 fixed + Notification Settings section
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialMasterItems: List<MasterItem>,
    initialMasterTakers: List<MasterTaker>,
    initialAlbumSettings: AlbumSettings,
    eventsCount: Int,
    deletedEvents: List<Event>,
    onUpdateMasterItems: (List<MasterItem>) -> Unit,
    onUpdateMasterTakers: (List<MasterTaker>) -> Unit,
    onDeleteMasterTaker: (Long) -> Unit,
    onUpdateAlbumSettings: (AlbumSettings) -> Unit,
    onRestoreEvent: (Event) -> Unit,
    onPermanentlyDeleteEvent: (Event) -> Unit
) {
    val context = LocalContext.current
    val notifStore = remember { NotificationSettingsStore(context) }

    // Album settings
    var baseCost       by remember { mutableStateOf(initialAlbumSettings.baseCost.toString()) }
    var defaultSheets  by remember { mutableStateOf(initialAlbumSettings.defaultSheets.toString()) }
    var extraSheetCost by remember { mutableStateOf(initialAlbumSettings.extraSheetCost.toString()) }

    // BUG-5 FIX: re-seed local lists whenever Firestore pushes new data
    val localItemList = remember { mutableStateListOf<MasterItem>().also { it.addAll(initialMasterItems) } }
    LaunchedEffect(initialMasterItems) {
        if (localItemList.map { it.id }.toSet() != initialMasterItems.map { it.id }.toSet() ||
            localItemList.any { local -> initialMasterItems.find { it.id == local.id }?.let { it.name != local.name || it.cost != local.cost } == true }) {
            localItemList.clear(); localItemList.addAll(initialMasterItems)
        }
    }

    val localMasterTakerList = remember { mutableStateListOf<MasterTaker>().also { it.addAll(initialMasterTakers) } }
    LaunchedEffect(initialMasterTakers) {
        if (localMasterTakerList.map { it.id }.toSet() != initialMasterTakers.map { it.id }.toSet() ||
            localMasterTakerList.any { local -> initialMasterTakers.find { it.id == local.id }?.let { it.name != local.name } == true }) {
            localMasterTakerList.clear(); localMasterTakerList.addAll(initialMasterTakers)
        }
    }

    // Notification settings state (live from store)
    var smsEnabled    by remember { mutableStateOf(notifStore.smsEnabled) }
    var reminderPhone by remember { mutableStateOf(notifStore.reminderPhone) }
    var reminderHour  by remember { mutableIntStateOf(notifStore.reminderHour) }
    var reminderMin   by remember { mutableIntStateOf(notifStore.reminderMinute) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPhoneDialog by remember { mutableStateOf(false) }

    var showAddItemDialog   by remember { mutableStateOf(false) }
    var editingItem         by remember { mutableStateOf<MasterItem?>(null) }
    var showAddTakerDialog  by remember { mutableStateOf(false) }
    var editingTaker        by remember { mutableStateOf<MasterTaker?>(null) }
    var showTrashBinDialog  by remember { mutableStateOf(false) }

    val totalDocuments = localItemList.size + localMasterTakerList.size + eventsCount
    val storagePct     = (totalDocuments.toFloat() / 50000f).coerceIn(0f, 1f)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // ── Cloud storage ─────────────────────────────────────────
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Cloud Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("Firestore free-tier usage (approx.)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val animPct by animateFloatAsState(storagePct, tween(800), label = "storagePct")
                    LinearProgressIndicator(progress = { animPct }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = if (storagePct > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, strokeCap = StrokeCap.Round)
                    Text("$totalDocuments / 50,000 documents", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            // ── Trash bin ─────────────────────────────────────────────
            Button(onClick = { showTrashBinDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)) {
                Icon(Icons.Filled.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Trash Bin (${deletedEvents.size})")
            }

            HorizontalDivider()

            // ── NOTIFICATION SETTINGS ─────────────────────────────────
            Text("Notification Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

                    // SMS toggle
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (smsEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff, null, Modifier.size(22.dp), if (smsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("SMS Reminders", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(if (smsEnabled) "SMS will be sent daily" else "SMS disabled — in-app only", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = smsEnabled,
                            onCheckedChange = { smsEnabled = it; notifStore.smsEnabled = it }
                        )
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    // Phone number
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Phone, null, Modifier.size(22.dp), MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Reminder Phone", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(reminderPhone.ifBlank { "Not set" }, style = MaterialTheme.typography.bodySmall, color = if (reminderPhone.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        FilledTonalButton(onClick = { showPhoneDialog = true }) { Text("Change") }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    // Reminder time
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AccessTime, null, Modifier.size(22.dp), MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Reminder Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                val h = reminderHour; val m = reminderMin
                                val ampm = if (h < 12) "AM" else "PM"
                                val hh   = if (h % 12 == 0) 12 else h % 12
                                Text("%d:%02d %s".format(hh, m, ampm), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        FilledTonalButton(onClick = { showTimePicker = true }) { Text("Change") }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    // Test buttons
                    Text("Test Tools", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                // Fire the actual WorkManager worker immediately — same code as real reminder
                                triggerTestReminder(context)
                                Toast.makeText(context, "Test reminder queued — check Alerts tab", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Notifications, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Test Alert", maxLines = 1)
                        }

                        Button(
                            onClick = {
                                if (!smsEnabled) { Toast.makeText(context, "SMS is turned off. Enable it first.", Toast.LENGTH_SHORT).show(); return@Button }
                                if (reminderPhone.isBlank()) { Toast.makeText(context, "Set a phone number first.", Toast.LENGTH_SHORT).show(); return@Button }
                                try {
                                    val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                                        context.getSystemService(android.telephony.SmsManager::class.java)
                                    else @Suppress("DEPRECATION") android.telephony.SmsManager.getDefault()
                                    if (smsManager == null) { Toast.makeText(context, "SmsManager unavailable.", Toast.LENGTH_LONG).show(); return@Button }
                                    val parts = smsManager.divideMessage("Supriya Digital TEST: SMS reminders are working!")
                                    smsManager.sendMultipartTextMessage(reminderPhone, null, parts, null, null)
                                    NotificationSettingsStore(context).logSuccess("Test SMS Sent", "Manual test SMS sent to $reminderPhone")
                                    Toast.makeText(context, "Test SMS sent to $reminderPhone!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) { Toast.makeText(context, "SMS failed: ${e.message}", Toast.LENGTH_LONG).show() }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.Sms, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Test SMS", maxLines = 1)
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Album settings ────────────────────────────────────────
            Text("Album Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = baseCost, onValueChange = { baseCost = it }, label = { Text("Base Album Cost (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = defaultSheets, onValueChange = { defaultSheets = it }, label = { Text("Default Sheets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = extraSheetCost, onValueChange = { extraSheetCost = it }, label = { Text("Extra Sheet Cost (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            Button(onClick = {
                onUpdateAlbumSettings(AlbumSettings(baseCost.toDoubleOrNull() ?: initialAlbumSettings.baseCost, defaultSheets.toIntOrNull() ?: initialAlbumSettings.defaultSheets, extraSheetCost.toDoubleOrNull() ?: initialAlbumSettings.extraSheetCost))
                Toast.makeText(context, "Album settings saved", Toast.LENGTH_SHORT).show()
            }, Modifier.fillMaxWidth()) { Text("Save Album Settings") }

            HorizontalDivider()

            // ── Master takers ─────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Master Taker List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FilledTonalButton(onClick = { showAddTakerDialog = true }) { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add") }
            }
            if (localMasterTakerList.isEmpty()) {
                EmptyState(Icons.Outlined.Groups, "No master takers", "Add takers to pre-fill details when assigning them to events.", modifier = Modifier.fillMaxWidth())
            } else {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp)) {
                        localMasterTakerList.forEach { taker ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                InitialsAvatar(name = taker.name, size = 36.dp); Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(taker.name, fontWeight = FontWeight.Medium)
                                    Text("Due: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", taker.defaultTotalDue)} · ${taker.defaultPhone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { editingTaker = taker }) { Icon(Icons.Outlined.Edit, "Edit") }
                                IconButton(onClick = { onDeleteMasterTaker(taker.id); localMasterTakerList.remove(taker); onUpdateMasterTakers(localMasterTakerList.toList()); Toast.makeText(context, "${taker.name} deleted", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Master items ──────────────────────────────────────────
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("Master Item List", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FilledTonalButton(onClick = { showAddItemDialog = true }) { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add") }
            }
            if (localItemList.isEmpty()) {
                EmptyState(Icons.Outlined.AddCircle, "No master items", "Add items to quickly pick them when building event packages.", modifier = Modifier.fillMaxWidth())
            } else {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(8.dp)) {
                        localItemList.forEach { item ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.name, fontWeight = FontWeight.Medium)
                                    Text("₹${String.format(java.util.Locale.getDefault(), "%,.2f", item.cost)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { editingItem = item }) { Icon(Icons.Outlined.Edit, "Edit") }
                                IconButton(onClick = { localItemList.remove(item); onUpdateMasterItems(localItemList.toList()); Toast.makeText(context, "${item.name} deleted", Toast.LENGTH_SHORT).show() }) { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Phone number dialog ───────────────────────────────────────
    if (showPhoneDialog) {
        var tempPhone by remember { mutableStateOf(reminderPhone) }
        AlertDialog(
            onDismissRequest = { showPhoneDialog = false },
            title = { Text("Reminder Phone Number") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This number receives your daily reminder SMS.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = tempPhone, onValueChange = { tempPhone = it.filter { c -> c.isDigit() || c == '+' } }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), placeholder = { Text("+919246789966") })
                    if (tempPhone.isNotBlank() && !tempPhone.startsWith("+"))
                        Text("Include country code, e.g. +91…", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempPhone.isNotBlank() && !tempPhone.startsWith("+")) {
                        Toast.makeText(context, "Include country code starting with '+'", Toast.LENGTH_LONG).show(); return@Button
                    }
                    reminderPhone = tempPhone; notifStore.reminderPhone = tempPhone
                    Toast.makeText(context, "Phone number saved", Toast.LENGTH_SHORT).show()
                    showPhoneDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showPhoneDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Time picker dialog ────────────────────────────────────────
    if (showTimePicker) {
        ReminderTimePicker(
            currentHour   = reminderHour,
            currentMinute = reminderMin,
            onDismiss     = { showTimePicker = false },
            onConfirm     = { h, m ->
                reminderHour = h; reminderMin = m
                notifStore.reminderHour   = h
                notifStore.reminderMinute = m
                rescheduleReminder(context)
                val ampm = if (h < 12) "AM" else "PM"; val hh = if (h % 12 == 0) 12 else h % 12
                Toast.makeText(context, "Reminder time set to %d:%02d %s".format(hh, m, ampm), Toast.LENGTH_SHORT).show()
                showTimePicker = false
            }
        )
    }

    if (showTrashBinDialog) TrashBinDialog(deletedEvents, onRestore = onRestoreEvent, onPermanentlyDelete = onPermanentlyDeleteEvent, onDismiss = { showTrashBinDialog = false })

    if (showAddItemDialog) {
        AddEditMasterItemDialog(onDismiss = { showAddItemDialog = false }, onConfirm = { name, cost ->
            localItemList.add(MasterItem(System.currentTimeMillis(), name, cost)); onUpdateMasterItems(localItemList.toList()); showAddItemDialog = false
            Toast.makeText(context, "$name added", Toast.LENGTH_SHORT).show()
        })
    }
    editingItem?.let { itemToEdit ->
        AddEditMasterItemDialog(item = itemToEdit, onDismiss = { editingItem = null }, onConfirm = { name, cost ->
            val idx = localItemList.indexOfFirst { it.id == itemToEdit.id }
            if (idx != -1) { localItemList[idx] = itemToEdit.copy(name = name, cost = cost); onUpdateMasterItems(localItemList.toList()); Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show() }
            editingItem = null
        })
    }
    if (showAddTakerDialog) {
        AddEditMasterTakerDialog(onDismiss = { showAddTakerDialog = false }, onConfirm = { name, due, phone ->
            localMasterTakerList.add(MasterTaker(System.currentTimeMillis(), name, due, phone)); onUpdateMasterTakers(localMasterTakerList.toList()); showAddTakerDialog = false
            Toast.makeText(context, "$name added", Toast.LENGTH_SHORT).show()
        })
    }
    editingTaker?.let { takerToEdit ->
        AddEditMasterTakerDialog(taker = takerToEdit, onDismiss = { editingTaker = null }, onConfirm = { name, due, phone ->
            val idx = localMasterTakerList.indexOfFirst { it.id == takerToEdit.id }
            if (idx != -1) { localMasterTakerList[idx] = takerToEdit.copy(name = name, defaultTotalDue = due, defaultPhone = phone); onUpdateMasterTakers(localMasterTakerList.toList()); Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show() }
            editingTaker = null
        })
    }
}

// ─────────────────────────────────────────────────────────────────
// REMINDER TIME PICKER DIALOG
// Scrollable hour + minute pickers (no external library needed)
// ─────────────────────────────────────────────────────────────────

@Composable
fun ReminderTimePicker(
    currentHour: Int,
    currentMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour   by remember { mutableIntStateOf(currentHour) }
    var selectedMinute by remember { mutableIntStateOf(currentMinute) }

    // Round minute to nearest 5
    if (selectedMinute % 5 != 0) selectedMinute = (selectedMinute / 5) * 5

    val hours   = (0..23).toList()
    val minutes = (0..59 step 5).toList()

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Reminder Time", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Daily SMS and notification will fire at this time.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))

                // Time display
                val ampm = if (selectedHour < 12) "AM" else "PM"
                val hh   = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                Text("%d:%02d %s".format(hh, selectedMinute, ampm), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(20.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Hour column
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        TimeScrollColumn(
                            values = hours,
                            selected = selectedHour,
                            label = { h ->
                                val ap = if (h < 12) "AM" else "PM"
                                val hDisplay = if (h % 12 == 0) 12 else h % 12
                                "%d %s".format(hDisplay, ap)
                            },
                            onSelect = { selectedHour = it }
                        )
                    }
                    // Minute column
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        TimeScrollColumn(
                            values = minutes,
                            selected = selectedMinute,
                            label = { m -> "%02d".format(m) },
                            onSelect = { selectedMinute = it }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(selectedHour, selectedMinute) }) { Text("Set Time") }
                }
            }
        }
    }
}

@Composable
private fun TimeScrollColumn(
    values: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit
) {
    LazyColumn(
        modifier       = Modifier.height(180.dp),
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(values) { v ->
            val isSelected = v == selected
            ElevatedCard(
                onClick = { onSelect(v) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(if (isSelected) 4.dp else 1.dp)
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(
                        label(v),
                        style      = if (isSelected) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color      = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// TRASH BIN DIALOG
// ─────────────────────────────────────────────────────────────────

@Composable
fun TrashBinDialog(deletedEvents: List<Event>, onRestore: (Event) -> Unit, onPermanentlyDelete: (Event) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth().wrapContentHeight(), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Trash Bin", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Deleted events are stored here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    if (deletedEvents.isEmpty()) {
                        item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("Trash is empty.") } }
                    } else {
                        items(deletedEvents) { event ->
                            ElevatedCard(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(event.title, fontWeight = FontWeight.Bold)
                                    Text("${event.customerName} · ${event.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                                        TextButton(onClick = { onPermanentlyDelete(event) }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete Forever") }
                                        Spacer(Modifier.width(8.dp))
                                        Button(onClick = { onRestore(event) }) { Text("Restore") }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// INTERNAL EXPENSES SCREEN
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalExpensesScreen(event: Event, onBack: () -> Unit, onUpdateExpenses: (List<InternalExpense>) -> Unit) {
    var showAddDialog  by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<InternalExpense?>(null) }
    val expenses   = event.internalExpenses.toList()
    val totalExpense = expenses.sumOf { it.amount }

    val handleUpdate: (InternalExpense) -> Unit = { newExpense ->
        val list = event.internalExpenses.toMutableList()
        val idx  = list.indexOfFirst { it.id == newExpense.id }
        if (idx != -1) list[idx] = newExpense else list.add(newExpense)
        onUpdateExpenses(list); editingExpense = null; showAddDialog = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Internal Expenses", style = MaterialTheme.typography.titleMedium)
                        Text("Total: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", totalExpense)}", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Filled.Add, "Add") } }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (expenses.isEmpty()) {
                item { EmptyState(Icons.Outlined.Payments, "No internal expenses", "These are for internal tracking only and won't appear on client bills.", modifier = Modifier.fillParentMaxWidth()) }
            }
            items(expenses, key = { it.id }) { expense ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(expense.title, fontWeight = FontWeight.Medium)
                            Text(expense.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("₹${String.format(java.util.Locale.getDefault(), "%,.2f", expense.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { editingExpense = expense }) { Icon(Icons.Outlined.Edit, "Edit") }
                        IconButton(onClick = { val list = event.internalExpenses.toMutableList(); list.removeIf { it.id == expense.id }; onUpdateExpenses(list) }) { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (showAddDialog || editingExpense != null) {
        AddEditInternalExpenseDialog(expense = editingExpense, onDismiss = { showAddDialog = false; editingExpense = null }, onConfirm = handleUpdate)
    }
}
