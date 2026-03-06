package com.example.supriyadigitalproducerfinal

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.work.*
import com.example.supriyadigitalproducerfinal.ui.theme.SupriyaDigitalProducerFinalTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import androidx.compose.ui.graphics.Color as ComposeColor

// --- HELPER FUNCTION: Deep Copy for Compose State Fix ---
fun Event.deepCopy(): Event {
    val gson = Gson()
    return gson.fromJson(gson.toJson(this), Event::class.java)
}

// --- MAIN ACTIVITY ---
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        setContent {
            SupriyaDigitalProducerFinalTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    var isAuthenticated by remember { mutableStateOf(false) }

                    if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
                        ProducerApp()
                    } else {
                        if (isAuthenticated) {
                            ProducerApp()
                        } else {
                            LockScreen(
                                onAuthenticate = {
                                    showBiometricPrompt(
                                        onSuccess = { isAuthenticated = true },
                                        onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        onError("Auth Error: $errString")
                    }
                }
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed. Try again.")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Supriya Digital Studio")
            .setSubtitle("Unlock to access producer data")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}

// --- LOCK SCREEN ---
@Composable
fun LockScreen(onAuthenticate: () -> Unit) {
    LaunchedEffect(Unit) { onAuthenticate() }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Lock, "Locked", Modifier.size(64.dp), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Producer App Locked", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Please authenticate to continue")
        Spacer(Modifier.height(32.dp))
        Button(onClick = onAuthenticate) { Text("Unlock App") }
    }
}

// --- PRODUCER APP ---
@Composable
fun ProducerApp() {
    val context = LocalContext.current
    val firestoreManager = remember { FirestoreManager() }
    val dataStore = remember { DataStore(context) }
    val scope = rememberCoroutineScope()

    var syncStatus by remember { mutableIntStateOf(0) }
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }
    var deletedEvents by remember { mutableStateOf<List<Event>>(emptyList()) }
    var itemList by remember { mutableStateOf<List<MasterItem>>(emptyList()) }
    var masterTakers by remember { mutableStateOf<List<MasterTaker>>(emptyList()) }
    val albumSettings = remember { mutableStateOf(dataStore.loadAlbumSettings()) }

    LaunchedEffect(Unit) { firestoreManager.getEventsFlow().collect { events = it } }
    LaunchedEffect(Unit) { firestoreManager.getDeletedEventsFlow().collect { deletedEvents = it } }
    LaunchedEffect(Unit) { firestoreManager.getMasterItemsFlow().collect { itemList = it } }
    LaunchedEffect(Unit) { firestoreManager.getMasterTakersFlow().collect { masterTakers = it } }

    fun runMigration() {
        scope.launch {
            val oldEvents = dataStore.loadEvents()
            val oldItems = dataStore.loadItemList()
            val oldTakers = dataStore.loadMasterTakers()

            if (oldEvents.isNotEmpty() || oldItems.isNotEmpty() || oldTakers.isNotEmpty()) {
                syncStatus = 1
                val success = firestoreManager.migrateLegacyData(oldEvents, oldItems, oldTakers)
                if (success) {
                    val prefs = context.getSharedPreferences("ProducerAppData", Context.MODE_PRIVATE)
                    prefs.edit { putBoolean("is_data_migrated_to_firestore", true) }
                    dataStore.clearLegacyData()
                    syncStatus = 3
                    delay(3000)
                    syncStatus = 0
                } else {
                    syncStatus = 2
                }
            } else {
                syncStatus = 0
            }
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("ProducerAppData", Context.MODE_PRIVATE)
        val isMigrated = prefs.getBoolean("is_data_migrated_to_firestore", false)
        if (!isMigrated) {
            runMigration()
        }
    }

    var selectedEventId by remember { mutableStateOf<Long?>(null) }
    var managingTakersDayId by remember { mutableStateOf<Long?>(null) }
    var showAllTakersEventId by remember { mutableStateOf<Long?>(null) }
    var showInternalExpensesEventId by remember { mutableStateOf<Long?>(null) }

    // NEW: Bottom Navigation State
    var currentBottomTab by remember { mutableIntStateOf(0) }

    val currentEvent = events.find { it.id == selectedEventId }
    val managingTakersDayEvent = events.find { ev -> ev.days.any { it.id == managingTakersDayId } }
    val managingTakersDay = managingTakersDayEvent?.days?.find { it.id == managingTakersDayId }
    val allTakersEvent = events.find { it.id == showAllTakersEventId }
    val internalExpensesEvent = events.find { it.id == showInternalExpensesEventId }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
            } else true
            val smsGranted = permissions[Manifest.permission.SEND_SMS] ?: false

            if (!notificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, "Notifications denied.", Toast.LENGTH_LONG).show()
            }
            if (!smsGranted) {
                Toast.makeText(context, "SMS permission denied. Automated texts will fail.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(key1 = true) {
        val permissionsToRequest = mutableListOf(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needsRequest = permissionsToRequest.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needsRequest) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        topBar = {
            Column {
                AppBanner()
                if (syncStatus != 0) { SyncStatusIndicator(status = syncStatus) }
            }
        },
        bottomBar = {
            // Hide bottom bar when deep inside sub-screens
            if (selectedEventId == null && managingTakersDayId == null && showAllTakersEventId == null && showInternalExpensesEventId == null) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentBottomTab == 0,
                        onClick = { currentBottomTab = 0 },
                        icon = { Icon(Icons.Filled.Event, "Events") },
                        label = { Text("Events") }
                    )
                    NavigationBarItem(
                        selected = currentBottomTab == 1,
                        onClick = { currentBottomTab = 1 },
                        icon = { Icon(Icons.Filled.Groups, "Takers") },
                        label = { Text("Takers") }
                    )
                    NavigationBarItem(
                        selected = currentBottomTab == 2,
                        onClick = { currentBottomTab = 2 },
                        icon = { Icon(Icons.Filled.Settings, "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when {
                    managingTakersDay != null -> TakerPaymentScreen(
                    day = managingTakersDay,
                    masterTakers = masterTakers,
                    onBack = { managingTakersDayId = null },
                    onTakerUpdated = { firestoreManager.saveEvent(managingTakersDayEvent) },
                    onMasterTakerUpdated = { firestoreManager.saveMasterTaker(it) }
                )
                allTakersEvent != null -> AllTakersSummaryScreen(
                    event = allTakersEvent,
                    onBack = { showAllTakersEventId = null },
                    onAddTakerToDays = { name, totalDue, amountPaid, dayIds ->
                        val updatedEvent = allTakersEvent.deepCopy()
                        updatedEvent.days.filter { it.id in dayIds }.forEachIndexed { index, day ->
                            day.takers.add(Taker(System.currentTimeMillis() + index, name, totalDue, amountPaid))
                        }
                        firestoreManager.saveEvent(updatedEvent)
                    }
                )
                internalExpensesEvent != null -> InternalExpensesScreen(
                    event = internalExpensesEvent,
                    onBack = { showInternalExpensesEventId = null },
                    onUpdateExpenses = { updatedList ->
                        val updatedEvent = internalExpensesEvent.deepCopy()
                        updatedEvent.internalExpenses = updatedList.toMutableList()
                        firestoreManager.saveEvent(updatedEvent)
                    }
                )
                currentEvent != null -> EventDetailScreen(
                    event = currentEvent, albumSettings = albumSettings.value, itemList = itemList,
                    onUpdatePayments = { payments -> val updatedEvent = currentEvent.deepCopy(); updatedEvent.payments = payments.toMutableList(); firestoreManager.saveEvent(updatedEvent) },
                    onUpdateDiscount = { discount -> val updatedEvent = currentEvent.deepCopy(); updatedEvent.discount = discount; firestoreManager.saveEvent(updatedEvent) },
                    onAddDay = { title, date -> val updatedEvent = currentEvent.deepCopy(); updatedEvent.days.add(Day(System.currentTimeMillis(), title, date)); firestoreManager.saveEvent(updatedEvent) },
                    onEditDay = { dayId, newTitle, newDate -> val updatedEvent = currentEvent.deepCopy(); updatedEvent.days.find { it.id == dayId }?.apply { title = newTitle; date = newDate }; firestoreManager.saveEvent(updatedEvent) },
                    onDeleteDay = { dayId -> val updatedEvent = currentEvent.deepCopy(); updatedEvent.days.removeIf { it.id == dayId }; firestoreManager.saveEvent(updatedEvent) },
                    onManageTakersForDay = { dayId -> managingTakersDayId = dayId },
                    onManageAllTakers = { showAllTakersEventId = currentEvent.id },
                    onAddEventItem = { item -> val updatedEvent = currentEvent.deepCopy(); updatedEvent.eventItems.add(item); firestoreManager.saveEvent(updatedEvent) },
                    onAddDayItem = { dayId, name, cost, quantity -> val updatedEvent = currentEvent.deepCopy(); updatedEvent.days.find { it.id == dayId }?.items?.add(Item(System.currentTimeMillis(), name, cost, quantity)); firestoreManager.saveEvent(updatedEvent) },
                    onEditItem = { itemId, dayId, newName, newCost -> val updatedEvent = currentEvent.deepCopy(); val item = if (dayId == null) updatedEvent.eventItems.find { it.id == itemId } else updatedEvent.days.find { it.id == dayId }?.items?.find { it.id == itemId }; item?.apply { name = newName; cost = newCost }; firestoreManager.saveEvent(updatedEvent) },
                    onUpdateItemQuantity = { itemId, dayId, newQuantity -> val updatedEvent = currentEvent.deepCopy(); val itemsList = if (dayId == null) updatedEvent.eventItems else updatedEvent.days.find { it.id == dayId }?.items; itemsList?.find { it.id == itemId }?.let { item -> if (newQuantity > 0) item.quantity = newQuantity else itemsList.remove(item) }; firestoreManager.saveEvent(updatedEvent) },
                    onDeleteItem = { itemId, dayId -> val updatedEvent = currentEvent.deepCopy(); val itemsList = if (dayId == null) updatedEvent.eventItems else updatedEvent.days.find { it.id == dayId }?.items; itemsList?.removeIf { it.id == itemId }; firestoreManager.saveEvent(updatedEvent) },
                    onShowInternalExpenses = { showInternalExpensesEventId = currentEvent.id },
                    onUpdateEventStatus = { isConfirmed -> val updatedEvent = currentEvent.deepCopy(); updatedEvent.isConfirmed = isConfirmed; firestoreManager.saveEvent(updatedEvent) },
                    onBack = { selectedEventId = null }
                )
                else -> {
                    when (currentBottomTab) {
                        0 -> EventListScreen(
                            events = events,
                            onEventClick = { selectedEventId = it.id },
                            onAddEvent = { title, date, cName, cPhone -> firestoreManager.saveEvent(Event(System.currentTimeMillis(), title, date, cName, cPhone)) },
                            onEditEvent = { eventId, newTitle, newDate, newCustomerName, newCustomerPhone -> events.find { it.id == eventId }?.let { firestoreManager.saveEvent(it.deepCopy().copy(title = newTitle, date = newDate, customerName = newCustomerName, customerPhone = newCustomerPhone)) } },
                            onDeleteEvent = { eventId -> firestoreManager.moveToTrash(eventId) },
                            onRefresh = { Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show(); runMigration() }
                        )
                        1 -> TakersDashboardScreen(
                            events = events,
                            onTakerUpdated = { eventId, dayId, updatedTaker ->
                                val event = events.find { it.id == eventId }
                                if (event != null) {
                                    val updatedEvent = event.deepCopy()
                                    updatedEvent.days.find { it.id == dayId }?.takers?.replaceAll { if (it.id == updatedTaker.id) updatedTaker else it }
                                    firestoreManager.saveEvent(updatedEvent)
                                }
                            }
                        )
                        2 -> SettingsScreen(
                            initialMasterItems = itemList,
                            initialMasterTakers = masterTakers,
                            initialAlbumSettings = albumSettings.value,
                            eventsCount = events.size,
                            deletedEvents = deletedEvents,
                            onUpdateMasterItems = { list -> list.forEach { firestoreManager.saveMasterItem(it) } },
                            onUpdateMasterTakers = { list -> list.forEach { firestoreManager.saveMasterTaker(it) } },
                            onUpdateAlbumSettings = { newSettings -> albumSettings.value = newSettings; dataStore.saveAlbumSettings(newSettings) },
                            onRestoreEvent = { event -> firestoreManager.restoreEvent(event.id) },
                            onPermanentlyDeleteEvent = { event -> firestoreManager.permanentlyDeleteEvent(event.id) }
                        )
                    }
                }
            }

            FloatingSyncIcon(
                status = syncStatus,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
                    .clickable {
                        Toast.makeText(context, "Checking for data to upload...", Toast.LENGTH_SHORT).show()
                        runMigration()
                    }
            )
        }
    }
}

// --- SYNC STATUS COMPONENTS ---
@Composable
fun SyncStatusIndicator(status: Int) {
    val (color, text, icon) = when (status) {
        1 -> Triple(MaterialTheme.colorScheme.primary, "Syncing old data...", Icons.Filled.CloudUpload)
        2 -> Triple(MaterialTheme.colorScheme.error, "Sync Failed. Check Internet.", Icons.Filled.CloudOff)
        3 -> Triple(ComposeColor(0xFF4CAF50), "Data Updated Successfully!", Icons.Filled.CloudDone)
        else -> return
    }

    Surface(color = color, contentColor = ComposeColor.White, modifier = Modifier.fillMaxWidth().height(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FloatingSyncIcon(status: Int, modifier: Modifier = Modifier) {
    val (color, icon) = when (status) {
        0 -> Pair(ComposeColor.Gray, Icons.Filled.CloudQueue)
        1 -> Pair(MaterialTheme.colorScheme.primary, Icons.Filled.CloudUpload)
        2 -> Pair(MaterialTheme.colorScheme.error, Icons.Filled.Warning)
        3 -> Pair(ComposeColor(0xFF4CAF50), Icons.Filled.CheckCircle)
        else -> Pair(ComposeColor.Gray, Icons.Filled.CloudQueue)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sync")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = if (status == 1) 360f else 0f,
        animationSpec = infiniteRepeatable(animation = tween(1000, easing = LinearEasing), repeatMode = RepeatMode.Restart), label = "rotation"
    )

    Surface(shape = CircleShape, color = color, shadowElevation = 4.dp, modifier = modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = "Sync Status", tint = ComposeColor.White, modifier = Modifier.rotate(if (status == 1) angle else 0f))
        }
    }
}

// --- DATA CLASSES ---
data class MasterItem(val id: Long = 0, var name: String = "", var cost: Double = 0.0)
data class MasterTaker(val id: Long = 0, var name: String = "", var defaultTotalDue: Double = 0.0, var defaultPhone: String = "")
data class TakerEventSummary(val eventId: Long, val eventTitle: String, val dayTitle: String, val dayDate: String, val totalDue: Double, val amountPaid: Double)
data class Item(val id: Long = 0, var name: String = "", var cost: Double = 0.0, var quantity: Int = 0)
data class Taker(val id: Long = 0, var name: String = "", var totalDue: Double = 0.0, var amountPaid: Double = 0.0)
data class Payment(val id: Long = 0, var title: String = "", var amount: Double = 0.0, var date: String = "")
data class Day(val id: Long = 0, var title: String = "", var date: String = "", var items: MutableList<Item> = mutableListOf(), var takers: MutableList<Taker> = mutableListOf())
data class InternalExpense(val id: Long = 0, var title: String = "", var amount: Double = 0.0, var date: String = "")
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
data class AlbumSettings(var baseCost: Double = 10000.0, var defaultSheets: Int = 30, var extraSheetCost: Double = 250.0)
private data class EditingTakerInfo(val taker: Taker, val eventId: Long, val dayId: Long)
private enum class SortOrder { NONE, ASCENDING, DESCENDING }

// --- DATA STORE ---
class DataStore(context: Context) {
    private val prefs = context.getSharedPreferences("ProducerAppData", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveEvents(events: List<Event>) { prefs.edit { putString("events_data", gson.toJson(events)) } }

    fun loadEvents(): MutableList<Event> {
        val json = prefs.getString("events_data", null)
        val type = object : TypeToken<MutableList<Event>>() {}.type
        return if (json != null) gson.fromJson(json, type) else mutableListOf()
    }

    fun saveItemList(itemList: List<MasterItem>) { prefs.edit { putString("item_list_data", gson.toJson(itemList)) } }

    fun loadItemList(): MutableList<MasterItem> {
        val json = prefs.getString("item_list_data", null)
        val type = object : TypeToken<MutableList<MasterItem>>() {}.type
        return if (json != null) gson.fromJson(json, type) else mutableListOf()
    }

    fun saveMasterTakers(masterTakers: List<MasterTaker>) { prefs.edit { putString("master_takers_list", gson.toJson(masterTakers)) } }

    fun loadMasterTakers(): MutableList<MasterTaker> {
        val json = prefs.getString("master_takers_list", null)
        val type = object : TypeToken<MutableList<MasterTaker>>() {}.type
        return if (json != null) gson.fromJson(json, type) else mutableListOf()
    }

    fun saveAlbumSettings(settings: AlbumSettings) { prefs.edit { putString("album_settings", gson.toJson(settings)) } }

    fun loadAlbumSettings(): AlbumSettings {
        val json = prefs.getString("album_settings", null)
        return if (json != null) gson.fromJson(json, AlbumSettings::class.java) else AlbumSettings()
    }

    fun clearLegacyData() {
        prefs.edit {
            remove("events_data")
            remove("item_list_data")
            remove("master_takers_list")
            apply()
        }
    }
}

@Composable
fun AppBanner() {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp, tonalElevation = 4.dp) {
        Image(
            painter = painterResource(id = R.drawable.supriya_banner),
            contentDescription = "Supriya Digital Studio Banner",
            modifier = Modifier.fillMaxWidth().height(100.dp).clip(RectangleShape),
            contentScale = ContentScale.FillWidth
        )
    }
}

fun createEventBillPdf(context: Context, event: Event, isFinalInvoice: Boolean) {
    val docTitle = if (isFinalInvoice) "I N V O I C E" else "BILL  ANALYSIS"
    val filePrefix = if (isFinalInvoice) "Invoice" else "Estimate"

    val subFolder = if (isFinalInvoice) "Confirmed" else "Analysis"
    val relativePath = Environment.DIRECTORY_DOCUMENTS + "/SupriyaDigital/$subFolder/"

    val totalCost = event.eventItems.sumOf { it.cost * it.quantity } +
            event.days.sumOf { day -> day.items.sumOf { item -> item.cost * item.quantity } }
    val totalPaid = event.payments.sumOf { it.amount }
    val grandTotal = totalCost - event.discount
    val balanceDue = grandTotal - totalPaid

    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas: android.graphics.Canvas = page.canvas

    val studioNamePaint = Paint().apply { typeface = Typeface.create("serif", Typeface.BOLD); textSize = 24f; color = Color.BLACK; textAlign = Paint.Align.CENTER }
    val subtitlePaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL); textSize = 10f; color = Color.DKGRAY; textAlign = Paint.Align.CENTER }
    val studioPhonePaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 10f; color = Color.BLACK; textAlign = Paint.Align.RIGHT }
    val sectionHeaderPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 9f; color = Color.GRAY; letterSpacing = 0.08f }
    val labelPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL); textSize = 10f; color = Color.DKGRAY; textAlign = Paint.Align.RIGHT }
    val dataTextPaint = android.text.TextPaint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL); textSize = 10f; color = Color.BLACK }
    val dataBoldTextPaint = android.text.TextPaint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 11f; color = Color.BLACK }
    val moneyPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.NORMAL); textSize = 10f; color = Color.BLACK; textAlign = Paint.Align.RIGHT }
    val moneyBoldPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 11f; color = Color.BLACK; textAlign = Paint.Align.RIGHT }
    val balancePaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 14f; color = if (balanceDue > 0) Color.rgb(220, 53, 69) else Color.rgb(40, 167, 69); textAlign = Paint.Align.RIGHT }
    val tableHeaderBgPaint = Paint().apply { color = Color.rgb(240, 240, 240); style = Paint.Style.FILL }
    val tableHeaderTextPaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 10f; color = Color.BLACK }
    val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f }

    val leftMargin = 40f
    val rightMargin = 555f
    val topMargin = 40f
    val centerX = 595f / 2f
    val colDesc = leftMargin + 10f
    val colQty = 340f
    val colCost = 450f
    val colTotal = rightMargin - 5f
    val descWidth = (colQty - colDesc - 20).toInt()

    fun drawWrappedText(text: String, x: Float, y: Float, width: Int, paint: android.text.TextPaint): Float {
        if (text.isEmpty()) return 0f
        val builder = android.text.StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(android.text.Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.0f).setIncludePad(false)
        val layout = builder.build()
        canvas.save(); canvas.translate(x, y); layout.draw(canvas); canvas.restore()
        return layout.height.toFloat()
    }

    class PageManager {
        var yPos = topMargin
        var currentPageNum = 1
        fun advance(space: Float): Boolean {
            yPos += space
            if (yPos > 800f) {
                pdfDocument.finishPage(page)
                currentPageNum++
                val newPageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNum).create()
                page = pdfDocument.startPage(newPageInfo)
                canvas = page.canvas
                yPos = topMargin + 20f
                return true
            }
            return false
        }
        fun drawTableHeader() {
            advance(10f)
            canvas.drawRect(leftMargin, yPos, rightMargin, yPos + 20f, tableHeaderBgPaint)
            val tY = yPos + 14f
            canvas.drawText("Description", colDesc, tY, tableHeaderTextPaint)
            canvas.drawText("Qty", colQty, tY, Paint(tableHeaderTextPaint).apply { textAlign = Paint.Align.CENTER })
            canvas.drawText("Unit Cost", colCost, tY, Paint(tableHeaderTextPaint).apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText("Total", colTotal, tY, Paint(tableHeaderTextPaint).apply { textAlign = Paint.Align.RIGHT })
            advance(20f)
        }
    }
    val pm = PageManager()

    try {
        canvas.drawRect(20f, 20f, 575f, 822f, Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = Color.LTGRAY })
        try {
            val drawable = AppCompatResources.getDrawable(context, R.drawable.logo)
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                val scale = 0.5f
                val w = (bitmap.width * scale).toInt(); val h = (bitmap.height * scale).toInt()
                val scaled = bitmap.scale(w, h, false)
                canvas.drawBitmap(scaled, 595f/2f - w/2f, 842f/2f - h/2f, Paint().apply { alpha = 20 })
            }
        } catch (_: Exception) {}

        // Header
        canvas.drawText("Supriya Digital Studio", centerX, pm.yPos + 30f, studioNamePaint)
        pm.advance(45f)
        canvas.drawText("Photography & Videography", centerX, pm.yPos + 5f, subtitlePaint)
        canvas.drawText("Phone: 9246789966", rightMargin, topMargin + 20f, studioPhonePaint)

        pm.advance(40f)
        val invTitlePaint = Paint().apply { typeface = Typeface.create("sans-serif", Typeface.BOLD); textSize = 16f; textAlign = Paint.Align.CENTER; letterSpacing = 0.3f }
        canvas.drawText(docTitle, 595f / 2, pm.yPos, invTitlePaint)
        pm.advance(35f)

        // Customer Info
        val leftColX = leftMargin
        val leftColWidth = 240
        val rightLabelsX = 380f
        val rightValuesX = 390f
        val rightColWidth = (rightMargin - rightValuesX).toInt()
        val infoStartY = pm.yPos

        var currentLeftY = infoStartY
        canvas.drawText("BILLED TO", leftColX, currentLeftY, sectionHeaderPaint)
        currentLeftY += 15f
        currentLeftY += drawWrappedText(event.customerName, leftColX, currentLeftY, leftColWidth, dataBoldTextPaint) + 2f
        if (event.customerPhone.isNotBlank()) {
            currentLeftY += drawWrappedText("Ph: ${event.customerPhone}", leftColX, currentLeftY, leftColWidth, dataTextPaint)
        }

        var currentRightY = infoStartY
        canvas.drawText(if(isFinalInvoice) "INVOICE DETAILS" else "ESTIMATE DETAILS", rightValuesX, currentRightY, sectionHeaderPaint)
        currentRightY += 15f
        canvas.drawText("Date:", rightLabelsX, currentRightY, labelPaint)
        canvas.drawText(SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date()), rightValuesX, currentRightY, Paint(dataTextPaint))

        currentRightY += 15f
        val formattedEventTitle = "Event: ${event.title}"
        val eventTitleHeight = drawWrappedText(formattedEventTitle, rightLabelsX - 30f, currentRightY, rightColWidth + 30, dataBoldTextPaint)
        currentRightY += maxOf(eventTitleHeight, 10f)

        pm.advance(maxOf(currentLeftY - infoStartY, currentRightY - infoStartY) + 25f)

        // Table
        pm.drawTableHeader()
        fun drawRow(name: String, qty: String, cost: Double, total: Double) {
            val builder = android.text.StaticLayout.Builder.obtain(name, 0, name.length, dataTextPaint, descWidth)
            val measure = builder.build()
            val rowHeight = maxOf(measure.height.toFloat(), 15f)
            if (pm.advance(rowHeight + 10f)) { pm.drawTableHeader(); pm.yPos -= (rowHeight + 10f) }
            drawWrappedText(name, colDesc, pm.yPos, descWidth, dataTextPaint)
            val textY = pm.yPos
            canvas.drawText(qty, colQty, textY, Paint(dataTextPaint).apply { textAlign = Paint.Align.CENTER })
            canvas.drawText(String.format(Locale.getDefault(), "%,.2f", cost), colCost, textY, moneyPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%,.2f", total), colTotal, textY, moneyBoldPaint)
            val lineY = pm.yPos + rowHeight + 5f
            canvas.drawLine(leftMargin, lineY, rightMargin, lineY, linePaint)
            pm.yPos = lineY + 5f
        }

        if (event.days.any { it.items.isNotEmpty() }) {
            event.days.forEach { day ->
                if (day.items.isNotEmpty()) {
                    if (pm.advance(25f)) pm.drawTableHeader()
                    canvas.drawText("Event: ${day.title} (${day.date})", colDesc, pm.yPos, dataBoldTextPaint)
                    pm.advance(5f)
                    day.items.forEach { item -> drawRow(item.name, item.quantity.toString(), item.cost, item.cost * item.quantity) }
                }
            }
        }
        if (event.eventItems.isNotEmpty()) {
            if (pm.advance(25f)) pm.drawTableHeader()
            canvas.drawText("Other Services", colDesc, pm.yPos, dataBoldTextPaint)
            pm.advance(5f)
            event.eventItems.forEach { item -> drawRow(item.name, item.quantity.toString(), item.cost, item.cost * item.quantity) }
        }

        // Totals
        pm.advance(20f)
        val totalsLabelX = 420f
        fun drawTotalLine(label: String, valStr: String, paint: Paint) {
            pm.advance(20f)
            canvas.drawText(label, totalsLabelX, pm.yPos, Paint(dataTextPaint).apply { textAlign = Paint.Align.RIGHT })
            canvas.drawText(valStr, colTotal, pm.yPos, paint)
        }
        canvas.drawLine(totalsLabelX - 20, pm.yPos, rightMargin, pm.yPos, Paint().apply { strokeWidth = 1f })
        drawTotalLine("Sub Total", String.format(Locale.getDefault(), "%,.2f", totalCost), moneyPaint)
        if (event.discount > 0) drawTotalLine("Discount", "- " + String.format(Locale.getDefault(), "%,.2f", event.discount), moneyPaint)
        if (event.payments.isNotEmpty()) {
            pm.advance(5f)
            event.payments.forEach { p -> drawTotalLine("Paid (${p.date})", "- " + String.format(Locale.getDefault(), "%,.2f", p.amount), moneyPaint) }
        }
        pm.advance(15f)
        canvas.drawLine(totalsLabelX - 20, pm.yPos, rightMargin, pm.yPos, Paint().apply { strokeWidth = 2f; color = Color.BLACK })
        pm.advance(20f)
        canvas.drawText(if(isFinalInvoice) "Balance Due" else "Est. Balance", totalsLabelX, pm.yPos, Paint(dataBoldTextPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText(String.format(Locale.getDefault(), "%,.2f", balanceDue), colTotal, pm.yPos, balancePaint)

        if (isFinalInvoice) {
            pm.advance(30f)
            canvas.drawText("* This invoice confirms the booking for the listed dates.", leftMargin, pm.yPos, Paint().apply { textSize = 8f; color = Color.GRAY })
        } else {
            pm.advance(30f)
            canvas.drawText("* This is an estimate/quotation only. Not a confirmation of booking.", leftMargin, pm.yPos, Paint().apply { textSize = 8f; color = Color.GRAY })
        }
        pdfDocument.finishPage(page)

        // Saving and Cleanup
        val safeName = event.customerName.replace(Regex("[^A-Za-z0-9]"), "_")
        val safeTitle = event.title.replace(Regex("[^A-Za-z0-9]"), "_")
        val baseFileName = "${safeName}_${safeTitle}"
        val finalFileName = "${filePrefix}_${baseFileName}.pdf"

        if (isFinalInvoice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val estimateName = "Estimate_${baseFileName}.pdf"
                val projection = arrayOf(MediaStore.MediaColumns._ID)
                val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                val selectionArgs = arrayOf(estimateName, "%SupriyaDigital/Analysis%")

                context.contentResolver.query(
                    MediaStore.Files.getContentUri("external"),
                    projection, selection, selectionArgs, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val deleteUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id)
                        context.contentResolver.delete(deleteUri, null, null)
                        Toast.makeText(context, "Removed old analysis bill", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PDF", "Could not delete old estimate", e)
            }
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)

        if (uri != null) {
            resolver.openOutputStream(uri).use { outputStream -> outputStream?.let { pdfDocument.writeTo(it) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            Toast.makeText(context, "Saved to $subFolder", Toast.LENGTH_LONG).show()

            val message = if(isFinalInvoice) "Hello ${event.customerName},\n\nHere is the invoice for ${event.title}.\n\nSupriya Digital Studio"
            else "Hello ${event.customerName},\n\nHere is the bill analysis for ${event.title}.\n\nSupriya Digital Studio"

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
        }
    } catch (e: Exception) {
        Log.e("PDF", "Error", e)
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
    } finally {
        pdfDocument.close()
    }
}

class ProducerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        scheduleEventReminders()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Event Reminders"
            val descriptionText = "Notifications for upcoming events"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("EVENT_REMINDER_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleEventReminders() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val reminderWorkRequest = PeriodicWorkRequestBuilder<EventReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "eventReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWorkRequest
        )
    }
}

// --- EVENT REMINDER WORKER (SMS + NOTIFICATIONS) ---
class EventReminderWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val dataStore = DataStore(context)
        val events = dataStore.loadEvents()
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }

        val tomorrowCal = todayCal.clone() as Calendar
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)

        val dayAfterCal = todayCal.clone() as Calendar
        dayAfterCal.add(Calendar.DAY_OF_YEAR, 2)

        val tomorrowStr = sdf.format(tomorrowCal.time)
        val dayAfterStr = sdf.format(dayAfterCal.time)

        val tomorrowEvents = mutableListOf<String>()
        val dayAfterEvents = mutableListOf<String>()

        fun formatTakers(takers: List<Taker>): String {
            if (takers.isEmpty()) return "None"
            return takers.joinToString(", ") { "${it.name} (Due: ₹${it.totalDue}, Paid: ₹${it.amountPaid})" }
        }

        events.forEach { event ->
            if (event.date == tomorrowStr) {
                tomorrowEvents.add("Event: ${event.title}\nClient: ${event.customerName}")
            } else if (event.date == dayAfterStr) {
                dayAfterEvents.add("Event: ${event.title}\nClient: ${event.customerName}")
            }

            event.days.forEach { day ->
                if (day.date == tomorrowStr) {
                    val tInfo = formatTakers(day.takers)
                    tomorrowEvents.add("Day: ${day.title} (Part of ${event.title})\nTakers: $tInfo")
                } else if (day.date == dayAfterStr) {
                    val tInfo = formatTakers(day.takers)
                    dayAfterEvents.add("Day: ${day.title} (Part of ${event.title})\nTakers: $tInfo")
                }
            }
        }

        if (tomorrowEvents.isNotEmpty() || dayAfterEvents.isNotEmpty()) {
            val messageBuilder = StringBuilder("Supriya Digital Reminder:\n\n")

            if (tomorrowEvents.isNotEmpty()) {
                messageBuilder.append("--- TOMORROW ---\n")
                tomorrowEvents.forEach { messageBuilder.append(it).append("\n\n") }
            }
            if (dayAfterEvents.isNotEmpty()) {
                messageBuilder.append("--- DAY AFTER ---\n")
                dayAfterEvents.forEach { messageBuilder.append(it).append("\n\n") }
            }

            val finalMessage = messageBuilder.toString().trim()
            val targetPhoneNumber = "9246789966"

            sendSmsMessage(context, targetPhoneNumber, finalMessage)
            sendNotification(context, finalMessage)
        }

        return Result.success()
    }

    private fun sendSmsMessage(context: Context, phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(android.telephony.SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    android.telephony.SmsManager.getDefault()
                }

                val parts = smsManager.divideMessage(message)
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                Log.d("EventReminder", "SMS Sent successfully")
            } catch (e: Exception) {
                Log.e("EventReminder", "Failed to send SMS", e)
            }
        } else {
            Log.e("EventReminder", "SEND_SMS permission not granted")
        }
    }

    private fun sendNotification(context: Context, fullMessage: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(context, "EVENT_REMINDER_CHANNEL_ID")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Upcoming Events Alert")
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullMessage))
            .setContentText("You have upcoming events. Expand to view details.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, builder.build())
        }
    }
}

// --- SCREEN COMPOSABLES ---
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EventListScreen(
    events: List<Event>,
    onEventClick: (Event) -> Unit,
    onAddEvent: (title: String, date: String, customerName: String, customerPhone: String) -> Unit,
    onEditEvent: (eventId: Long, newTitle: String, newDate: String, newCustomerName: String, newCustomerPhone: String) -> Unit,
    onDeleteEvent: (eventId: Long) -> Unit,
    onRefresh: () -> Unit
) {
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.NONE) }
    val context = LocalContext.current

    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            onRefresh()
            delay(1000)
            pullRefreshState.endRefresh()
        }
    }

    var selectedYear by remember { mutableStateOf("Year") }
    var selectedMonth by remember { mutableStateOf("Month") }

    val yearList = remember(events.toList()) {
        listOf("Year") + events.map { it.date.substring(6) }.distinct().sorted()
    }
    val monthList = remember {
        listOf("Month", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December")
    }
    val monthNumberMap = remember {
        monthList.mapIndexedNotNull { index, monthName ->
            if (index > 0) monthName to String.format("%02d", index) else null
        }.toMap()
    }

    val displayedEvents = remember(events.toList(), searchQuery, sortOrder, selectedYear, selectedMonth) {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        val filteredList = events
            .filter { event ->
                if (searchQuery.isBlank()) true
                else {
                    event.title.contains(searchQuery, ignoreCase = true) ||
                            event.customerName.contains(searchQuery, ignoreCase = true) ||
                            event.date.contains(searchQuery, ignoreCase = true)
                }
            }
            .filter { event ->
                if (selectedYear == "Year") true
                else event.date.endsWith(selectedYear)
            }
            .filter { event ->
                if (selectedYear != "Year" && selectedMonth != "Month") {
                    val monthNumber = monthNumberMap[selectedMonth]
                    event.date.substring(3, 5) == monthNumber
                } else {
                    true
                }
            }

        when (sortOrder) {
            SortOrder.ASCENDING -> filteredList.sortedBy { try { sdf.parse(it.date) } catch (_: Exception) { Date(0) } }
            SortOrder.DESCENDING -> filteredList.sortedByDescending { try { sdf.parse(it.date) } catch (_: Exception) { Date(0) } }
            SortOrder.NONE -> filteredList
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                actions = {
                    IconButton(onClick = {
                        onRefresh()
                        showCalendarDialog = true
                    }) {
                        Icon(Icons.Default.CalendarToday, "Calendar")
                    }
                    IconButton(onClick = {
                        sortOrder = when (sortOrder) {
                            SortOrder.NONE -> SortOrder.ASCENDING
                            SortOrder.ASCENDING -> SortOrder.DESCENDING
                            SortOrder.DESCENDING -> SortOrder.NONE
                        }
                        val message = when (sortOrder) {
                            SortOrder.ASCENDING -> "Sorted by date (Oldest first)"
                            SortOrder.DESCENDING -> "Sorted by date (Newest first)"
                            SortOrder.NONE -> "Sort cleared"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, "Sort by date")
                    }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { showAddEventDialog = true }) { Icon(Icons.Filled.Add, "Add Event") } }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).nestedScroll(pullRefreshState.nestedScrollConnection)) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StyledSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { showFilterDialog = true },
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter Events")
                    }
                }

                LazyColumn(contentPadding = PaddingValues(bottom = 8.dp)) {
                    if (displayedEvents.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No events found. Pull down to refresh.", color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                    items(displayedEvents, key = { it.id }) { event ->
                        Box(modifier = Modifier.animateItemPlacement()) {
                            EventListItem(event, onEventClick, onEditEvent, onDeleteEvent)
                        }
                    }
                }
            }
            PullToRefreshContainer(
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showFilterDialog) {
        FilterDialog(
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            yearOptions = yearList,
            monthOptions = monthList,
            onDismiss = { showFilterDialog = false },
            onApply = { year, month ->
                selectedYear = year
                selectedMonth = month
                showFilterDialog = false
            }
        )
    }

    if (showAddEventDialog) {
        AddEventDialog(onDismiss = { showAddEventDialog = false }, onConfirm = { title, date, cName, cPhone -> onAddEvent(title, date, cName, cPhone); showAddEventDialog = false })
    }

    if (showCalendarDialog) {
        EventCalendarDialog(
            events = events,
            onDismiss = { showCalendarDialog = false },
            onRefresh = onRefresh
        )
    }
}

// --- CALENDAR COMPONENT ---
@Composable
fun EventCalendarDialog(
    events: List<Event>,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    var calendarInstance by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val sdf = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    val eventDatesMap = remember(events) {
        val map = mutableMapOf<String, MutableList<String>>()
        events.forEach { event ->
            val displayName = if (event.customerName.isNotBlank()) {
                "${event.customerName} - ${event.title}"
            } else {
                event.title
            }

            map.getOrPut(event.date) { mutableListOf() }.add(displayName)

            event.days.forEach { day ->
                map.getOrPut(day.date) { mutableListOf() }.add("$displayName (${day.title})")
            }
        }
        map
    }

    var selectedDateEvents by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDateString by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val newCal = calendarInstance.clone() as Calendar
                            newCal.add(Calendar.YEAR, -1)
                            calendarInstance = newCal
                            onRefresh()
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.KeyboardDoubleArrowLeft, "Previous Year")
                        }

                        IconButton(onClick = {
                            val newCal = calendarInstance.clone() as Calendar
                            newCal.add(Calendar.MONTH, -1)
                            calendarInstance = newCal
                            onRefresh()
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous Month")
                        }
                    }

                    Text(
                        text = monthYearFormat.format(calendarInstance.time),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val newCal = calendarInstance.clone() as Calendar
                            newCal.add(Calendar.MONTH, 1)
                            calendarInstance = newCal
                            onRefresh()
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next Month")
                        }

                        IconButton(onClick = {
                            val newCal = calendarInstance.clone() as Calendar
                            newCal.add(Calendar.YEAR, 1)
                            calendarInstance = newCal
                            onRefresh()
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.KeyboardDoubleArrowRight, "Next Year")
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                        Text(it, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val daysInMonth = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
                val firstDayOfWeek = calendarInstance.get(Calendar.DAY_OF_WEEK) - 1
                val totalCells = daysInMonth + firstDayOfWeek
                val rows = kotlin.math.ceil(totalCells / 7.0).toInt()

                var dayCounter = 1

                Column {
                    for (i in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (j in 0..6) {
                                if (i == 0 && j < firstDayOfWeek) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else if (dayCounter <= daysInMonth) {
                                    val dayToRender = dayCounter
                                    val cellCal = calendarInstance.clone() as Calendar
                                    cellCal.set(Calendar.DAY_OF_MONTH, dayToRender)
                                    val dateStr = sdf.format(cellCal.time)
                                    val hasEvent = eventDatesMap.containsKey(dateStr)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(if (hasEvent) MaterialTheme.colorScheme.primaryContainer else ComposeColor.Transparent)
                                            .clickable {
                                                selectedDateString = dateStr
                                                selectedDateEvents = eventDatesMap[dateStr] ?: emptyList()
                                                onRefresh()
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = dayToRender.toString(),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (hasEvent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                                fontWeight = if (hasEvent) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (hasEvent) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                    dayCounter++
                                } else {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                }
                            }
                        }
                    }
                }

                if (selectedDateEvents.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text("Events on $selectedDateString:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                        items(selectedDateEvents) { eventTitle ->
                            Text(
                                "• $eventTitle",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                } else if (selectedDateString.isNotEmpty()) {
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    Text("No events on $selectedDateString.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun StyledSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text("Search...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentYear: String,
    currentMonth: String,
    yearOptions: List<String>,
    monthOptions: List<String>,
    onDismiss: () -> Unit,
    onApply: (year: String, month: String) -> Unit
) {
    var tempYear by remember { mutableStateOf(currentYear) }
    var tempMonth by remember { mutableStateOf(currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Filter Events", style = MaterialTheme.typography.titleLarge)

                FilterDropdown(
                    label = "Year",
                    selectedValue = tempYear,
                    options = yearOptions,
                    onValueChange = {
                        tempYear = it
                        if (it == "Year") {
                            tempMonth = "Month"
                        }
                    }
                )

                FilterDropdown(
                    label = "Month",
                    selectedValue = tempMonth,
                    options = monthOptions,
                    onValueChange = { tempMonth = it },
                    enabled = tempYear != "Year"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        tempYear = "Year"
                        tempMonth = "Month"
                    }) {
                        Text("Clear")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onApply(tempYear, tempMonth) }) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropdown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakersDashboardScreen(events: List<Event>, onTakerUpdated: (eventId: Long, dayId: Long, updatedTaker: Taker) -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Grouped by Day", "Grouped by Taker")

    Scaffold(topBar = {
        TopAppBar(title = { Text("Takers Dashboard") })
    }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTabIndex) {
                0 -> GroupedByDayTakersView(events = events, onTakerUpdated = onTakerUpdated)
                1 -> GroupedByTakerView(events = events, onTakerUpdated = onTakerUpdated)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedByTakerView(events: List<Event>, onTakerUpdated: (eventId: Long, dayId: Long, updatedTaker: Taker) -> Unit) {
    val context = LocalContext.current

    val allTakersGrouped = remember(events.toList(), context) {
        val dataStore = DataStore(context)
        val masterTakersMap = dataStore.loadMasterTakers().associateBy { it.name }

        val flattenedTakers = try {
            events.flatMap { event ->
                (event.days).flatMap { day ->
                    (day.takers).mapNotNull { taker ->
                        val masterTaker = masterTakersMap[taker.name]
                        val phone = masterTaker?.defaultPhone ?: ""

                        Triple(
                            taker.name,
                            phone,
                            TakerEventSummary(
                                eventId = event.id,
                                eventTitle = event.title,
                                dayTitle = day.title,
                                dayDate = day.date,
                                totalDue = taker.totalDue,
                                amountPaid = taker.amountPaid
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GroupedByTakerView", "Error flattening taker data", e)
            emptyList()
        }

        flattenedTakers.groupBy { it.first }
            .mapValues { (_, summaries) ->
                val totalDue = summaries.sumOf { it.third.totalDue }
                val totalPaid = summaries.sumOf { it.third.amountPaid }
                val phone = summaries.firstOrNull()?.second ?: ""

                Triple(
                    totalDue,
                    totalPaid,
                    Pair(phone, summaries.map { it.third })
                )
            }
            .toList().sortedBy { it.first }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (allTakersGrouped.isEmpty()) {
            item {
                Text("No takers added to any event yet.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(allTakersGrouped) { (takerName, summary) ->
            val (totalDue, totalPaid, phoneAndSummaries) = summary
            val (phone, eventSummaries) = phoneAndSummaries
            val balance = totalDue - totalPaid

            ExpandableTakerSummaryCard(
                takerName = takerName,
                totalBalance = balance,
                takerPhone = phone,
                eventSummaries = eventSummaries,
                context = context
            )
        }
    }
}

private fun generateTakerSummaryText(takerName: String, totalBalance: Double, eventSummaries: List<TakerEventSummary>): String {
    val totalDue = eventSummaries.sumOf { it.totalDue }
    val totalPaid = eventSummaries.sumOf { it.amountPaid }

    val details = eventSummaries.joinToString(separator = "\n") { summary ->
        "  - ${summary.eventTitle} (${summary.dayDate}): Due ₹${String.format(Locale.getDefault(), "%,.2f", summary.totalDue)}, Paid ₹${String.format(Locale.getDefault(), "%,.2f", summary.amountPaid)}"
    }

    return """
        *Taker Financial Summary*
        Taker: $takerName
        
        Details:
        $details
        
        Total Due: ₹${String.format(Locale.getDefault(), "%,.2f", totalDue)}
        Amount Paid: ₹${String.format(Locale.getDefault(), "%,.2f", totalPaid)}
        *Balance Outstanding: ₹${String.format(Locale.getDefault(), "%,.2f", totalBalance)}*
        
        Shared via Supriya Digital Producer App.
    """.trimIndent()
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableTakerSummaryCard(
    takerName: String,
    totalBalance: Double,
    takerPhone: String,
    eventSummaries: List<TakerEventSummary>,
    context: Context
) {
    var expanded by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }

    val summaryText = remember(takerName, totalBalance, eventSummaries) {
        generateTakerSummaryText(takerName, totalBalance, eventSummaries)
    }

    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager }

    Card(modifier = Modifier
        .fillMaxWidth()
        .animateContentSize()
        .combinedClickable(
            onClick = { expanded = !expanded },
            onLongClick = { showContextMenu = true }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            takerName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (takerPhone.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, "tel:$takerPhone".toUri()))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Filled.Phone, "Call $takerName", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Text(
                        "Total Balance: ₹${String.format(Locale.getDefault(), "%,.2f", totalBalance)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (totalBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(24.dp)
                )
            }

            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    eventSummaries.forEach { summary ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                summary.eventTitle,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${summary.dayTitle} (${summary.dayDate})", style = MaterialTheme.typography.bodyMedium)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Due: ₹${String.format(Locale.getDefault(), "%,.2f", summary.totalDue)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Paid: ₹${String.format(Locale.getDefault(), "%,.2f", summary.amountPaid)}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showContextMenu) {
        AlertDialog(
            onDismissRequest = { showContextMenu = false },
            title = { Text("Share Taker Summary") },
            text = { Text("Select an action for the summary of ${takerName}.") },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, summaryText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Taker Summary"))
                        showContextMenu = false
                    }) {
                        Text("Share")
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        val clip = android.content.ClipData.newPlainText("Taker Summary", summaryText)
                        clipboardManager?.setPrimaryClip(clip)
                        Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                        showContextMenu = false
                    }) {
                        Text("Copy")
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showContextMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GroupedByDayTakersView(events: List<Event>, onTakerUpdated: (eventId: Long, dayId: Long, updatedTaker: Taker) -> Unit) {
    var editingTakerInfo by remember { mutableStateOf<EditingTakerInfo?>(null) }
    val eventsWithTakers = remember(events.toList()) { events.filter { event -> event.days.any { it.takers.isNotEmpty() } } }
    val dataStore = DataStore(LocalContext.current)
    val masterTakers = remember { dataStore.loadMasterTakers() }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        eventsWithTakers.forEach { event ->
            item(key = event.id) {
                Text(text = event.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp))
            }
            val daysWithTakers = event.days.filter { it.takers.isNotEmpty() }
            items(daysWithTakers, key = { it.id }) { day ->
                ExpandableDayTakerCard(event = event, day = day, onEditTaker = { taker -> editingTakerInfo = EditingTakerInfo(taker, event.id, day.id) })
            }
        }
    }

    if (editingTakerInfo != null) {
        AddEditTakerDialog(
            taker = editingTakerInfo!!.taker,
            masterTakers = masterTakers,
            onDismiss = { editingTakerInfo = null },
            onConfirm = { updatedTaker ->
                onTakerUpdated(editingTakerInfo!!.eventId, editingTakerInfo!!.dayId, updatedTaker)
                editingTakerInfo = null
            },
            onUpdateMasterTaker = null
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpandableDayTakerCard(event: Event, day: Day, onEditTaker: (Taker) -> Unit) {
    var expanded by remember { mutableStateOf(true) }

    Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "${day.title} (${day.date})", style = MaterialTheme.typography.titleLarge)
                    Text(event.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                }
                Icon(
                    if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(24.dp)
                )
            }

            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                day.takers.forEach { taker ->
                    TakerItemCard(taker = taker, onEditClick = { onEditTaker(taker) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    event: Event, albumSettings: AlbumSettings, itemList: List<MasterItem>,
    onUpdatePayments: (List<Payment>) -> Unit, onUpdateDiscount: (Double) -> Unit,
    onAddDay: (title: String, date: String) -> Unit, onEditDay: (dayId: Long, newTitle: String, newDate: String) -> Unit, onDeleteDay: (dayId: Long) -> Unit,
    onAddEventItem: (Item) -> Unit, onAddDayItem: (dayId: Long, name: String, cost: Double, quantity: Int) -> Unit,
    onEditItem: (itemId: Long, dayId: Long?, newName: String, newCost: Double) -> Unit,
    onUpdateItemQuantity: (itemId: Long, dayId: Long?, newQuantity: Int) -> Unit, onDeleteItem: (itemId: Long, dayId: Long?) -> Unit,
    onBack: () -> Unit, onManageTakersForDay: (dayId: Long) -> Unit, onManageAllTakers: () -> Unit,
    onShowInternalExpenses: () -> Unit,
    onUpdateEventStatus: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showAddDayDialog by remember { mutableStateOf(false) }
    var showAddAlbumDialog by remember { mutableStateOf(false) }
    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var editingPayment by remember { mutableStateOf<Payment?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    val sdf = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    val sortedDays = remember(event.days.toList()) {
        event.days.sortedBy { day -> try { sdf.parse(day.date) } catch (_: Exception) { Date(0) } }
    }

    val totalCost = remember(event) { event.eventItems.sumOf { it.cost * it.quantity } + event.days.sumOf { day -> day.items.sumOf { item -> item.cost * item.quantity } } }
    val totalPaid = remember(event) { event.payments.sumOf { it.amount } }
    val remainingBalance = totalCost - event.discount - totalPaid

    val handlePaymentUpdate: (Payment?, isDelete: Boolean) -> Unit = { updatedPayment, isDelete ->
        val currentPayments = event.payments.toMutableList()
        if (isDelete && updatedPayment != null) {
            currentPayments.removeIf { it.id == updatedPayment.id }
        } else if (updatedPayment != null) {
            val index = currentPayments.indexOfFirst { it.id == updatedPayment.id }
            if (index != -1) {
                currentPayments[index] = updatedPayment
            }
        }
        onUpdatePayments(currentPayments)
    }

    val currentAlbumSettings = albumSettings

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(event.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        if(event.isConfirmed) {
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Filled.CheckCircle, "Confirmed", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        "Balance: ₹${String.format(Locale.getDefault(), "%,.2f", remainingBalance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (remainingBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                } },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = onShowInternalExpenses) { Icon(Icons.Filled.AccountTree, "Internal Expenses") }

                    if (!isEditing) {
                        IconButton(onClick = { showDownloadDialog = true }) {
                            Icon(Icons.Filled.Download, "Download Bill")
                        }
                    }
                    IconButton(onClick = onManageAllTakers) { Icon(Icons.Filled.Groups, contentDescription = "All Takers Summary") }
                }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { isEditing = !isEditing }) { if (isEditing) Icon(Icons.Filled.Done, "Done Editing") else Icon(Icons.Filled.Edit, "Edit Event") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (sortedDays.isEmpty() && event.eventItems.isEmpty() && !isEditing) {
                item {
                    Column(
                        modifier = Modifier.fillParentMaxSize().padding(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Info, "Info", modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Text("No details for this event yet.", style = MaterialTheme.typography.titleMedium)
                        Text("Tap the edit button (✎) to add information.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }

            if (isEditing) { item { EditActionsRow(onAddPaymentClick = { showAddPaymentDialog = true }, onAddAlbumClick = { showAddAlbumDialog = true }, onAddDayClick = { showAddDayDialog = true }, onAddItemClick = { showAddItemDialog = true }) } }

            if (event.payments.isNotEmpty() || event.discount > 0 || isEditing) {
                item {
                    FinancialSummaryCard(
                        payments = event.payments,
                        totalCost = totalCost,
                        discount = event.discount,
                        isEditing = isEditing,
                        onEditDiscountClick = { showDiscountDialog = true },
                        onEditPaymentClick = { payment -> editingPayment = payment }
                    )
                }
            }

            items(items = sortedDays, key = { it.id }) { day ->
                DayItemCard(day, isEditing, itemList, onEditDay, onDeleteDay,
                    onAddItem = { name, cost, qty -> onAddDayItem(day.id, name, cost, qty) },
                    onEditItem = { itemId, newName, newCost -> onEditItem(itemId, day.id, newName, newCost) },
                    onUpdateItemQuantity = { itemId, newQty -> onUpdateItemQuantity(itemId, day.id, newQty) },
                    onDeleteItem = { itemId -> onDeleteItem(itemId, day.id) },
                    onManageTakers = { onManageTakersForDay(day.id) })
            }
            item {
                EventItemsCard(event.eventItems, isEditing,
                    onEditItem = { itemId, newName, newCost -> onEditItem(itemId, null, newName, newCost) },
                    onUpdateItemQuantity = { itemId, newQty -> onUpdateItemQuantity(itemId, null, newQty) },
                    onDeleteItem = { itemId -> onDeleteItem(itemId, null) })
            }
        }
    }

    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Select Document Type") },
            text = { Text("Generate a formal Invoice (confirms event) or a Bill Analysis (estimate)?") },
            confirmButton = {
                Button(onClick = {
                    createEventBillPdf(context, event, isFinalInvoice = true)
                    onUpdateEventStatus(true)
                    showDownloadDialog = false
                }) {
                    Text("Invoice (Confirm)")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showDownloadDialog = false }) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(onClick = {
                        createEventBillPdf(context, event, isFinalInvoice = false)
                        showDownloadDialog = false
                    }) {
                        Text("Bill Analysis")
                    }
                }
            }
        )
    }

    if (showAddDayDialog) { AddDayDialog(onDismiss = { showAddDayDialog = false }, onConfirm = { title, date -> onAddDay(title, date); showAddDayDialog = false }) }

    if (showAddAlbumDialog) {
        AddAlbumDialog(
            settings = currentAlbumSettings,
            onDismiss = { showAddAlbumDialog = false },
            onConfirm = { item -> onAddEventItem(item); showAddAlbumDialog = false }
        )
    }

    if (showAddPaymentDialog) {
        AddPaymentDialog(onDismiss = { showAddPaymentDialog = false }, onConfirm = { title, amount, date ->
            onUpdatePayments(event.payments.toMutableList().apply { add(Payment(System.currentTimeMillis(), title, amount, date)) })

            if (amount > 0 && !event.isConfirmed) {
                onUpdateEventStatus(true)
                Toast.makeText(context, "Payment received! Event marked as Confirmed.", Toast.LENGTH_SHORT).show()
            }

            showAddPaymentDialog = false
        })
    }

    if (editingPayment != null) {
        EditPaymentDialog(
            payment = editingPayment!!,
            onDismiss = { editingPayment = null },
            onSave = { updatedPayment -> handlePaymentUpdate(updatedPayment, false); editingPayment = null },
            onDelete = { deletedPayment -> handlePaymentUpdate(deletedPayment, true); editingPayment = null }
        )
    }

    if (showAddItemDialog) { AddItemDialog(itemList, onDismiss = { showAddItemDialog = false }, onConfirm = { name, cost -> onAddEventItem(Item(System.currentTimeMillis(), name, cost, 1)); showAddItemDialog = false }) }
    if (showDiscountDialog) { AddEditDiscountDialog(event.discount, onDismiss = { showDiscountDialog = false }, onConfirm = { newDiscount -> onUpdateDiscount(newDiscount); showDiscountDialog = false }) }
}

@Composable
fun FinancialSummaryCard(payments: List<Payment>, totalCost: Double, discount: Double, isEditing: Boolean, onEditDiscountClick: () -> Unit, onEditPaymentClick: (Payment) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Financial Summary", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            ListItem(
                headlineContent = { Text("Sub-Total") },
                trailingContent = { Text("₹${String.format(Locale.getDefault(), "%,.2f", totalCost)}", fontWeight = FontWeight.Medium) },
                modifier = Modifier.height(40.dp)
            )

            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Discount")
                        if (isEditing) Icon(Icons.Filled.Edit, "Edit", modifier = Modifier.size(16.dp).padding(start = 4.dp))
                    }
                },
                trailingContent = { Text("- ₹${String.format(Locale.getDefault(), "%,.2f", discount)}", color = if (discount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                modifier = Modifier.clickable(enabled = isEditing, onClick = onEditDiscountClick).height(40.dp)
            )

            val grandTotal = totalCost - discount
            ListItem(
                headlineContent = { Text("Grand Total", fontWeight = FontWeight.Bold) },
                trailingContent = { Text("₹${String.format(Locale.getDefault(), "%,.2f", grandTotal)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                modifier = Modifier.height(40.dp),
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            )

            HorizontalDivider(Modifier.padding(top = 8.dp))
            Text("Payments Received:", style = MaterialTheme.typography.titleMedium)

            payments.forEach { payment ->
                ListItem(
                    headlineContent = { Text(payment.title) },
                    supportingContent = { Text(payment.date, style = MaterialTheme.typography.bodySmall) },
                    trailingContent = { Text("- ₹${String.format(Locale.getDefault(), "%,.2f", payment.amount)}", fontWeight = FontWeight.Medium) },
                    modifier = Modifier.clickable(enabled = isEditing, onClick = { onEditPaymentClick(payment) }),
                    colors = ListItemDefaults.colors(containerColor = ComposeColor.Transparent)
                )
            }

            val totalPaid = payments.sumOf { it.amount }
            val balanceDue = grandTotal - totalPaid
            HorizontalDivider(Modifier.padding(top = 8.dp))
            ListItem(
                headlineContent = { Text("Balance Due", fontWeight = FontWeight.ExtraBold) },
                trailingContent = {
                    Text(
                        String.format(Locale.getDefault(), "₹%,.2f", balanceDue),
                        fontWeight = FontWeight.ExtraBold,
                        color = if (balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakerPaymentScreen(
    day: Day,
    masterTakers: List<MasterTaker>,
    onBack: () -> Unit,
    onTakerUpdated: () -> Unit,
    onMasterTakerUpdated: (MasterTaker) -> Unit
) {
    var showTakerDialog by remember { mutableStateOf<Taker?>(null) }
    var takerToDelete by remember { mutableStateOf<Taker?>(null) }

    val handleDeleteTaker: (Taker) -> Unit = { taker ->
        day.takers.removeIf { it.id == taker.id }
        onTakerUpdated()
        takerToDelete = null
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("${day.title} - Takers") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showTakerDialog = Taker(System.currentTimeMillis(), "", 0.0, 0.0)
            }) {
                Icon(Icons.Filled.Add, "Add Taker")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(day.takers, key = { it.id }) { taker ->
                TakerItemCard(
                    taker = taker,
                    date = day.date,
                    onEditClick = { showTakerDialog = taker },
                    onDeleteClick = { takerToDelete = taker }
                )
            }
        }
    }

    if (showTakerDialog != null) {
        AddEditTakerDialog(
            taker = showTakerDialog!!,
            masterTakers = masterTakers,
            onDismiss = { showTakerDialog = null },
            onConfirm = { updatedTaker ->
                val index = day.takers.indexOfFirst { it.id == updatedTaker.id }
                if (index != -1) { day.takers[index] = updatedTaker } else { day.takers.add(updatedTaker) }
                onTakerUpdated()
                showTakerDialog = null
            },
            onUpdateMasterTaker = onMasterTakerUpdated
        )
    }

    if (takerToDelete != null) {
        ConfirmDeleteDialog(
            title = "Delete Taker",
            text = "Are you sure you want to remove '${takerToDelete!!.name}' from this day?",
            onDismiss = { takerToDelete = null },
            onConfirm = { handleDeleteTaker(takerToDelete!!) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTakersSummaryScreen(event: Event, onBack: () -> Unit, onAddTakerToDays: (name: String, totalDue: Double, amountPaid: Double, dayIds: List<Long>) -> Unit) {
    var showAddTakerDialog by remember { mutableStateOf(false) }
    val daysWithTakers = remember(event.days.toList().map { it.takers.size }) { event.days.filter { it.takers.isNotEmpty() } }
    val grandTotalDue = remember(daysWithTakers) { event.days.sumOf { day -> day.takers.sumOf { it.totalDue } } }
    Scaffold(
        topBar = { TopAppBar(title = { Text("${event.title} - Takers") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }, actions = { Text("Total Due: ₹${String.format(Locale.getDefault(), "%,.2f", grandTotalDue)}", modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold) }) },
        floatingActionButton = { FloatingActionButton(onClick = { showAddTakerDialog = true }) { Icon(Icons.Filled.Add, "Add Taker") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(daysWithTakers) { day ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(text = "${day.title} (${day.date})", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
                        day.takers.forEach { taker -> TakerItemCard(taker = taker, onEditClick = { }); Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
    if (showAddTakerDialog) { AddTakerToEventDialog(event = event, onDismiss = { showAddTakerDialog = false }, onConfirm = { name, totalDue, amountPaid, dayIds -> onAddTakerToDays(name, totalDue, amountPaid, dayIds); showAddTakerDialog = false }) }
}
// Part - 2
@Composable
fun EditActionsRow(onAddPaymentClick: () -> Unit, onAddAlbumClick: () -> Unit, onAddDayClick: () -> Unit, onAddItemClick: () -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onAddDayClick, modifier = Modifier.weight(1f)) { Text("Add Day") }
            FilledTonalButton(onClick = onAddItemClick, modifier = Modifier.weight(1f)) { Text("Add Item") }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onAddAlbumClick, modifier = Modifier.weight(1f)) { Text("Add Album") }
            FilledTonalButton(onClick = onAddPaymentClick, modifier = Modifier.weight(1f)) { Text("Add Payment") }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventListItem(
    event: Event, onClick: (Event) -> Unit,
    onEditEvent: (eventId: Long, newTitle: String, newDate: String, newCustomerName: String, newCustomerPhone: String) -> Unit,
    onDeleteEvent: (eventId: Long) -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    val showEditDialog = remember { mutableStateOf(false) }
    val showDeleteConfirm = remember { mutableStateOf(false) }

    val totalCost = remember(event) { event.eventItems.sumOf { it.cost * it.quantity } + event.days.sumOf { day -> day.items.sumOf { item -> item.cost * item.quantity } } }
    val totalPaid = remember(event) { event.payments.sumOf { it.amount } }
    val balance = totalCost - event.discount - totalPaid

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).combinedClickable(onClick = { if (showActions) showActions = false else onClick(event) }, onLongClick = { showActions = true })) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(event.title, style = MaterialTheme.typography.titleMedium)
                    if (event.isConfirmed) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Confirmed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(event.customerName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(event.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                if (event.customerPhone.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val context = LocalContext.current
                        IconButton(onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${event.customerPhone}".toUri()))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Could not open dialer.", Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Phone, "Call", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Text(event.customerPhone, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Balance: ₹${String.format(Locale.getDefault(), "%,.2f", balance)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            if (showActions) {
                Row {
                    IconButton(onClick = { showEditDialog.value = true; showActions = false }) { Icon(Icons.Filled.Edit, "Edit") }
                    IconButton(onClick = { showDeleteConfirm.value = true; showActions = false }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
        }
    }
    if (showEditDialog.value) EditEventDialog(event,
        onDismiss = { showEditDialog.value = false },
        onConfirm = { newTitle, newDate, newCName, newCPhone ->
            onEditEvent(event.id, newTitle, newDate, newCName, newCPhone)
            showEditDialog.value = false
        }
    )
    if (showDeleteConfirm.value) ConfirmDeleteDialog("Move to Trash", "Are you sure you want to move '${event.title}' to the Trash Bin?",
        onDismiss = { showDeleteConfirm.value = false },
        onConfirm = { onDeleteEvent(event.id); showDeleteConfirm.value = false }
    )
}

@Composable
fun EventItemsCard(eventItems: List<Item>, isEditing: Boolean, onEditItem: (itemId: Long, newName: String, newCost: Double) -> Unit, onUpdateItemQuantity: (itemId: Long, newQuantity: Int) -> Unit, onDeleteItem: (itemId: Long) -> Unit) {
    if (eventItems.isNotEmpty()) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Event Items & Services", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                eventItems.forEach { item ->
                    key(item.id, item.quantity) {
                        ItemRow(item, isEditing,
                            onEdit = { newName, newCost -> onEditItem(item.id, newName, newCost) },
                            onUpdateQuantity = { newQty -> onUpdateItemQuantity(item.id, newQty) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun DayItemCard(day: Day, isEditing: Boolean, itemList: List<MasterItem>,
                onEditDay: (dayId: Long, newTitle: String, newDate: String) -> Unit,
                onDeleteDay: (dayId: Long) -> Unit,
                onAddItem: (name: String, cost: Double, quantity: Int) -> Unit,
                onEditItem: (itemId: Long, newName: String, newCost: Double) -> Unit,
                onUpdateItemQuantity: (itemId: Long, newQuantity: Int) -> Unit,
                onDeleteItem: (itemId: Long) -> Unit,
                onManageTakers: () -> Unit) {
    var showEditDayDialog by remember { mutableStateOf(false) }
    var showDeleteDayDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column {
                    Text(day.title, style = MaterialTheme.typography.titleLarge)
                    Text(day.date, style = MaterialTheme.typography.bodyMedium)
                }
                if (isEditing) {
                    Row {
                        IconButton(onClick = { showEditDayDialog = true }) { Icon(Icons.Filled.Edit, "Edit Day") }
                        IconButton(onClick = { showDeleteDayDialog = true }) { Icon(Icons.Filled.Delete, "Delete Day") }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (day.items.isEmpty()) {
                Text("No items for this day.", modifier = Modifier.padding(vertical = 8.dp))
            } else {
                day.items.forEach { item ->
                    key(item.id, item.quantity) {
                        ItemRow(item, isEditing,
                            onEdit = { newName, newCost -> onEditItem(item.id, newName, newCost) },
                            onUpdateQuantity = { newQty -> onUpdateItemQuantity(item.id, newQty) },
                            onDelete = { onDeleteItem(item.id) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isEditing) {
                    Button(onClick = { showAddItemDialog = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.AutoMirrored.Filled.PlaylistAdd, "Add Item", modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Add Item")
                    }
                }
                OutlinedButton(onClick = onManageTakers, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Groups, "Manage Takers", modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Takers")
                }
            }
        }
    }

    if (showEditDayDialog) EditDayDialog(day, onDismiss = { showEditDayDialog = false }, onConfirm = { newTitle, newDate -> onEditDay(day.id, newTitle, newDate); showEditDayDialog = false })
    if (showDeleteDayDialog) ConfirmDeleteDialog("Delete Day", "Delete '${day.title}'?", onDismiss = { showDeleteDayDialog = false }, onConfirm = { onDeleteDay(day.id); showDeleteDayDialog = false })
    if (showAddItemDialog) AddItemDialog(itemList, onDismiss = { showAddItemDialog = false }, onConfirm = { name, cost -> onAddItem(name, cost, 1); showAddItemDialog = false })
}

@Composable
fun ItemRow(item: Item, isEditing: Boolean, onEdit: (String, Double) -> Unit, onUpdateQuantity: (Int) -> Unit, onDelete: () -> Unit) {
    var showEditDialog by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Medium)
            Text("Cost: ₹${item.cost} Qty: ${item.quantity}", style = MaterialTheme.typography.bodySmall)
        }
        if (isEditing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onUpdateQuantity(item.quantity - 1) }, enabled = item.quantity > 1) { Icon(Icons.Filled.RemoveCircleOutline, "Decrease") }
                Text(item.quantity.toString())
                IconButton(onClick = { onUpdateQuantity(item.quantity + 1) }) { Icon(Icons.Filled.AddCircleOutline, "Increase") }
                IconButton(onClick = { showEditDialog = true }) { Icon(Icons.Filled.Edit, "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
            }
        }
    }
    if (showEditDialog) {
        var name by remember { mutableStateOf(item.name) }
        var cost by remember { mutableStateOf(item.cost.toString()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Item") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") })
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Item Cost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            },
            confirmButton = { Button(onClick = { onEdit(name, cost.toDoubleOrNull() ?: item.cost); showEditDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TakerItemCard(taker: Taker, date: String? = null, onEditClick: () -> Unit, onDeleteClick: (() -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(taker.name, style = MaterialTheme.typography.titleMedium)
                date?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                val balance = taker.totalDue - taker.amountPaid
                Text(
                    "Balance: ₹${String.format(Locale.getDefault(), "%,.2f", balance)}",
                    fontWeight = FontWeight.Bold,
                    color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onEditClick) { Icon(Icons.Filled.Edit, "Edit Taker") }
            onDeleteClick?.let {
                IconButton(onClick = it) { Icon(Icons.Filled.Delete, "Delete Taker", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

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
    onUpdateAlbumSettings: (AlbumSettings) -> Unit,
    onRestoreEvent: (Event) -> Unit,
    onPermanentlyDeleteEvent: (Event) -> Unit
) {
    var baseCost by remember { mutableStateOf(initialAlbumSettings.baseCost.toString()) }
    var defaultSheets by remember { mutableStateOf(initialAlbumSettings.defaultSheets.toString()) }
    var extraSheetCost by remember { mutableStateOf(initialAlbumSettings.extraSheetCost.toString()) }

    val localItemList = remember { mutableStateListOf<MasterItem>().also { it.addAll(initialMasterItems) } }
    val localMasterTakerList = remember { mutableStateListOf<MasterTaker>().also { it.addAll(initialMasterTakers) } }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MasterItem?>(null) }
    var showAddTakerDialog by remember { mutableStateOf(false) }
    var editingTaker by remember { mutableStateOf<MasterTaker?>(null) }
    var showTrashBinDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val totalDocuments = initialMasterItems.size + initialMasterTakers.size + eventsCount
    val maxFreeTierDocs = 50000
    val storagePercentage = (totalDocuments.toFloat() / maxFreeTierDocs.toFloat()).coerceIn(0f, 1f)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // --- CLOUD STORAGE DASHBOARD ---
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cloud, contentDescription = "Cloud Storage")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cloud Storage Status", style = MaterialTheme.typography.titleMedium)
                    }
                    Text("Current usage against Firebase free tier daily limit (Approx).", style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { storagePercentage },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = if (storagePercentage > 0.8f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        strokeCap = StrokeCap.Round
                    )
                    Text("$totalDocuments / 50,000 documents", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }

            // --- TRASH BIN ---
            Button(
                onClick = { showTrashBinDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Trash Bin")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Trash Bin (${deletedEvents.size})")
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("Album Settings", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = baseCost, onValueChange = { baseCost = it }, label = { Text("Base Album Cost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = defaultSheets, onValueChange = { defaultSheets = it }, label = { Text("Default Sheets") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(value = extraSheetCost, onValueChange = { extraSheetCost = it }, label = { Text("Extra Sheet Cost") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Button(onClick = {
                onUpdateAlbumSettings(AlbumSettings(
                    baseCost.toDoubleOrNull() ?: initialAlbumSettings.baseCost,
                    defaultSheets.toIntOrNull() ?: initialAlbumSettings.defaultSheets,
                    extraSheetCost.toDoubleOrNull() ?: initialAlbumSettings.extraSheetCost
                ))
                Toast.makeText(context, "Album settings saved", Toast.LENGTH_SHORT).show()
            }) { Text("Save Album Settings") }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text("Master Taker List", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Column(Modifier.padding(8.dp)) {
                    localMasterTakerList.forEach { taker ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(taker.name, fontWeight = FontWeight.Medium)
                                Text("Due: ₹${String.format(Locale.getDefault(), "%,.2f", taker.defaultTotalDue)} / Phone: ${taker.defaultPhone}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { editingTaker = taker }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Taker")
                            }
                            IconButton(onClick = {
                                localMasterTakerList.remove(taker)
                                onUpdateMasterTakers(localMasterTakerList.toList())
                                Toast.makeText(context, "${taker.name} deleted", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Taker")
                            }
                        }
                        HorizontalDivider()
                    }
                    if (localMasterTakerList.isEmpty()) {
                        Text(
                            "No master takers found. Add one to get started!",
                            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showAddTakerDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add New Taker to Master List")
            }

            HorizontalDivider(Modifier.padding(vertical = 16.dp))

            Text("Master Item List", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            Card {
                Column(Modifier.padding(8.dp)) {
                    localItemList.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Medium)
                                Text(
                                    "₹${String.format(Locale.getDefault(), "%,.2f", item.cost)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { editingItem = item }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Item")
                            }
                            IconButton(onClick = {
                                localItemList.remove(item)
                                onUpdateMasterItems(localItemList.toList())
                                Toast.makeText(context, "${item.name} deleted", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Item")
                            }
                        }
                        HorizontalDivider()
                    }
                    if (localItemList.isEmpty()) {
                        Text(
                            "No master items found. Add one to get started!",
                            modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { showAddItemDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Add New Item to Master List")
            }
        }
    }

    if (showTrashBinDialog) {
        TrashBinDialog(
            deletedEvents = deletedEvents,
            onRestore = onRestoreEvent,
            onPermanentlyDelete = onPermanentlyDeleteEvent,
            onDismiss = { showTrashBinDialog = false }
        )
    }

    if (showAddItemDialog) {
        AddEditMasterItemDialog(
            onDismiss = { showAddItemDialog = false },
            onConfirm = { name, cost ->
                val newItem = MasterItem(System.currentTimeMillis(), name, cost)
                localItemList.add(newItem)
                onUpdateMasterItems(localItemList.toList())
                showAddItemDialog = false
                Toast.makeText(context, "$name added", Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingItem?.let { itemToEdit ->
        AddEditMasterItemDialog(
            item = itemToEdit,
            onDismiss = { editingItem = null },
            onConfirm = { name, cost ->
                val index = localItemList.indexOfFirst { it.id == itemToEdit.id }
                if (index != -1) {
                    localItemList[index] = itemToEdit.copy(name = name, cost = cost)
                    onUpdateMasterItems(localItemList.toList())
                    Toast.makeText(context, "Item updated", Toast.LENGTH_SHORT).show()
                }
                editingItem = null
            }
        )
    }

    if (showAddTakerDialog) {
        AddEditMasterTakerDialog(
            onDismiss = { showAddTakerDialog = false },
            onConfirm = { name, due, phone ->
                val newTaker = MasterTaker(System.currentTimeMillis(), name, due, phone)
                localMasterTakerList.add(newTaker)
                onUpdateMasterTakers(localMasterTakerList.toList())
                showAddTakerDialog = false
                Toast.makeText(context, "$name added to Master Takers", Toast.LENGTH_SHORT).show()
            }
        )
    }

    editingTaker?.let { takerToEdit ->
        AddEditMasterTakerDialog(
            taker = takerToEdit,
            onDismiss = { editingTaker = null },
            onConfirm = { name, due, phone ->
                val index = localMasterTakerList.indexOfFirst { it.id == takerToEdit.id }
                if (index != -1) {
                    localMasterTakerList[index] = takerToEdit.copy(name = name, defaultTotalDue = due, defaultPhone = phone)
                    onUpdateMasterTakers(localMasterTakerList.toList())
                    Toast.makeText(context, "Taker updated", Toast.LENGTH_SHORT).show()
                }
                editingTaker = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashBinDialog(
    deletedEvents: List<Event>,
    onRestore: (Event) -> Unit,
    onPermanentlyDelete: (Event) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Trash Bin", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Deleted events are stored here. You can restore them or permanently delete them.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    if (deletedEvents.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Trash is empty.")
                            }
                        }
                    } else {
                        items(deletedEvents) { event ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(event.title, fontWeight = FontWeight.Bold)
                                    Text("${event.customerName} | ${event.date}", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(
                                            onClick = { onPermanentlyDelete(event) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Delete Forever")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(onClick = { onRestore(event) }) {
                                            Text("Restore")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(itemList: List<MasterItem>, onDismiss: () -> Unit, onConfirm: (name: String, cost: Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val filteredOptions = if (name.isEmpty()) {
        emptyList()
    } else {
        itemList.filter { it.name.contains(name, ignoreCase = true) }
    }

    LaunchedEffect(filteredOptions) {
        if (filteredOptions.isNotEmpty()) {
            expanded = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Item") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            expanded = true
                        },
                        label = { Text("Item Name") },
                        modifier = Modifier.menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )

                    if (filteredOptions.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteredOptions.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.name} (₹${item.cost})") },
                                    onClick = {
                                        name = item.name
                                        cost = item.cost.toString()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cost, onValueChange = { cost = it }, label = { Text("Cost") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, cost.toDoubleOrNull() ?: 0.0) }, enabled = name.isNotBlank() && cost.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlbumDialog(settings: AlbumSettings, onDismiss: () -> Unit, onConfirm: (Item) -> Unit) {
    var name by remember { mutableStateOf("") }
    var sheets by remember { mutableStateOf(settings.defaultSheets.toString()) }
    var quantity by remember { mutableStateOf("1") }

    val sheetCount = sheets.toIntOrNull() ?: settings.defaultSheets
    val qty = quantity.toIntOrNull() ?: 1
    val extraSheets = (sheetCount - settings.defaultSheets).coerceAtLeast(0)

    val unitCost = settings.baseCost + (extraSheets * settings.extraSheetCost)
    val totalCost = unitCost * qty

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Album") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Album Name (e.g. Wedding)") },
                    placeholder = { Text("Album") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = sheets,
                    onValueChange = { sheets = it },
                    label = { Text("Number of Sheets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Number of Albums") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text("Base Cost (${settings.defaultSheets} sheets): ₹${settings.baseCost}", style = MaterialTheme.typography.bodySmall)
                if (extraSheets > 0) {
                    Text("Extra Sheets ($extraSheets): +₹${extraSheets * settings.extraSheetCost}", style = MaterialTheme.typography.bodySmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Unit Cost:", fontWeight = FontWeight.Bold)
                    Text("₹${"%.2f".format(unitCost)}", fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total ($qty albums):", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Text("₹${"%.2f".format(totalCost)}", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (name.isBlank()) "Album" else name
                    onConfirm(Item(
                        id = System.currentTimeMillis(),
                        name = "$finalName ($sheetCount sheets)",
                        cost = unitCost,
                        quantity = qty
                    ))
                },
                enabled = sheetCount > 0 && qty > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(onDismiss: () -> Unit, onConfirm: (title: String, amount: Double, date: String) -> Unit) {
    var title by remember { mutableStateOf("Advance") }
    var amount by remember { mutableStateOf("") }

    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    var date by remember { mutableStateOf(sdf.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }

    val initialDateMillis = remember { try { sdf.parse(date)?.time } catch (_: Exception) { System.currentTimeMillis() } }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Payment") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Payment For") })
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            "Select Date",
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                    }
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(title, amount.toDoubleOrNull() ?: 0.0, date) }, enabled = title.isNotBlank() && amount.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPaymentDialog(payment: Payment, onDismiss: () -> Unit, onSave: (Payment) -> Unit, onDelete: (Payment) -> Unit) {
    var title by remember { mutableStateOf(payment.title) }
    var amountText by remember { mutableStateOf(payment.amount.toString()) }

    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    var date by remember { mutableStateOf(payment.date) }
    var showDatePicker by remember { mutableStateOf(false) }

    val initialDateMillis = remember { try { sdf.parse(payment.date)?.time } catch (_: Exception) { System.currentTimeMillis() } }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = sdf.format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    val enableSave = title.isNotBlank() && amountText.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Payment") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Payment For") })
                OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            Icons.Default.CalendarToday,
                            "Select Date",
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                    }
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = { onDelete(payment) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSave(payment.copy(title = title, amount = amountText.toDouble(), date = date)) },
                    enabled = enableSave
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDiscountDialog(currentDiscount: Double, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var discount by remember { mutableStateOf(currentDiscount.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Discount") },
        text = { OutlinedTextField(value = discount, onValueChange = { discount = it }, label = { Text("Discount Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
        confirmButton = { Button(onClick = { onConfirm(discount.toDoubleOrNull() ?: 0.0) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(onDismiss: () -> Unit, onConfirm: (title: String, date: String, customerName: String, customerPhone: String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    var date by remember { mutableStateOf(sdf.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = sdf.format(Date(it)) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") })
                OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") })
                OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it }, label = { Text("Customer Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = date, onValueChange = {}, label = { Text("Event Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Select Date", modifier = Modifier.clickable { showDatePicker = true }) })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(title, date, customerName, customerPhone) }, enabled = title.isNotBlank() && customerName.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEventDialog(event: Event, onDismiss: () -> Unit, onConfirm: (newTitle: String, newDate: String, newCustomerName: String, newCustomerPhone: String) -> Unit) {
    var title by remember { mutableStateOf(event.title) }
    var customerName by remember { mutableStateOf(event.customerName) }
    var customerPhone by remember { mutableStateOf(event.customerPhone) }
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    var date by remember { mutableStateOf(event.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialDateMillis = remember { try { sdf.parse(event.date)?.time } catch (_: Exception) { System.currentTimeMillis() } }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = sdf.format(Date(it)) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Event") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") })
                OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") })
                OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it }, label = { Text("Customer Phone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = date, onValueChange = {}, label = { Text("Event Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Select Date", modifier = Modifier.clickable { showDatePicker = true }) })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(title, date, customerName, customerPhone) }, enabled = title.isNotBlank() && customerName.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDayDialog(onDismiss: () -> Unit, onConfirm: (title: String, date: String) -> Unit) {
    var title by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    var date by remember { mutableStateOf(sdf.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = sdf.format(Date(it)) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Day") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Day Title") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = date, onValueChange = {}, label = { Text("Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Select Date", modifier = Modifier.clickable { showDatePicker = true }) })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(title, date) }, enabled = title.isNotBlank()) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayDialog(day: Day, onDismiss: () -> Unit, onConfirm: (newTitle: String, newDate: String) -> Unit) {
    var title by remember { mutableStateOf(day.title) }
    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    var date by remember { mutableStateOf(day.date) }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialDateMillis = remember { try { sdf.parse(day.date)?.time } catch (_: Exception) { System.currentTimeMillis() } }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = sdf.format(Date(it)) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Day") },
        text = {
            Column {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Day Title") })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = date, onValueChange = {}, label = { Text("Date") }, readOnly = true, trailingIcon = { Icon(Icons.Default.CalendarToday, "Select Date", modifier = Modifier.clickable { showDatePicker = true }) })
            }
        },
        confirmButton = { Button(onClick = { onConfirm(title, date) }, enabled = title.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ConfirmDeleteDialog(title: String, text: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTakerDialog(
    taker: Taker,
    masterTakers: List<MasterTaker> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Taker) -> Unit,
    onUpdateMasterTaker: ((MasterTaker) -> Unit)? = null
) {
    var name by remember { mutableStateOf(taker.name) }
    var totalDue by remember { mutableStateOf(if (taker.totalDue == 0.0) "" else taker.totalDue.toString()) }
    var amountPaid by remember { mutableStateOf(if (taker.amountPaid == 0.0) "" else taker.amountPaid.toString()) }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentMasterTaker = remember(name, masterTakers) {
        masterTakers.find { it.name.equals(name, ignoreCase = true) }
    }
    val isNewTakerEntry = taker.name.isBlank()
    val isNewMasterTaker = currentMasterTaker == null && name.isNotBlank()


    val filteredMasterTakers = remember(name) {
        masterTakers.filter { it.name.contains(name, ignoreCase = true) }
    }

    LaunchedEffect(name) {
        val selectedMasterTaker = masterTakers.find { it.name == name }
        if (selectedMasterTaker != null && taker.name.isBlank()) {
            totalDue = selectedMasterTaker.defaultTotalDue.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (taker.name.isBlank()) "Add Taker" else "Edit Taker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            expanded = true
                        },
                        label = { Text("Taker Name (Select or Type)") },
                        modifier = Modifier.menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )

                    if (filteredMasterTakers.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteredMasterTakers.forEach { masterTaker ->
                                DropdownMenuItem(
                                    text = { Text("${masterTaker.name} (Due: ₹${masterTaker.defaultTotalDue}, Phone: ${masterTaker.defaultPhone})") },
                                    onClick = {
                                        name = masterTaker.name
                                        totalDue = masterTaker.defaultTotalDue.toString()
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(value = totalDue, onValueChange = { totalDue = it }, label = { Text("Total Due") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = amountPaid, onValueChange = { amountPaid = it }, label = { Text("Amount Paid") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                onUpdateMasterTaker?.let { updateMaster ->
                    val actionText = when {
                        currentMasterTaker != null -> "Update Master Due"
                        isNewTakerEntry && isNewMasterTaker -> "Add to Master List"
                        else -> null
                    }
                    val infoText = when {
                        currentMasterTaker != null -> "This Taker exists in Master List. Update its default due?"
                        isNewTakerEntry && isNewMasterTaker -> "This is a new Taker. Add to Master List with current Due?"
                        else -> null
                    }

                    if (actionText != null) {
                        Column(Modifier.padding(top = 8.dp)) {
                            infoText?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                FilledTonalButton(
                                    onClick = {
                                        val newDue = totalDue.toDoubleOrNull() ?: 0.0
                                        val masterTakerToSave = currentMasterTaker?.copy(defaultTotalDue = newDue)
                                            ?: MasterTaker(System.currentTimeMillis(), name, newDue, "")
                                        updateMaster(masterTakerToSave)
                                        Toast.makeText(context, "${name}'s Master Due updated!", Toast.LENGTH_SHORT).show()
                                    },
                                    enabled = name.isNotBlank() && (totalDue.toDoubleOrNull() != null || currentMasterTaker != null)
                                ) {
                                    Text(actionText)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(taker.copy(name = name, totalDue = totalDue.toDoubleOrNull() ?: 0.0, amountPaid = amountPaid.toDoubleOrNull() ?: 0.0))
            }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTakerToEventDialog(event: Event, onDismiss: () -> Unit, onConfirm: (name: String, totalDue: Double, amountPaid: Double, selectedDayIds: List<Long>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var totalDue by remember { mutableStateOf("") }
    var amountPaid by remember { mutableStateOf("") }
    val selectedDayIds = remember { mutableStateListOf<Long>() }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Add Taker to Event", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Taker Name") })
                OutlinedTextField(value = totalDue, onValueChange = { totalDue = it }, label = { Text("Total Due") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = amountPaid, onValueChange = { amountPaid = it }, label = { Text("Amount Paid") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(Modifier.height(16.dp))
                Text("Assign to Days:", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    event.days.forEach { day ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable {
                            if (day.id in selectedDayIds) selectedDayIds.remove(day.id) else selectedDayIds.add(day.id)
                        }) {
                            Checkbox(checked = day.id in selectedDayIds, onCheckedChange = null)
                            Text(day.title)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(name, totalDue.toDoubleOrNull() ?: 0.0, amountPaid.toDoubleOrNull() ?: 0.0, selectedDayIds) }, enabled = name.isNotBlank()) { Text("Add") }
                }
            }
        }
    }
}

@Composable
fun AddEditMasterItemDialog(
    item: MasterItem? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, cost: Double) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var cost by remember { mutableStateOf(item?.cost?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Add Master Item" else "Edit Master Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") }
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Item Cost") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, cost.toDoubleOrNull() ?: 0.0) },
                enabled = name.isNotBlank() && cost.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddEditMasterTakerDialog(
    taker: MasterTaker? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, defaultTotalDue: Double, defaultPhone: String) -> Unit
) {
    var name by remember { mutableStateOf(taker?.name ?: "") }
    var defaultTotalDue by remember { mutableStateOf(if (taker?.defaultTotalDue == 0.0) "" else taker?.defaultTotalDue?.toString() ?: "") }
    var defaultPhone by remember { mutableStateOf(taker?.defaultPhone ?: "") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (taker == null) "Add Master Taker" else "Edit Master Taker Defaults") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Taker Name") }
                )
                OutlinedTextField(
                    value = defaultTotalDue,
                    onValueChange = { defaultTotalDue = it },
                    label = { Text("Default Total Due") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = defaultPhone,
                    onValueChange = {
                        val newText = it.filter { char -> char.isDigit() || char == '+' || char.isWhitespace() }
                        defaultPhone = newText
                    },
                    label = { Text("Default Phone Number (e.g., +91 9876543210)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                if (defaultPhone.isNotBlank() && !defaultPhone.startsWith('+')) {
                    Text(
                        "Note: For calling, the number should start with the country code, e.g., +91...",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formattedPhone = defaultPhone.filter { it.isDigit() || it == '+' }
                    if (formattedPhone.isNotBlank() && !formattedPhone.startsWith('+')) {
                        Toast.makeText(context, "Please include a country code starting with '+' (e.g., +91...)", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    onConfirm(
                        name,
                        defaultTotalDue.toDoubleOrNull() ?: 0.0,
                        formattedPhone
                    )
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalExpensesScreen(
    event: Event,
    onBack: () -> Unit,
    onUpdateExpenses: (List<InternalExpense>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<InternalExpense?>(null) }

    val expenses = event.internalExpenses.toList()

    val totalExpense = expenses.sumOf { it.amount }

    val handleUpdate: (InternalExpense) -> Unit = { newExpense ->
        val currentList = event.internalExpenses.toMutableList()
        val index = currentList.indexOfFirst { it.id == newExpense.id }
        if (index != -1) {
            currentList[index] = newExpense
        } else {
            currentList.add(newExpense)
        }
        onUpdateExpenses(currentList)
        editingExpense = null
        showAddDialog = false
    }

    val handleDelete: (InternalExpense) -> Unit = { expense ->
        val currentList = event.internalExpenses.toMutableList()
        currentList.removeIf { it.id == expense.id }
        onUpdateExpenses(currentList)
        editingExpense = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column {
                    Text("${event.title} - Internal Expenses")
                    Text("Total: ₹${String.format(Locale.getDefault(), "%,.2f", totalExpense)}", style = MaterialTheme.typography.bodySmall)
                } },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, "Add Expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (expenses.isEmpty()) {
                item {
                    Text("No internal expenditures recorded for this event.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(expenses, key = { it.id }) { expense ->
                ExpenseListItem(
                    expense = expense,
                    onEdit = { editingExpense = expense },
                    onDelete = { handleDelete(expense) }
                )
            }
        }
    }

    if (showAddDialog || editingExpense != null) {
        AddEditInternalExpenseDialog(
            expense = editingExpense,
            onDismiss = { showAddDialog = false; editingExpense = null },
            onConfirm = handleUpdate
        )
    }
}

@Composable
fun ExpenseListItem(expense: InternalExpense, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(expense.title, fontWeight = FontWeight.Medium)
                Text(expense.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            Text("₹${String.format(Locale.getDefault(), "%,.2f", expense.amount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInternalExpenseDialog(
    expense: InternalExpense? = null,
    onDismiss: () -> Unit,
    onConfirm: (InternalExpense) -> Unit
) {
    val isEditing = expense != null
    var title by remember { mutableStateOf(expense?.title ?: "") }
    var amountText by remember { mutableStateOf(if (isEditing) expense!!.amount.toString() else "") }

    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    var date by remember { mutableStateOf(expense?.date ?: sdf.format(Date())) }
    var showDatePicker by remember { mutableStateOf(false) }
    val initialDateMillis = remember { try { sdf.parse(date)?.time } catch (_: Exception) { System.currentTimeMillis() } }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { datePickerState.selectedDateMillis?.let { date = sdf.format(Date(it)) }; showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = datePickerState) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Internal Expense" else "Add Internal Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title/Description") })
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (Internal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    label = { Text("Date") },
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.CalendarToday, "Select Date", modifier = Modifier.clickable { showDatePicker = true }) }
                )
                Text(
                    "Note: This expense will NOT appear on the client's bill.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val expenseToSave = expense?.copy(title = title, amount = amount, date = date)
                        ?: InternalExpense(System.currentTimeMillis(), title, amount, date)
                    onConfirm(expenseToSave)
                },
                enabled = title.isNotBlank() && amountText.isNotBlank()
            ) { Text(if (isEditing) "Save" else "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}