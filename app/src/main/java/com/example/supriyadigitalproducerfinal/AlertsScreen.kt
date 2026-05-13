package com.example.supriyadigitalproducerfinal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.supriyadigitalproducerfinal.ui.theme.BalanceDue
import com.example.supriyadigitalproducerfinal.ui.theme.BalancePaid
import androidx.compose.ui.graphics.Color as ComposeColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// ALERTS SCREEN — shows all logged success / error / info / warning
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen() {
    val context = LocalContext.current
    val store   = remember { NotificationSettingsStore(context) }

    var alerts          by remember { mutableStateOf(store.loadAlerts()) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Refresh when screen is opened
    LaunchedEffect(Unit) { alerts = store.loadAlerts() }

    // Group by date label (Today / Yesterday / Earlier)
    val today     = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date())
    val yesterday = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).let { sdf ->
        val cal = Calendar.getInstance().also { it.add(Calendar.DAY_OF_YEAR, -1) }
        sdf.format(cal.time)
    }

    fun dayLabel(timestamp: Long): String {
        val d = SimpleDateFormat("ddMMyyyy", Locale.getDefault()).format(Date(timestamp))
        return when (d) {
            today     -> "Today"
            yesterday -> "Yesterday"
            else      -> SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    val grouped = alerts.groupBy { dayLabel(it.timestamp) }
    val dayOrder = grouped.keys.toList() // already newest-first because alerts are stored newest-first

    val successCount = alerts.count { it.type == AlertType.SUCCESS }
    val errorCount   = alerts.count { it.type == AlertType.ERROR }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Alerts", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (alerts.isNotEmpty()) {
                            Text(
                                "${alerts.size} total · $successCount ok · $errorCount errors",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (alerts.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Filled.ClearAll, "Clear All Alerts")
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (alerts.isEmpty()) {
            Box(Modifier.padding(padding)) {
                EmptyState(
                    icon     = Icons.Filled.NotificationsNone,
                    title    = "No alerts yet",
                    subtitle = "Success messages, errors, and SMS logs will appear here automatically.",
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp)
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Summary chips at top
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryChip("${alerts.size} Total",    MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                        SummaryChip("$successCount Success",  ComposeColor(0xFFE8F5E9),                  BalancePaid)
                        SummaryChip("$errorCount Errors",     MaterialTheme.colorScheme.errorContainer,   MaterialTheme.colorScheme.error)
                    }
                }

                dayOrder.forEach { dayLabel ->
                    val dayAlerts = grouped[dayLabel] ?: return@forEach

                    item(key = "header_$dayLabel") {
                        SectionHeader(dayLabel, Modifier.padding(top = 8.dp))
                    }

                    items(dayAlerts, key = { it.id }) { alert ->
                        AlertCard(alert)
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title   = { Text("Clear All Alerts") },
            text    = { Text("This will permanently delete all ${alerts.size} alert entries. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { store.clearAlerts(); alerts = emptyList<AlertEntry>().toMutableList(); showClearDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear All") }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancel") } }
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// ALERT CARD
// ─────────────────────────────────────────────────────────────────

@Composable
fun AlertCard(alert: AlertEntry) {
    val (bgColor, iconColor, icon) = when (alert.type) {
        AlertType.SUCCESS -> Triple(ComposeColor(0xFFE8F5E9), BalancePaid,                          Icons.Filled.CheckCircle)
        AlertType.ERROR   -> Triple(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.error, Icons.Filled.Error)
        AlertType.WARNING -> Triple(ComposeColor(0xFFFFF8E1), ComposeColor(0xFFF57F17),             Icons.Filled.Warning)
        AlertType.INFO    -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Icons.Filled.Info)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().animateContentSize()
    ) {
        Row(
            Modifier
                .background(bgColor)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon badge
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(18.dp), iconColor)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(
                        alert.title,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = iconColor
                    )
                    Text(
                        alert.formattedTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (alert.message.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        alert.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// SUMMARY CHIP
// ─────────────────────────────────────────────────────────────────

@Composable
fun SummaryChip(label: String, background: ComposeColor, contentColor: ComposeColor) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// ALERT BADGE — small inline badge used in navigation bar
// ─────────────────────────────────────────────────────────────────

@Composable
fun AlertBadge(count: Int) {
    if (count <= 0) return
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (count > 9) "9+" else count.toString(),
            style      = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)),
            color      = MaterialTheme.colorScheme.onError,
            fontWeight = FontWeight.Bold
        )
    }
}
