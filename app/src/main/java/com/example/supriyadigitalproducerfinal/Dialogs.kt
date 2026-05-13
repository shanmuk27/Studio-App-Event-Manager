package com.example.supriyadigitalproducerfinal

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// ADD ITEM DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(itemList: List<MasterItem>, onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var name     by remember { mutableStateOf("") }
    var cost     by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = if (name.isEmpty()) emptyList() else itemList.filter { it.name.contains(name, true) }
    LaunchedEffect(filteredOptions) { if (filteredOptions.isNotEmpty()) expanded = true }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Item") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = name, onValueChange = { name = it; expanded = true }, label = { Text("Item Name") }, modifier = Modifier.menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                if (filteredOptions.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        filteredOptions.forEach { item ->
                            DropdownMenuItem(text = { Text("${item.name} (₹${String.format(Locale.getDefault(), "%,.2f", item.cost)})") }, onClick = { name = item.name; cost = item.cost.toString(); expanded = false })
                        }
                    }
                }
            }
            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }, confirmButton = { Button(onClick = { onConfirm(name, cost.toDoubleOrNull() ?: 0.0) }, enabled = name.isNotBlank() && cost.isNotBlank()) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD ALBUM DIALOG — Bug-11 fixed
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumDialog(settings: AlbumSettings, onDismiss: () -> Unit, onConfirm: (Item) -> Unit) {
    var name     by remember { mutableStateOf("") }
    var sheets   by remember { mutableStateOf(settings.defaultSheets.toString()) }
    var quantity by remember { mutableStateOf("1") }

    // BUG-11 FIX: coerceAtLeast(1) guards against blank input
    val sheetCount   = sheets.toIntOrNull()?.coerceAtLeast(1) ?: settings.defaultSheets
    val qty          = quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val extraSheets  = (sheetCount - settings.defaultSheets).coerceAtLeast(0)
    val unitCost     = settings.baseCost + (extraSheets * settings.extraSheetCost)
    val totalCost    = unitCost * qty

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Album") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Album Name") }, singleLine = true)
            OutlinedTextField(value = sheets, onValueChange = { sheets = it }, label = { Text("Number of Sheets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Number of Albums") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            HorizontalDivider()
            Text("Base Cost (${settings.defaultSheets} sheets): ₹${String.format(Locale.getDefault(), "%,.2f", settings.baseCost)}", style = MaterialTheme.typography.bodySmall)
            if (extraSheets > 0) Text("Extra Sheets ($extraSheets): +₹${String.format(Locale.getDefault(), "%,.2f", extraSheets * settings.extraSheetCost)}", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Unit Cost:", fontWeight = FontWeight.Bold)
                Text("₹${String.format(Locale.getDefault(), "%.2f", unitCost)}", fontWeight = FontWeight.Bold)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), Arrangement.SpaceBetween) {
                    Text("Total ($qty albums):", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("₹${String.format(Locale.getDefault(), "%.2f", totalCost)}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }, confirmButton = {
        Button(onClick = { onConfirm(Item(System.currentTimeMillis(), "${name.ifBlank { "Album" }} ($sheetCount sheets)", unitCost, qty)) }, enabled = sheetCount > 0 && qty > 0) { Text("Add") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD PAYMENT DIALOG — Bug-8 fixed (UTC dates)
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String) -> Unit) {
    var title          by remember { mutableStateOf("Advance") }
    var amount         by remember { mutableStateOf("") }
    var date           by remember { mutableStateOf(formatDateFromMillis(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = formatDateFromMillis(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add Payment") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Payment For") })
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = date, onValueChange = {}, label = { Text("Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Pick Date", modifier = Modifier.clickable { showDatePicker = true }) })
        }
    }, confirmButton = { Button(onClick = { onConfirm(title, amount.toDoubleOrNull() ?: 0.0, date) }, enabled = title.isNotBlank() && amount.isNotBlank()) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// EDIT PAYMENT DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPaymentDialog(payment: Payment, onDismiss: () -> Unit, onSave: (Payment) -> Unit, onDelete: (Payment) -> Unit) {
    var title          by remember { mutableStateOf(payment.title) }
    var amountText     by remember { mutableStateOf(payment.amount.toString()) }
    var date           by remember { mutableStateOf(payment.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialMillis  = remember { parseDateToUtcMillis(payment.date) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = formatDateFromMillis(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Payment") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Payment For") })
            OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = date, onValueChange = {}, label = { Text("Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Pick Date", modifier = Modifier.clickable { showDatePicker = true }) })
        }
    }, confirmButton = {
        Row {
            Button(onClick = { onDelete(payment) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { onSave(payment.copy(title = title, amount = amountText.toDoubleOrNull() ?: payment.amount, date = date)) }, enabled = title.isNotBlank() && amountText.toDoubleOrNull() != null) { Text("Save") }
        }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD/EDIT DISCOUNT DIALOG
// ─────────────────────────────────────────────────────────────────

@Composable
fun AddEditDiscountDialog(currentDiscount: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var discount by remember { mutableStateOf(currentDiscount.toString()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Discount") }, text = {
        OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Discount Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
    }, confirmButton = { Button(onClick = { onConfirm(discount.toDoubleOrNull() ?: 0.0) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD EVENT DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var title          by remember { mutableStateOf("") }
    var customerName   by remember { mutableStateOf("") }
    var customerPhone  by remember { mutableStateOf("") }
    var date           by remember { mutableStateOf(formatDateFromMillis(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = formatDateFromMillis(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Event") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") })
            OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") })
            OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it }, label = { Text("Customer Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            OutlinedTextField(value = date, onValueChange = {}, label = { Text("Event Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Pick Date", modifier = Modifier.clickable { showDatePicker = true }) })
        }
    }, confirmButton = { Button(onClick = { onConfirm(title, date, customerName, customerPhone) }, enabled = title.isNotBlank() && customerName.isNotBlank()) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// EDIT EVENT DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(event: Event, onDismiss: () -> Unit, onConfirm: (String, String, String, String) -> Unit) {
    var title          by remember { mutableStateOf(event.title) }
    var customerName   by remember { mutableStateOf(event.customerName) }
    var customerPhone  by remember { mutableStateOf(event.customerPhone) }
    var date           by remember { mutableStateOf(event.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialMillis  = remember { parseDateToUtcMillis(event.date) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = formatDateFromMillis(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Event") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") })
            OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") })
            OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it }, label = { Text("Customer Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            OutlinedTextField(value = date, onValueChange = {}, label = { Text("Event Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Pick Date", modifier = Modifier.clickable { showDatePicker = true }) })
        }
    }, confirmButton = { Button(onClick = { onConfirm(title, date, customerName, customerPhone) }, enabled = title.isNotBlank() && customerName.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD DAY DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDayDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title          by remember { mutableStateOf("") }
    var date           by remember { mutableStateOf(formatDateFromMillis(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = formatDateFromMillis(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Day") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Day Title") })
            OutlinedTextField(value = date, onValueChange = {}, label = { Text("Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Pick Date", modifier = Modifier.clickable { showDatePicker = true }) })
        }
    }, confirmButton = { Button(onClick = { onConfirm(title, date) }, enabled = title.isNotBlank()) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// EDIT DAY DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayDialog(day: Day, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var title          by remember { mutableStateOf(day.title) }
    var date           by remember { mutableStateOf(day.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialMillis  = remember { parseDateToUtcMillis(day.date) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = formatDateFromMillis(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Edit Day") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Day Title") })
            OutlinedTextField(value = date, onValueChange = {}, label = { Text("Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Pick Date", modifier = Modifier.clickable { showDatePicker = true }) })
        }
    }, confirmButton = { Button(onClick = { onConfirm(title, date) }, enabled = title.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// CONFIRM DELETE DIALOG
// ─────────────────────────────────────────────────────────────────

@Composable
fun ConfirmDeleteDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD/EDIT TAKER DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTakerDialog(
    taker: Taker,
    masterTakers: List<MasterTaker> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Taker) -> Unit,
    onUpdateMasterTaker: ((MasterTaker) -> Unit)? = null
) {
    var name        by remember { mutableStateOf(taker.name) }
    var totalDue    by remember { mutableStateOf(if (taker.totalDue == 0.0) "" else taker.totalDue.toString()) }
    var amountPaid  by remember { mutableStateOf(if (taker.amountPaid == 0.0) "" else taker.amountPaid.toString()) }
    var newPhone    by remember { mutableStateOf("") }
    var expanded    by remember { mutableStateOf(false) }
    val context     = LocalContext.current

    val currentMasterTaker   = remember(name, masterTakers) { masterTakers.find { it.name.equals(name, true) } }
    val filteredMasterTakers = remember(name) { masterTakers.filter { it.name.contains(name, true) } }
    val isNewTakerEntry      = taker.name.isBlank()
    val isNewMasterTaker     = currentMasterTaker == null && name.isNotBlank()

    LaunchedEffect(name) {
        val selected = masterTakers.find { it.name == name }
        if (selected != null && taker.name.isBlank()) totalDue = selected.defaultTotalDue.toString()
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (taker.name.isBlank()) "Add Taker" else "Edit Taker") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = name, onValueChange = { name = it; expanded = true }, label = { Text("Taker Name") }, modifier = Modifier.menuAnchor(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
                if (filteredMasterTakers.isNotEmpty()) {
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        filteredMasterTakers.forEach { mt ->
                            DropdownMenuItem(text = { Text("${mt.name} (Due: ₹${mt.defaultTotalDue})") }, onClick = { name = mt.name; totalDue = mt.defaultTotalDue.toString(); expanded = false })
                        }
                    }
                }
            }
            OutlinedTextField(value = totalDue, onValueChange = { totalDue = it }, label = { Text("Total Due (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = amountPaid, onValueChange = { amountPaid = it }, label = { Text("Amount Paid (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            // BUG-FIX: auto-add to master list — show phone field for brand-new takers
            if (onUpdateMasterTaker != null && isNewTakerEntry && isNewMasterTaker) {
                HorizontalDivider()
                Text("New taker — will be saved to Master List", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = newPhone,
                    onValueChange = { newPhone = it.filter { c -> c.isDigit() || c == '+' || c.isWhitespace() } },
                    label = { Text("Phone (e.g. +91 9876543210)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }, confirmButton = {
        Button(onClick = {
            val savedTaker = taker.copy(name = name, totalDue = totalDue.toDoubleOrNull() ?: 0.0, amountPaid = amountPaid.toDoubleOrNull() ?: 0.0)
            if (onUpdateMasterTaker != null) {
                if (isNewTakerEntry && isNewMasterTaker) {
                    // Auto-add brand-new taker to master list with phone
                    val phone = newPhone.filter { it.isDigit() || it == '+' }
                    onUpdateMasterTaker(MasterTaker(System.currentTimeMillis(), name, totalDue.toDoubleOrNull() ?: 0.0, phone))
                    Toast.makeText(context, "$name added to Master List", Toast.LENGTH_SHORT).show()
                } else if (currentMasterTaker != null) {
                    // Silently sync default due if it changed
                    val due = totalDue.toDoubleOrNull() ?: 0.0
                    if (due != currentMasterTaker.defaultTotalDue) onUpdateMasterTaker(currentMasterTaker.copy(defaultTotalDue = due))
                }
            }
            onConfirm(savedTaker)
        }, enabled = name.isNotBlank()) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD TAKER TO EVENT DIALOG
// ─────────────────────────────────────────────────────────────────

@Composable
fun AddTakerToEventDialog(event: Event, onDismiss: () -> Unit, onConfirm: (String, Double, Double, List<Long>) -> Unit) {
    var name          by remember { mutableStateOf("") }
    var totalDue      by remember { mutableStateOf("") }
    var amountPaid    by remember { mutableStateOf("") }
    val selectedDayIds = remember { mutableStateListOf<Long>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Add Taker to Event", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Taker Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = totalDue, onValueChange = { totalDue = it }, label = { Text("Total Due (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = amountPaid, onValueChange = { amountPaid = it }, label = { Text("Amount Paid (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Text("Assign to Days:", style = MaterialTheme.typography.titleSmall)
                Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 200.dp)) {
                    event.days.forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { if (day.id in selectedDayIds) selectedDayIds.remove(day.id) else selectedDayIds.add(day.id) }.padding(vertical = 4.dp)) {
                            Checkbox(checked = day.id in selectedDayIds, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                            Text("${day.title} (${day.date})", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(name, totalDue.toDoubleOrNull() ?: 0.0, amountPaid.toDoubleOrNull() ?: 0.0, selectedDayIds) }, enabled = name.isNotBlank()) { Text("Add") }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// ADD/EDIT MASTER ITEM DIALOG
// ─────────────────────────────────────────────────────────────────

@Composable
fun AddEditMasterItemDialog(item: MasterItem? = null, onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var cost by remember { mutableStateOf(item?.cost?.toString() ?: "") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (item == null) "Add Master Item" else "Edit Master Item") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
            OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Item Cost (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
    }, confirmButton = { Button(onClick = { onConfirm(name, cost.toDoubleOrNull() ?: 0.0) }, enabled = name.isNotBlank() && cost.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD/EDIT MASTER TAKER DIALOG
// ─────────────────────────────────────────────────────────────────

@Composable
fun AddEditMasterTakerDialog(taker: MasterTaker? = null, onDismiss: () -> Unit, onConfirm: (String, Double, String) -> Unit) {
    var name           by remember { mutableStateOf(taker?.name ?: "") }
    var defaultTotalDue by remember { mutableStateOf(if (taker?.defaultTotalDue == 0.0 || taker == null) "" else taker.defaultTotalDue.toString()) }
    var defaultPhone   by remember { mutableStateOf(taker?.defaultPhone ?: "") }
    val context = LocalContext.current

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (taker == null) "Add Master Taker" else "Edit Master Taker") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Taker Name") })
            OutlinedTextField(value = defaultTotalDue, onValueChange = { defaultTotalDue = it }, label = { Text("Default Total Due (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = defaultPhone, onValueChange = { defaultPhone = it.filter { c -> c.isDigit() || c == '+' || c.isWhitespace() } }, label = { Text("Phone (e.g. +91 9876543210)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            if (defaultPhone.isNotBlank() && !defaultPhone.trimStart().startsWith('+'))
                Text("Include country code, e.g. +91…", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = {
        Button(onClick = {
            val formatted = defaultPhone.filter { it.isDigit() || it == '+' }
            if (formatted.isNotBlank() && !formatted.startsWith('+')) { Toast.makeText(context, "Please include country code starting with '+'", Toast.LENGTH_LONG).show(); return@Button }
            onConfirm(name, defaultTotalDue.toDoubleOrNull() ?: 0.0, formatted)
        }, enabled = name.isNotBlank()) { Text("Save") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

// ─────────────────────────────────────────────────────────────────
// ADD/EDIT INTERNAL EXPENSE DIALOG
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInternalExpenseDialog(expense: InternalExpense? = null, onDismiss: () -> Unit, onConfirm: (InternalExpense) -> Unit) {
    val isEditing      = expense != null
    var title          by remember { mutableStateOf(expense?.title ?: "") }
    var amountText     by remember { mutableStateOf(if (isEditing) expense!!.amount.toString() else "") }
    var date           by remember { mutableStateOf(expense?.date ?: formatDateFromMillis(System.currentTimeMillis())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialMillis  = remember { if (expense != null) parseDateToUtcMillis(expense.date) else System.currentTimeMillis() }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    if (showDatePicker) {
        DatePickerDialog(onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = formatDateFromMillis(it) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isEditing) "Edit Expense" else "Add Internal Expense") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title") })
            OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = date, onValueChange = {}, label = { Text("Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Pick Date", modifier = Modifier.clickable { showDatePicker = true }) })
            Text("This expense will NOT appear on the client's bill.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }, confirmButton = {
        Button(onClick = {
            val amount = amountText.toDoubleOrNull() ?: 0.0
            onConfirm(expense?.copy(title = title, amount = amount, date = date) ?: InternalExpense(System.currentTimeMillis(), title, amount, date))
        }, enabled = title.isNotBlank() && amountText.isNotBlank()) { Text(if (isEditing) "Save" else "Add") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
