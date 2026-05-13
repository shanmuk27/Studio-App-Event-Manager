package com.example.supriyadigitalproducerfinal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import com.example.supriyadigitalproducerfinal.ui.theme.SupriyaDigitalProducerFinalTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

// ─────────────────────────────────────────────────────────────────
// MAIN ACTIVITY
// ─────────────────────────────────────────────────────────────────

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bm      = BiometricManager.from(this)
        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        setContent {
            SupriyaDigitalProducerFinalTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var isAuthenticated by remember { mutableStateOf(false) }
                    if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) ProducerApp()
                    else {
                        if (isAuthenticated) ProducerApp()
                        else LockScreen(onAuthenticate = {
                            showBiometricPrompt(
                                onSuccess = { isAuthenticated = true },
                                onError   = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                            )
                        })
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { onSuccess() }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) onError("Auth Error: $errString")
            }
            override fun onAuthenticationFailed() { onError("Authentication failed. Try again.") }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Supriya Digital Studio")
            .setSubtitle("Unlock to access producer data")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        prompt.authenticate(info)
    }
}

// ─────────────────────────────────────────────────────────────────
// LOCK SCREEN
// ─────────────────────────────────────────────────────────────────

@Composable
fun LockScreen(onAuthenticate: () -> Unit) {
    LaunchedEffect(Unit) { onAuthenticate() }
    val pulse = rememberInfiniteTransition(label = "lockPulse")
    val scale by pulse.animateFloat(1f, 1.10f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale")
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.background))
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(Modifier.size(100.dp).graphicsLayer(scaleX = scale, scaleY = scale), CircleShape, MaterialTheme.colorScheme.primary, shadowElevation = 20.dp) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Lock, "Locked", Modifier.size(50.dp), MaterialTheme.colorScheme.onPrimary) }
            }
            Spacer(Modifier.height(32.dp))
            Text("Supriya Digital Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("Authenticate to continue", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(40.dp))
            Button(onClick = onAuthenticate, modifier = Modifier.height(52.dp).widthIn(min = 200.dp)) {
                Icon(Icons.Filled.Fingerprint, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text("Unlock App", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// PRODUCER APP — 5 tabs: Events, Takers, Analytics, Alerts, Settings
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProducerApp() {
    val context = LocalContext.current
    val firestoreManager = remember { FirestoreManager() }
    val dataStore        = remember { DataStore(context) }
    val notifStore       = remember { NotificationSettingsStore(context) }
    val scope            = rememberCoroutineScope()

    var syncStatus  by remember { mutableIntStateOf(0) }
    var events      by remember { mutableStateOf<List<Event>>(emptyList()) }
    var deletedEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var itemList    by remember { mutableStateOf<List<MasterItem>>(emptyList()) }
    var masterTakers by remember { mutableStateOf<List<MasterTaker>>(emptyList()) }
    val albumSettings = remember { mutableStateOf(dataStore.loadAlbumSettings()) }

    // Unread error count for Alerts badge
    var unreadAlertCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        unreadAlertCount = notifStore.loadAlerts().count { it.type == AlertType.ERROR || it.type == AlertType.WARNING }
    }

    // Live Firestore streams
    LaunchedEffect(Unit) { firestoreManager.getEventsFlow().collect { events = it } }
    LaunchedEffect(Unit) { firestoreManager.getDeletedEventsFlow().collect { deletedEvents = it } }
    LaunchedEffect(Unit) { firestoreManager.getMasterItemsFlow().collect { itemList = it } }
    LaunchedEffect(Unit) { firestoreManager.getMasterTakersFlow().collect { masterTakers = it } }

    fun runMigration() {
        scope.launch {
            val oldEvents  = dataStore.loadEvents()
            val oldItems   = dataStore.loadItemList()
            val oldTakers  = dataStore.loadMasterTakers()
            if (oldEvents.isNotEmpty() || oldItems.isNotEmpty() || oldTakers.isNotEmpty()) {
                syncStatus = 1
                val ok = firestoreManager.migrateLegacyData(oldEvents, oldItems, oldTakers)
                if (ok) {
                    context.getSharedPreferences("ProducerAppData", Context.MODE_PRIVATE)
                        .edit { putBoolean("is_data_migrated_to_firestore", true) }
                    dataStore.clearLegacyData()
                    notifStore.logSuccess("Migration Complete", "All local data migrated to Firestore successfully.")
                    syncStatus = 3; delay(3000); syncStatus = 0
                } else {
                    notifStore.logError("Migration Failed", "Could not migrate local data to Firestore. Check internet connection.")
                    syncStatus = 2
                }
            } else syncStatus = 0
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("ProducerAppData", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_data_migrated_to_firestore", false)) runMigration()
    }

    // Permissions
    val permissionsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val notifOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false else true
        val smsOk   = permissions[Manifest.permission.SEND_SMS] ?: false
        if (!notifOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, "Notifications denied.", Toast.LENGTH_LONG).show()
        if (!smsOk) Toast.makeText(context, "SMS permission denied.", Toast.LENGTH_LONG).show()
    }
    LaunchedEffect(Unit) {
        val perms = mutableListOf(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        if (perms.any { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED })
            permissionsLauncher.launch(perms.toTypedArray())
    }

    // Navigation
    var selectedEventId                by remember { mutableStateOf<Long?>(null) }
    var managingTakersDayId            by remember { mutableStateOf<Long?>(null) }
    var showAllTakersEventId           by remember { mutableStateOf<Long?>(null) }
    var showInternalExpensesEventId    by remember { mutableStateOf<Long?>(null) }
    var currentBottomTab               by remember { mutableIntStateOf(0) }

    val currentEvent           = events.find { it.id == selectedEventId }
    val managingTakersDayEvent = events.find { ev -> ev.days.any { it.id == managingTakersDayId } }
    val managingTakersDay      = managingTakersDayEvent?.days?.find { it.id == managingTakersDayId }
    val allTakersEvent         = events.find { it.id == showAllTakersEventId }
    val internalExpensesEvent  = events.find { it.id == showInternalExpensesEventId }

    val navDepth = when {
        managingTakersDayId != null || showAllTakersEventId != null || showInternalExpensesEventId != null -> 2
        selectedEventId != null -> 1
        else -> 0
    }
    val isMainScreen = navDepth == 0

    // Clear badge when user opens Alerts tab
    LaunchedEffect(currentBottomTab) {
        if (currentBottomTab == 3) unreadAlertCount = 0
    }

    Scaffold(
        topBar = {
            Column {
                AppBanner()
                AnimatedVisibility(visible = syncStatus != 0) { SyncStatusIndicator(syncStatus) }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = isMainScreen,
                enter = slideInHorizontally(tween(300)) { 0 } + fadeIn(tween(300)),
                exit  = slideOutHorizontally(tween(300)) { 0 } + fadeOut(tween(300))
            ) {
                NavigationBar(tonalElevation = 8.dp) {
                    // Tab 0: Events
                    NavigationBarItem(selected = currentBottomTab == 0, onClick = { currentBottomTab = 0 }, icon = { Icon(if (currentBottomTab == 0) Icons.Filled.Event else Icons.Outlined.Event, "Events") }, label = { Text("Events", style = MaterialTheme.typography.labelMedium) })
                    // Tab 1: Takers
                    NavigationBarItem(selected = currentBottomTab == 1, onClick = { currentBottomTab = 1 }, icon = { Icon(if (currentBottomTab == 1) Icons.Filled.Groups else Icons.Outlined.Groups, "Takers") }, label = { Text("Takers", style = MaterialTheme.typography.labelMedium) })
                    // Tab 2: Analytics
                    NavigationBarItem(selected = currentBottomTab == 2, onClick = { currentBottomTab = 2 }, icon = { Icon(if (currentBottomTab == 2) Icons.Filled.BarChart else Icons.Outlined.BarChart, "Analytics") }, label = { Text("Analytics", style = MaterialTheme.typography.labelMedium) })
                    // Tab 3: Alerts — with badge for errors/warnings
                    NavigationBarItem(
                        selected = currentBottomTab == 3,
                        onClick  = { currentBottomTab = 3; unreadAlertCount = 0 },
                        icon = {
                            BadgedBox(badge = {
                                if (unreadAlertCount > 0) Badge { Text(if (unreadAlertCount > 9) "9+" else "$unreadAlertCount") }
                            }) {
                                Icon(if (currentBottomTab == 3) Icons.Filled.Notifications else Icons.Outlined.Notifications, "Alerts")
                            }
                        },
                        label = { Text("Alerts", style = MaterialTheme.typography.labelMedium) }
                    )
                    // Tab 4: Settings
                    NavigationBarItem(selected = currentBottomTab == 4, onClick = { currentBottomTab = 4 }, icon = { Icon(if (currentBottomTab == 4) Icons.Filled.Settings else Icons.Outlined.Settings, "Settings") }, label = { Text("Settings", style = MaterialTheme.typography.labelMedium) })
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            AnimatedContent(
                targetState = navDepth,
                transitionSpec = {
                    if (targetState >= initialState) {
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 3 } + fadeOut(tween(200)))
                    } else {
                        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 3 } + fadeIn(tween(300))).togetherWith(
                            slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
                    }
                },
                label = "mainNav"
            ) { depth ->
                val unused = depth
                when {
                    // ── Depth 2 ────────────────────────────────────────────────
                    managingTakersDay != null -> TakerPaymentScreen(
                        day = managingTakersDay, eventTitle = managingTakersDayEvent?.title ?: "", masterTakers = masterTakers,
                        onBack = { managingTakersDayId = null },
                        onSaveTakers = { dayId, updatedTakers ->
                            val event = events.find { ev -> ev.days.any { it.id == dayId } } ?: return@TakerPaymentScreen
                            val u = event.deepCopy(); val di = u.days.indexOfFirst { it.id == dayId }
                            if (di != -1) u.days[di].takers = updatedTakers.toMutableList()
                            firestoreManager.saveEvent(u)
                        },
                        onMasterTakerUpdated = { firestoreManager.saveMasterTaker(it) }
                    )

                    allTakersEvent != null -> AllTakersSummaryScreen(
                        event = allTakersEvent, onBack = { showAllTakersEventId = null },
                        onAddTakerToDays = { name, totalDue, amountPaid, dayIds ->
                            val updated = allTakersEvent.deepCopy()
                            updated.days.filter { it.id in dayIds }.forEachIndexed { i, day ->
                                day.takers.add(Taker(System.currentTimeMillis() + i, name, totalDue, amountPaid))
                            }
                            firestoreManager.saveEvent(updated)
                        }
                    )

                    internalExpensesEvent != null -> InternalExpensesScreen(
                        event = internalExpensesEvent, onBack = { showInternalExpensesEventId = null },
                        onUpdateExpenses = { updatedList ->
                            val updated = internalExpensesEvent.deepCopy(); updated.internalExpenses = updatedList.toMutableList()
                            firestoreManager.saveEvent(updated)
                        }
                    )

                    // ── Depth 1 ────────────────────────────────────────────────
                    currentEvent != null -> EventDetailScreen(
                        event = currentEvent, albumSettings = albumSettings.value, itemList = itemList,
                        onUpdatePayments = { payments -> val u = currentEvent.deepCopy(); u.payments = payments.toMutableList(); firestoreManager.saveEvent(u) },
                        onUpdateDiscount = { discount -> val u = currentEvent.deepCopy(); u.discount = discount; firestoreManager.saveEvent(u) },
                        onAddDay = { title, date -> val u = currentEvent.deepCopy(); u.days.add(Day(System.currentTimeMillis(), title, date)); firestoreManager.saveEvent(u) },
                        onEditDay = { dayId, newTitle, newDate -> val u = currentEvent.deepCopy(); u.days.find { it.id == dayId }?.apply { title = newTitle; date = newDate }; firestoreManager.saveEvent(u) },
                        onDeleteDay = { dayId -> val u = currentEvent.deepCopy(); u.days.removeIf { it.id == dayId }; firestoreManager.saveEvent(u) },
                        onManageTakersForDay = { dayId -> managingTakersDayId = dayId },
                        onManageAllTakers = { showAllTakersEventId = currentEvent.id },
                        onAddEventItem = { item -> val u = currentEvent.deepCopy(); u.eventItems.add(item); firestoreManager.saveEvent(u) },
                        onAddDayItem = { dayId, name, cost, qty -> val u = currentEvent.deepCopy(); u.days.find { it.id == dayId }?.items?.add(Item(System.currentTimeMillis(), name, cost, qty)); firestoreManager.saveEvent(u) },
                        onEditItem = { itemId, dayId, newName, newCost -> val u = currentEvent.deepCopy(); val item = if (dayId == null) u.eventItems.find { it.id == itemId } else u.days.find { it.id == dayId }?.items?.find { it.id == itemId }; item?.apply { name = newName; cost = newCost }; firestoreManager.saveEvent(u) },
                        onUpdateItemQuantity = { itemId, dayId, newQty -> val u = currentEvent.deepCopy(); val list = if (dayId == null) u.eventItems else u.days.find { it.id == dayId }?.items; list?.find { it.id == itemId }?.let { item -> if (newQty > 0) item.quantity = newQty else list.remove(item) }; firestoreManager.saveEvent(u) },
                        onDeleteItem = { itemId, dayId -> val u = currentEvent.deepCopy(); val list = if (dayId == null) u.eventItems else u.days.find { it.id == dayId }?.items; list?.removeIf { it.id == itemId }; firestoreManager.saveEvent(u) },
                        onShowInternalExpenses = { showInternalExpensesEventId = currentEvent.id },
                        onUpdateEventStatus = { isConfirmed -> val u = currentEvent.deepCopy(); u.isConfirmed = isConfirmed; firestoreManager.saveEvent(u) },
                        onBack = { selectedEventId = null }
                    )

                    // ── Depth 0 ────────────────────────────────────────────────
                    else -> when (currentBottomTab) {
                        0 -> EventListScreen(
                            events = events, onEventClick = { selectedEventId = it.id },
                            onAddEvent = { title, date, cName, cPhone -> firestoreManager.saveEvent(Event(System.currentTimeMillis(), title, date, cName, cPhone)) },
                            onEditEvent = { eventId, newTitle, newDate, newCName, newCPhone ->
                                events.find { it.id == eventId }?.let { firestoreManager.saveEvent(it.deepCopy().copy(title = newTitle, date = newDate, customerName = newCName, customerPhone = newCPhone)) }
                            },
                            onDeleteEvent = { eventId -> firestoreManager.moveToTrash(eventId) },
                            onRefresh = { Toast.makeText(context, "Checking for updates…", Toast.LENGTH_SHORT).show(); runMigration() }
                        )
                        1 -> TakersDashboardScreen(
                            events = events, masterTakers = masterTakers,
                            onTakerUpdated = { eventId, dayId, updatedTaker ->
                                val event = events.find { it.id == eventId } ?: return@TakersDashboardScreen
                                val u = event.deepCopy(); u.days.find { it.id == dayId }?.takers?.replaceAll { if (it.id == updatedTaker.id) updatedTaker else it }
                                firestoreManager.saveEvent(u)
                            }
                        )
                        2 -> AnalyticsScreen(events = events, masterTakers = masterTakers)
                        3 -> AlertsScreen()
                        else -> SettingsScreen(
                            initialMasterItems = itemList, initialMasterTakers = masterTakers,
                            initialAlbumSettings = albumSettings.value, eventsCount = events.size, deletedEvents = deletedEvents,
                            onUpdateMasterItems  = { list -> list.forEach { firestoreManager.saveMasterItem(it) } },
                            onUpdateMasterTakers = { list -> list.forEach { firestoreManager.saveMasterTaker(it) } },
                            onDeleteMasterTaker  = { id -> firestoreManager.deleteMasterTaker(id) },
                            onUpdateAlbumSettings = { s -> albumSettings.value = s; dataStore.saveAlbumSettings(s) },
                            onRestoreEvent = { firestoreManager.restoreEvent(it.id) },
                            onPermanentlyDeleteEvent = { firestoreManager.permanentlyDeleteEvent(it.id) }
                        )
                    }
                }
            }

            // Floating sync icon — main screen only (Bug-13)
            AnimatedVisibility(
                visible = isMainScreen,
                enter   = scaleIn(tween(200)) + fadeIn(tween(200)),
                exit    = scaleOut(tween(200)) + fadeOut(tween(200)),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp)
            ) {
                FloatingSyncIcon(status = syncStatus, modifier = Modifier.clickable {
                    Toast.makeText(context, "Checking for data to upload…", Toast.LENGTH_SHORT).show(); runMigration()
                })
            }
        }
    }
}