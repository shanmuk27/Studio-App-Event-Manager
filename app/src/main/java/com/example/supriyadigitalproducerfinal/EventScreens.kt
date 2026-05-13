package com.example.supriyadigitalproducerfinal

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.PhotoAlbum
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.example.supriyadigitalproducerfinal.ui.theme.BalanceDue
import com.example.supriyadigitalproducerfinal.ui.theme.BalancePaid
import com.example.supriyadigitalproducerfinal.ui.theme.BalancePartial
import com.example.supriyadigitalproducerfinal.ui.theme.PendingTint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Card
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color as ComposeColor

// ─────────────────────────────────────────────────────────────────
// EVENT LIST SCREEN
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventListScreen(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onAddEvent: (String, String, String, String) -> Unit,
    onEditEvent: (Long, String, String, String, String) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onRefresh: () -> Unit
) {
    var showAddEventDialog  by remember { mutableStateOf(false) }
    var showFilterDialog    by remember { mutableStateOf(false) }
    var showCalendarDialog  by remember { mutableStateOf(false) }
    var searchQuery         by remember { mutableStateOf("") }
    var sortOrder           by remember { mutableStateOf(SortOrder.NONE) }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) { onRefresh(); delay(1000); pullRefreshState.endRefresh() }
    }

    var selectedYear  by remember { mutableStateOf("Year") }
    var selectedMonth by remember { mutableStateOf("Month") }

    val yearList = remember(events.toList()) {
        listOf("Year") + events.map { it.date.takeLast(4) }.distinct().sorted()
    }
    val monthList = listOf("Month","January","February","March","April","May","June",
        "July","August","September","October","November","December")
    val monthNumberMap = monthList.mapIndexedNotNull { i, m ->
        if (i > 0) m to String.format("%02d", i) else null }.toMap()

    val displayedEvents = remember(events.toList(), searchQuery, sortOrder, selectedYear, selectedMonth) {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        events.filter { e ->
            (searchQuery.isBlank() || e.title.contains(searchQuery, true) ||
                    e.customerName.contains(searchQuery, true) || e.date.contains(searchQuery, true)) &&
                    (selectedYear == "Year" || e.date.endsWith(selectedYear)) &&
                    (selectedYear == "Year" || selectedMonth == "Month" ||
                            e.date.substring(3, 5) == monthNumberMap[selectedMonth]) &&
                    // FIX: confirmed/unconfirmed filter
                    when (sortOrder) {
                        SortOrder.CONFIRMED_ONLY   -> e.isConfirmed
                        SortOrder.UNCONFIRMED_ONLY -> !e.isConfirmed
                        else -> true
                    }
        }.let { filtered ->
            when (sortOrder) {
                SortOrder.ASCENDING  -> filtered.sortedBy    { try { sdf.parse(it.date) } catch (_: Exception) { Date(Long.MAX_VALUE) } }
                SortOrder.DESCENDING -> filtered.sortedByDescending { try { sdf.parse(it.date) } catch (_: Exception) { Date(0) } }
                else                 -> filtered
            }
        }
    }

    // FIX: Balance Due only counts confirmed events
    val totalBalance = events.filter { it.isConfirmed }.sumOf { e ->
        e.eventItems.sumOf { it.cost * it.quantity } +
                e.days.sumOf { d -> d.items.sumOf { it.cost * it.quantity } } -
                e.discount - e.payments.sumOf { it.amount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showCalendarDialog = true }) { Icon(Icons.Outlined.CalendarMonth, "Calendar") }
                    IconButton(onClick = {
                        sortOrder = when (sortOrder) {
                            SortOrder.NONE             -> SortOrder.ASCENDING
                            SortOrder.ASCENDING        -> SortOrder.DESCENDING
                            SortOrder.DESCENDING       -> SortOrder.CONFIRMED_ONLY
                            SortOrder.CONFIRMED_ONLY   -> SortOrder.UNCONFIRMED_ONLY
                            SortOrder.UNCONFIRMED_ONLY -> SortOrder.NONE
                        }
                        Toast.makeText(context, when (sortOrder) {
                            SortOrder.ASCENDING        -> "Oldest first"
                            SortOrder.DESCENDING       -> "Newest first"
                            SortOrder.CONFIRMED_ONLY   -> "Confirmed events only"
                            SortOrder.UNCONFIRMED_ONLY -> "Unconfirmed events only"
                            SortOrder.NONE             -> "Sort cleared"
                        }, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            when (sortOrder) {
                                SortOrder.CONFIRMED_ONLY   -> Icons.Filled.CheckCircle
                                SortOrder.UNCONFIRMED_ONLY -> Icons.Outlined.Info
                                else                       -> Icons.AutoMirrored.Filled.Sort
                            },
                            "Sort / Filter",
                            tint = when (sortOrder) {
                                SortOrder.CONFIRMED_ONLY   -> BalancePaid
                                SortOrder.UNCONFIRMED_ONLY -> BalanceDue
                                else                       -> LocalContentColor.current
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddEventDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("New Event") },
                expanded = !listState.isScrollInProgress
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).nestedScroll(pullRefreshState.nestedScrollConnection)) {
            LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 88.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard("Total Events", "${events.size}", Icons.Filled.Event, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatCard("Balance Due", "₹${String.format(Locale.getDefault(), "%,.0f", totalBalance)}", Icons.Filled.AccountBalance, if (totalBalance > 0) BalanceDue else BalancePaid, Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StyledSearchBar(value = searchQuery, onValueChange = { searchQuery = it }, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showFilterDialog = true }, modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Icon(Icons.Default.FilterList, "Filter")
                        }
                    }
                }
                if (displayedEvents.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Outlined.Event,
                            title = "No events found",
                            subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Tap 'New Event' to create your first event",
                            modifier = Modifier.fillParentMaxWidth().padding(top = 40.dp)
                        )
                    }
                }
                items(displayedEvents, key = { it.id }) { event ->
                    EventListItem(event, onEventClick, onEditEvent, onDeleteEvent, Modifier.animateItemPlacement(tween(300)))
                }
            }
            PullToRefreshContainer(state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
        }
    }

    if (showFilterDialog) FilterDialog(selectedYear, selectedMonth, yearList, monthList, onDismiss = { showFilterDialog = false }, onApply = { y, m -> selectedYear = y; selectedMonth = m; showFilterDialog = false })
    if (showAddEventDialog) AddEventDialog(onDismiss = { showAddEventDialog = false }, onConfirm = { t, d, n, p -> onAddEvent(t, d, n, p); showAddEventDialog = false })
    if (showCalendarDialog) EventCalendarDialog(events = events, onDismiss = { showCalendarDialog = false })
}

// ─────────────────────────────────────────────────────────────────
// EVENT LIST ITEM
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventListItem(event: Event, onClick: (Event) -> Unit, onEditEvent: (Long, String, String, String, String) -> Unit, onDeleteEvent: (Long) -> Unit, modifier: Modifier = Modifier) {
    var showActions     by remember { mutableStateOf(false) }
    val showEditDialog  = remember { mutableStateOf(false) }
    val showDeleteConfirm = remember { mutableStateOf(false) }
    val context = LocalContext.current

    val totalCost = event.eventItems.sumOf { it.cost * it.quantity } + event.days.sumOf { d -> d.items.sumOf { it.cost * it.quantity } }
    val totalPaid = event.payments.sumOf { it.amount }
    val balance   = totalCost - event.discount - totalPaid
    val accentColor = when { balance <= 0 -> BalancePaid; balance < totalCost * 0.5 -> BalancePartial; else -> BalanceDue }

    ElevatedCard(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(onClick = { if (showActions) showActions = false else onClick(event) }, onLongClick = { showActions = true }),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Row {
            Box(Modifier.width(5.dp).fillMaxHeight().background(accentColor))
            Column(Modifier.weight(1f).padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    InitialsAvatar(name = event.customerName.ifBlank { event.title }, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            Spacer(Modifier.width(6.dp))
                            if (event.isConfirmed) StatusChip("Confirmed", BalancePaid)
                            else StatusChip("Pending", PendingTint, ComposeColor(0xFF4E2600))
                        }
                        Text(event.customerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarToday, null, Modifier.size(13.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(event.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (event.customerPhone.isNotBlank()) {
                            Spacer(Modifier.width(8.dp))
                            IconButton(onClick = {
                                try { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${event.customerPhone}".toUri())) }
                                catch (_: Exception) { Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show() }
                            }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Phone, "Call", Modifier.size(15.dp), MaterialTheme.colorScheme.tertiary)
                            }
                        }
                    }
                    Text("₹${String.format(Locale.getDefault(), "%,.0f", balance)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
                }
                AnimatedVisibility(visible = showActions) {
                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                        TextButton(onClick = { showEditDialog.value = true; showActions = false }) { Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Edit") }
                        TextButton(onClick = { showDeleteConfirm.value = true; showActions = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                            Icon(Icons.Outlined.Delete, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Delete")
                        }
                    }
                }
            }
        }
    }
    if (showEditDialog.value) EditEventDialog(event, onDismiss = { showEditDialog.value = false }, onConfirm = { t, d, n, p -> onEditEvent(event.id, t, d, n, p); showEditDialog.value = false })
    if (showDeleteConfirm.value) ConfirmDeleteDialog("Move to Trash", "Move '${event.title}' to the Trash Bin?", onDismiss = { showDeleteConfirm.value = false }, onConfirm = { onDeleteEvent(event.id); showDeleteConfirm.value = false })
}

// ─────────────────────────────────────────────────────────────────
// CALENDAR DIALOG — Bug-12 Fixed: no onRefresh on navigation
// ─────────────────────────────────────────────────────────────────

@Composable
fun EventCalendarDialog(events: List<Event>, onDismiss: () -> Unit) {
    var calendarInstance by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        })
    }
    val sdf           = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val monthYearFmt  = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val eventDatesMap = remember(events) {
        mutableMapOf<String, MutableList<String>>().also { map ->
            events.forEach { event ->
                val dn = if (event.customerName.isNotBlank()) "${event.customerName} – ${event.title}" else event.title
                map.getOrPut(event.date) { mutableListOf() }.add(dn)
                event.days.forEach { day -> map.getOrPut(day.date) { mutableListOf() }.add("$dn (${day.title})") }
            }
        }
    }
    var selectedDateEvents by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDateString  by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row {
                        IconButton(onClick = { calendarInstance = (calendarInstance.clone() as Calendar).also { it.add(Calendar.YEAR, -1) } }, Modifier.size(36.dp)) { Icon(Icons.Default.KeyboardDoubleArrowLeft, "Prev Year") }
                        IconButton(onClick = { calendarInstance = (calendarInstance.clone() as Calendar).also { it.add(Calendar.MONTH, -1) } }, Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Prev Month") }
                    }
                    Text(monthYearFmt.format(calendarInstance.time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    Row {
                        IconButton(onClick = { calendarInstance = (calendarInstance.clone() as Calendar).also { it.add(Calendar.MONTH, 1) } }, Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Month") }
                        IconButton(onClick = { calendarInstance = (calendarInstance.clone() as Calendar).also { it.add(Calendar.YEAR, 1) } }, Modifier.size(36.dp)) { Icon(Icons.Default.KeyboardDoubleArrowRight, "Next Year") }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), Arrangement.SpaceEvenly) {
                    listOf("S","M","T","W","T","F","S").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(8.dp))
                val daysInMonth   = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfWeek = calendarInstance.get(Calendar.DAY_OF_WEEK) - 1
                val rows = kotlin.math.ceil((daysInMonth + firstDayOfWeek) / 7.0).toInt()
                var dayCounter = 1
                Column {
                    for (i in 0 until rows) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                            for (j in 0..6) {
                                if (i == 0 && j < firstDayOfWeek) { Box(Modifier.weight(1f).aspectRatio(1f)) }
                                else if (dayCounter <= daysInMonth) {
                                    val day = dayCounter
                                    val cellCal = (calendarInstance.clone() as Calendar).also { it.set(Calendar.DAY_OF_MONTH, day) }
                                    val dateStr  = sdf.format(cellCal.time)
                                    val hasEvent = eventDatesMap.containsKey(dateStr)
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp).clip(CircleShape).background(if (hasEvent) MaterialTheme.colorScheme.primaryContainer else ComposeColor.Transparent).clickable { selectedDateString = dateStr; selectedDateEvents = eventDatesMap[dateStr] ?: emptyList() }, contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(day.toString(), style = MaterialTheme.typography.bodySmall, color = if (hasEvent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, fontWeight = if (hasEvent) FontWeight.Bold else FontWeight.Normal)
                                            if (hasEvent) Box(Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                        }
                                    }
                                    dayCounter++
                                } else { Box(Modifier.weight(1f).aspectRatio(1f)); dayCounter++ }
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = selectedDateString.isNotEmpty()) {
                    Column {
                        HorizontalDivider(Modifier.padding(vertical = 12.dp))
                        if (selectedDateEvents.isNotEmpty()) {
                            Text("Events on $selectedDateString", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(4.dp))
                            selectedDateEvents.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 2.dp)) }
                        } else {
                            Text("No events on $selectedDateString.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// EVENT DETAIL SCREEN
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: Event, albumSettings: AlbumSettings, itemList: List<MasterItem>,
    onUpdatePayments: (List<Payment>) -> Unit, onUpdateDiscount: (Double) -> Unit,
    onAddDay: (String, String) -> Unit, onEditDay: (Long, String, String) -> Unit, onDeleteDay: (Long) -> Unit,
    onAddEventItem: (Item) -> Unit, onAddDayItem: (Long, String, Double, Int) -> Unit,
    onEditItem: (Long, Long?, String, Double) -> Unit, onUpdateItemQuantity: (Long, Long?, Int) -> Unit, onDeleteItem: (Long, Long?) -> Unit,
    onBack: () -> Unit, onManageTakersForDay: (Long) -> Unit, onManageAllTakers: () -> Unit,
    onShowInternalExpenses: () -> Unit, onUpdateEventStatus: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showAddDayDialog     by remember { mutableStateOf(false) }
    var showAddAlbumDialog   by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var editingPayment       by remember { mutableStateOf<Payment?>(null) }
    var showAddItemDialog    by remember { mutableStateOf(false) }
    var showDiscountDialog   by remember { mutableStateOf(false) }
    var showDownloadDialog   by remember { mutableStateOf(false) }
    var isEditing            by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val sortedDays = remember(event.days.toList()) {
        event.days.sortedBy { day -> try { sdf.parse(day.date) } catch (_: Exception) { Date(Long.MAX_VALUE) } }
    }

    val totalCost        = event.eventItems.sumOf { it.cost * it.quantity } + event.days.sumOf { d -> d.items.sumOf { it.cost * it.quantity } }
    val totalPaid        = event.payments.sumOf { it.amount }
    val remainingBalance = totalCost - event.discount - totalPaid

    val handlePaymentUpdate: (Payment?, Boolean) -> Unit = { updatedPayment, isDelete ->
        val payments = event.payments.toMutableList()
        if (isDelete && updatedPayment != null) payments.removeIf { it.id == updatedPayment.id }
        else if (updatedPayment != null) { val idx = payments.indexOfFirst { it.id == updatedPayment.id }; if (idx != -1) payments[idx] = updatedPayment }
        onUpdatePayments(payments)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(event.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (event.isConfirmed) { Spacer(Modifier.width(6.dp)); Icon(Icons.Filled.CheckCircle, "Confirmed", tint = BalancePaid, modifier = Modifier.size(16.dp)) }
                        }
                        Text("Balance: ₹${String.format(Locale.getDefault(), "%,.2f", remainingBalance)}", style = MaterialTheme.typography.labelSmall, color = if (remainingBalance > 0) MaterialTheme.colorScheme.error else BalancePaid)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    // CHANGE 5: Unconfirm toggle — tap to toggle confirmed/unconfirmed
                    IconButton(onClick = { onUpdateEventStatus(!event.isConfirmed) }) {
                        Icon(
                            if (event.isConfirmed) Icons.Filled.CheckCircle else Icons.Outlined.Info,
                            contentDescription = if (event.isConfirmed) "Mark Unconfirmed" else "Mark Confirmed",
                            tint = if (event.isConfirmed) BalancePaid else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onShowInternalExpenses) { Icon(Icons.Filled.AccountTree, "Internal Expenses") }
                    if (!isEditing) IconButton(onClick = { showDownloadDialog = true }) { Icon(Icons.Filled.Download, "Download") }
                    IconButton(onClick = onManageAllTakers) { Icon(Icons.Filled.Groups, "All Takers") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { isEditing = !isEditing }, containerColor = if (isEditing) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary) {
                AnimatedContent(isEditing, label = "fabIcon") { editing ->
                    Icon(if (editing) Icons.Filled.Done else Icons.Filled.Edit, if (editing) "Done" else "Edit")
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (sortedDays.isEmpty() && event.eventItems.isEmpty() && !isEditing) {
                item { EmptyState(Icons.Outlined.Info, "No details yet", "Tap the pencil button to add days, items and payments.", modifier = Modifier.fillParentMaxWidth().padding(vertical = 40.dp)) }
            }
            if (isEditing) { item { EditActionsRow(onAddPaymentClick = { showAddPaymentDialog = true }, onAddAlbumClick = { showAddAlbumDialog = true }, onAddDayClick = { showAddDayDialog = true }, onAddItemClick = { showAddItemDialog = true }) } }
            if (event.payments.isNotEmpty() || event.discount > 0 || isEditing) {
                item { FinancialSummaryCard(event.payments, totalCost, event.discount, isEditing, onEditDiscountClick = { showDiscountDialog = true }, onEditPaymentClick = { editingPayment = it }) }
            }
            items(sortedDays, key = { it.id }) { day ->
                DayItemCard(day, isEditing, itemList, onEditDay, onDeleteDay,
                    onAddItem = { name, cost, qty -> onAddDayItem(day.id, name, cost, qty) },
                    onEditItem = { itemId, newName, newCost -> onEditItem(itemId, day.id, newName, newCost) },
                    onUpdateItemQuantity = { itemId, newQty -> onUpdateItemQuantity(itemId, day.id, newQty) },
                    onDeleteItem = { itemId -> onDeleteItem(itemId, day.id) },
                    onManageTakers = { onManageTakersForDay(day.id) })
            }
            item { EventItemsCard(event.eventItems, isEditing, onEditItem = { id, n, c -> onEditItem(id, null, n, c) }, onUpdateItemQuantity = { id, q -> onUpdateItemQuantity(id, null, q) }, onDeleteItem = { id -> onDeleteItem(id, null) }) }
        }
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Select Document Type") },
            text = { Text("Generate a formal Invoice (confirms the booking) or a Bill Analysis (estimate)?") },
            confirmButton = {
                Button(onClick = { createEventBillPdf(context, event, true); onUpdateEventStatus(true); showDownloadDialog = false }) { Text("Invoice (Confirm)") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDownloadDialog = false }) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { createEventBillPdf(context, event, false); showDownloadDialog = false }) { Text("Bill Analysis") }
                }
            }
        )
    }
    if (showAddDayDialog)    AddDayDialog(onDismiss = { showAddDayDialog = false }, onConfirm = { t, d -> onAddDay(t, d); showAddDayDialog = false })
    if (showAddAlbumDialog)  AddAlbumDialog(settings = albumSettings, onDismiss = { showAddAlbumDialog = false }, onConfirm = { item -> onAddEventItem(item); showAddAlbumDialog = false })
    if (showAddPaymentDialog) {
        AddPaymentDialog(onDismiss = { showAddPaymentDialog = false }, onConfirm = { title, amount, date ->
            onUpdatePayments(event.payments.toMutableList().apply { add(Payment(System.currentTimeMillis(), title, amount, date)) })
            if (amount > 0 && !event.isConfirmed) { onUpdateEventStatus(true); Toast.makeText(context, "Payment added! Event marked as Confirmed.", Toast.LENGTH_SHORT).show() }
            showAddPaymentDialog = false
        })
    }
    if (editingPayment != null) EditPaymentDialog(editingPayment!!, onDismiss = { editingPayment = null }, onSave = { p -> handlePaymentUpdate(p, false); editingPayment = null }, onDelete = { p -> handlePaymentUpdate(p, true); editingPayment = null })
    if (showAddItemDialog)   AddItemDialog(itemList, onDismiss = { showAddItemDialog = false }, onConfirm = { n, c -> onAddEventItem(Item(System.currentTimeMillis(), n, c, 1)); showAddItemDialog = false })
    if (showDiscountDialog)  AddEditDiscountDialog(event.discount, onDismiss = { showDiscountDialog = false }, onConfirm = { d -> onUpdateDiscount(d); showDiscountDialog = false })
}

// ─────────────────────────────────────────────────────────────────
// FINANCIAL SUMMARY CARD
// ─────────────────────────────────────────────────────────────────

@Composable
fun FinancialSummaryCard(payments: List<Payment>, totalCost: Double, discount: Double, isEditing: Boolean, onEditDiscountClick: () -> Unit, onEditPaymentClick: (Payment) -> Unit) {
    val grandTotal = totalCost - discount
    val totalPaid  = payments.sumOf { it.amount }
    val balanceDue = grandTotal - totalPaid

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Financial Summary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            PaymentProgressBar(grandTotal, totalPaid, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()

            @Composable
            fun SummaryRow(label: String, value: String, color: ComposeColor = MaterialTheme.colorScheme.onSurface, clickable: Boolean = false, onClick: () -> Unit = {}, showEditIcon: Boolean = false) {
                Row(Modifier.fillMaxWidth().then(if (clickable) Modifier.clickable { onClick() } else Modifier).padding(vertical = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        if (showEditIcon && isEditing) { Spacer(Modifier.width(4.dp)); Icon(Icons.Outlined.Edit, null, Modifier.size(14.dp), MaterialTheme.colorScheme.primary) }
                    }
                    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
                }
            }

            SummaryRow("Sub-Total", "₹${String.format(Locale.getDefault(), "%,.2f", totalCost)}")
            SummaryRow("Discount", "– ₹${String.format(Locale.getDefault(), "%,.2f", discount)}", color = if (discount > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface, clickable = isEditing, onClick = onEditDiscountClick, showEditIcon = true)
            SummaryRow("Grand Total", "₹${String.format(Locale.getDefault(), "%,.2f", grandTotal)}", MaterialTheme.colorScheme.primary)

            if (payments.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Text("Payments Received", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                payments.forEach { payment ->
                    Row(Modifier.fillMaxWidth().then(if (isEditing) Modifier.clickable { onEditPaymentClick(payment) } else Modifier).padding(vertical = 6.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Column { Text(payment.title, style = MaterialTheme.typography.bodyMedium); Text(payment.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Text("– ₹${String.format(Locale.getDefault(), "%,.2f", payment.amount)}", style = MaterialTheme.typography.bodyMedium, color = BalancePaid, fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(Modifier.padding(top = 8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = if (balanceDue > 0) MaterialTheme.colorScheme.errorContainer else ComposeColor(0xFFE8F5E9), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("Balance Due", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("₹${String.format(Locale.getDefault(), "%,.2f", balanceDue)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (balanceDue > 0) MaterialTheme.colorScheme.error else ComposeColor(0xFF2E7D32))
                }
            }
        }
    }
}

@Composable
fun EditActionsRow(onAddPaymentClick: () -> Unit, onAddAlbumClick: () -> Unit, onAddDayClick: () -> Unit, onAddItemClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onAddDayClick, Modifier.weight(1f)) { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add Day") }
            FilledTonalButton(onClick = onAddItemClick, Modifier.weight(1f)) { Icon(Icons.Outlined.AddCircle, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add Item") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onAddAlbumClick, Modifier.weight(1f)) { Icon(Icons.Outlined.PhotoAlbum, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add Album") }
            FilledTonalButton(onClick = onAddPaymentClick, Modifier.weight(1f)) { Icon(Icons.Outlined.Payments, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Add Payment") }
        }
    }
}

@Composable
fun EventItemsCard(eventItems: List<Item>, isEditing: Boolean, onEditItem: (Long, String, Double) -> Unit, onUpdateItemQuantity: (Long, Int) -> Unit, onDeleteItem: (Long) -> Unit) {
    if (eventItems.isNotEmpty()) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Event Items & Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                eventItems.forEach { item ->
                    key(item.id, item.quantity) {
                        ItemRow(item, isEditing, onEdit = { n, c -> onEditItem(item.id, n, c) }, onUpdateQuantity = { q -> onUpdateItemQuantity(item.id, q) }, onDelete = { onDeleteItem(item.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun DayItemCard(day: Day, isEditing: Boolean, itemList: List<MasterItem>, onEditDay: (Long, String, String) -> Unit, onDeleteDay: (Long) -> Unit, onAddItem: (String, Double, Int) -> Unit, onEditItem: (Long, String, Double) -> Unit, onUpdateItemQuantity: (Long, Int) -> Unit, onDeleteItem: (Long) -> Unit, onManageTakers: () -> Unit) {
    var showEditDayDialog   by remember { mutableStateOf(false) }
    var showDeleteDayDialog by remember { mutableStateOf(false) }
    var showAddItemDialog   by remember { mutableStateOf(false) }

    ElevatedCard(Modifier.fillMaxWidth(), elevation = CardDefaults.elevatedCardElevation(3.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(day.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.CalendarToday, null, Modifier.size(13.dp), MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.width(4.dp)); Text(day.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                if (isEditing) { Row { IconButton(onClick = { showEditDayDialog = true }) { Icon(Icons.Outlined.Edit, "Edit Day") }; IconButton(onClick = { showDeleteDayDialog = true }) { Icon(Icons.Outlined.Delete, "Delete Day", tint = MaterialTheme.colorScheme.error) } } }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (day.items.isEmpty()) { Text("No items for this day.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp)) }
            else { day.items.forEach { item -> key(item.id, item.quantity) { ItemRow(item, isEditing, onEdit = { n, c -> onEditItem(item.id, n, c) }, onUpdateQuantity = { q -> onUpdateItemQuantity(item.id, q) }, onDelete = { onDeleteItem(item.id) }) } } }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing) { Button(onClick = { showAddItemDialog = true }, Modifier.weight(1f)) { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Add Item") } }
                OutlinedButton(onClick = onManageTakers, Modifier.weight(1f)) { Icon(Icons.Filled.Groups, null, Modifier.size(ButtonDefaults.IconSize)); Spacer(Modifier.size(ButtonDefaults.IconSpacing)); Text("Takers") }
            }
        }
    }
    if (showEditDayDialog)   EditDayDialog(day, onDismiss = { showEditDayDialog = false }, onConfirm = { t, d -> onEditDay(day.id, t, d); showEditDayDialog = false })
    if (showDeleteDayDialog) ConfirmDeleteDialog("Delete Day", "Delete '${day.title}'?", onDismiss = { showDeleteDayDialog = false }, onConfirm = { onDeleteDay(day.id); showDeleteDayDialog = false })
    if (showAddItemDialog)   AddItemDialog(itemList, onDismiss = { showAddItemDialog = false }, onConfirm = { n, c -> onAddItem(n, c, 1); showAddItemDialog = false })
}

@Composable
fun ItemRow(item: Item, isEditing: Boolean, onEdit: (String, Double) -> Unit, onUpdateQuantity: (Int) -> Unit, onDelete: () -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("₹${String.format(Locale.getDefault(), "%,.2f", item.cost)} × ${item.quantity} = ₹${String.format(Locale.getDefault(), "%,.2f", item.cost * item.quantity)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onUpdateQuantity(item.quantity - 1) }, enabled = item.quantity > 1) { Icon(Icons.Filled.RemoveCircleOutline, "–", Modifier.size(20.dp)) }
                Text(item.quantity.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 20.dp))
                IconButton(onClick = { onUpdateQuantity(item.quantity + 1) }) { Icon(Icons.Filled.AddCircleOutline, "+", Modifier.size(20.dp)) }
                IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Outlined.Edit, "Edit", Modifier.size(20.dp)) }
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Delete", Modifier.size(20.dp), MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (showEditDialog) {
        var name by remember { mutableStateOf(item.name) }
        var cost by remember { mutableStateOf(item.cost.toString()) }
        AlertDialog(onDismissRequest = { showEditDialog = false }, title = { Text("Edit Item") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Item Cost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        }, confirmButton = { Button(onClick = { onEdit(name, cost.toDoubleOrNull() ?: item.cost); showEditDialog = false }) { Text("Save") } }, dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } })
    }
}

@Composable
fun TakerItemCard(taker: Taker, date: String? = null, onEditClick: () -> Unit, onDeleteClick: (() -> Unit)? = null) {
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
            IconButton(onClick = onEditClick) { Icon(Icons.Outlined.Edit, "Edit Taker") }
            onDeleteClick?.let { IconButton(onClick = it) { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) } }
        }
    }
}
