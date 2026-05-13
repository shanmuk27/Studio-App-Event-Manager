package com.example.supriyadigitalproducerfinal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.supriyadigitalproducerfinal.ui.theme.BalanceDue
import com.example.supriyadigitalproducerfinal.ui.theme.BalancePaid
import com.example.supriyadigitalproducerfinal.ui.theme.avatarColors
import java.util.Locale
import kotlin.math.absoluteValue
import androidx.compose.ui.graphics.Color as ComposeColor

// ─────────────────────────────────────────────────────────────────
// APP BANNER
// ─────────────────────────────────────────────────────────────────

@Composable
fun AppBanner() {
    Surface(Modifier.fillMaxWidth(), shadowElevation = 8.dp, tonalElevation = 4.dp) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.supriya_banner),
            contentDescription = "Supriya Digital Studio",
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RectangleShape),
            contentScale = ContentScale.FillWidth
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// SYNC STATUS
// ─────────────────────────────────────────────────────────────────

@Composable
fun SyncStatusIndicator(status: Int) {
    val (color, text, icon) = when (status) {
        1 -> Triple(MaterialTheme.colorScheme.primary,   "Syncing data to cloud…",      Icons.Filled.CloudUpload)
        2 -> Triple(MaterialTheme.colorScheme.error,     "Sync failed. Check internet.", Icons.Filled.CloudOff)
        3 -> Triple(ComposeColor(0xFF2E7D32),             "Data synced successfully!",   Icons.Filled.CloudDone)
        else -> return
    }
    Surface(color = color, contentColor = ComposeColor.White, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 16.dp)
        ) {
            Icon(icon, null, Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun FloatingSyncIcon(status: Int, modifier: Modifier = Modifier) {
    val (color, icon) = when (status) {
        0    -> Pair(MaterialTheme.colorScheme.surfaceVariant, Icons.Filled.CloudQueue)
        1    -> Pair(MaterialTheme.colorScheme.primary,        Icons.Filled.CloudUpload)
        2    -> Pair(MaterialTheme.colorScheme.error,          Icons.Filled.Warning)
        3    -> Pair(ComposeColor(0xFF2E7D32),                  Icons.Filled.CloudDone)
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, Icons.Filled.CloudQueue)
    }
    val tr = rememberInfiniteTransition(label = "sync")
    val angle by tr.animateFloat(
        initialValue = 0f,
        targetValue  = if (status == 1) 360f else 0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "angle"
    )
    Surface(shape = CircleShape, color = color, shadowElevation = 6.dp, modifier = modifier.size(48.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, "Sync Status", tint = ComposeColor.White,
                modifier = Modifier.rotate(if (status == 1) angle else 0f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// INITIALS AVATAR
// ─────────────────────────────────────────────────────────────────

@Composable
fun InitialsAvatar(name: String, size: Dp, modifier: Modifier = Modifier) {
    val initials = name.trim().split(" ").take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("").ifEmpty { "?" }
    val bg = avatarColors[name.hashCode().absoluteValue % avatarColors.size]
    Box(
        modifier = modifier.size(size).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initials,
            color = ComposeColor.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// STATUS CHIP
// ─────────────────────────────────────────────────────────────────

@Composable
fun StatusChip(
    label: String,
    background: ComposeColor,
    contentColor: ComposeColor = ComposeColor.White
) {
    Surface(shape = RoundedCornerShape(50), color = background) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(
                label,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// PAYMENT PROGRESS BAR
// ─────────────────────────────────────────────────────────────────

@Composable
fun PaymentProgressBar(totalCost: Double, totalPaid: Double, modifier: Modifier = Modifier) {
    val pct = if (totalCost > 0) (totalPaid / totalCost).coerceIn(0.0, 1.0).toFloat() else 0f
    val animPct by animateFloatAsState(pct, tween(800, easing = FastOutSlowInEasing), label = "progress")
    val barColor = if (pct >= 1f) ComposeColor(0xFF2E7D32) else MaterialTheme.colorScheme.primary
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
            Text(
                "Paid ₹${String.format(Locale.getDefault(), "%,.0f", totalPaid)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${(animPct * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (pct >= 1f) ComposeColor(0xFF2E7D32) else MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animPct },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// EMPTY STATE
// ─────────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: Pair<String, () -> Unit>? = null
) {
    Column(
        modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(40.dp), MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        action?.let { (label, onClick) ->
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = onClick) { Text(label) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// STAT CARD
// ─────────────────────────────────────────────────────────────────

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: ComposeColor,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, Modifier.size(22.dp), color)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// SECTION HEADER
// ─────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    )
}

// ─────────────────────────────────────────────────────────────────
// STYLED SEARCH BAR
// ─────────────────────────────────────────────────────────────────

@Composable
fun StyledSearchBar(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("Search events…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    inner()
                }
            }
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Search, "Search", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// FILTER DIALOG + DROPDOWN
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDialog(
    currentYear: String,
    currentMonth: String,
    yearOptions: List<String>,
    monthOptions: List<String>,
    onDismiss: () -> Unit,
    onApply: (String, String) -> Unit
) {
    var tempYear  by remember { mutableStateOf(currentYear) }
    var tempMonth by remember { mutableStateOf(currentMonth) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Filter Events", style = MaterialTheme.typography.titleLarge)
                FilterDropdown(
                    label = "Year",
                    selectedValue = tempYear,
                    options = yearOptions,
                    onValueChange = { tempYear = it; if (it == "Year") tempMonth = "Month" }
                )
                FilterDropdown(
                    label = "Month",
                    selectedValue = tempMonth,
                    options = monthOptions,
                    onValueChange = { tempMonth = it },
                    enabled = tempYear != "Year"
                )
                Row(Modifier.fillMaxWidth(), Arrangement.End, Alignment.CenterVertically) {
                    TextButton(onClick = { tempYear = "Year"; tempMonth = "Month" }) { Text("Clear") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onApply(tempYear, tempMonth) }) { Text("Apply") }
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
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onValueChange(option); expanded = false }
                )
            }
        }
    }
}
