package com.example.supriyadigitalproducerfinal

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.supriyadigitalproducerfinal.ui.theme.BalanceDue
import com.example.supriyadigitalproducerfinal.ui.theme.BalancePaid
import com.example.supriyadigitalproducerfinal.ui.theme.BalancePartial
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color as ComposeColor

// ─────────────────────────────────────────────────────────────────
// ANALYTICS SCREEN
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(events: List<Event>, masterTakers: List<MasterTaker>) {
    val sdf         = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }
    val monthFmt    = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }
    val monthKeyFmt = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }

    // ── Monthly stats ────────────────────────────────────────────────
    val monthlyStats: List<MonthlyStats> = remember(events.toList()) {
        val map = mutableMapOf<String, Triple<Double, Double, String>>()
        events.forEach { event ->
            event.payments.forEach { payment ->
                val cal = Calendar.getInstance()
                try { cal.time = sdf.parse(payment.date) ?: Date() } catch (_: Exception) {}
                val key     = monthKeyFmt.format(cal.time)
                val display = monthFmt.format(cal.time)
                val existing = map[key] ?: Triple(0.0, 0.0, display)
                map[key] = existing.copy(first = existing.first + payment.amount, third = display)
            }
            event.days.forEach { day ->
                val cal = Calendar.getInstance()
                try { cal.time = sdf.parse(day.date) ?: Date() } catch (_: Exception) {}
                val key        = monthKeyFmt.format(cal.time)
                val display    = monthFmt.format(cal.time)
                val takerTotal = day.takers.sumOf { it.totalDue }
                val existing   = map[key] ?: Triple(0.0, 0.0, display)
                map[key] = existing.copy(second = existing.second + takerTotal, third = display)
            }
        }
        map.entries.sortedByDescending { it.key }.map { (_, v) ->
            val (revenue, takerCost, display) = v
            MonthlyStats(display, revenue, takerCost, revenue - takerCost)
        }
    }

    // ── This month ───────────────────────────────────────────────────
    val thisMonthKey = monthKeyFmt.format(Date())
    val thisMonth = monthlyStats.firstOrNull {
        try {
            val cal = Calendar.getInstance()
            cal.time = SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(it.month) ?: Date()
            monthKeyFmt.format(cal.time) == thisMonthKey
        } catch (_: Exception) { false }
    }

    // ── All-time totals ──────────────────────────────────────────────
    val totalRevenue   = events.sumOf { e -> e.payments.sumOf { it.amount } }
    val totalTakerCost = events.sumOf { e -> e.days.sumOf { d -> d.takers.sumOf { it.totalDue } } }
    // FIX: Pending Dues only counts confirmed events
    val totalBalance   = events.filter { it.isConfirmed }.sumOf { e ->
        val cost = e.eventItems.sumOf { it.cost * it.quantity } +
                e.days.sumOf { d -> d.items.sumOf { it.cost * it.quantity } }
        cost - e.discount - e.payments.sumOf { it.amount }
    }

    // ── Taker payout list ────────────────────────────────────────────
    val takerPayouts: List<TakerPayout> = remember(events.toList()) {
        events.flatMap { event ->
            event.days.flatMap { day ->
                day.takers.map { taker ->
                    TakerPayout(taker.name, event.title, day.title, day.date, taker.totalDue, taker.amountPaid)
                }
            }
        }.sortedByDescending { it.dayDate }
    }

    // ── Top 5 takers by cost ─────────────────────────────────────────
    val topTakers = remember(takerPayouts) {
        takerPayouts.groupBy { it.takerName }
            .mapValues { (_, list) -> list.sumOf { it.amountDue } }
            .entries.sortedByDescending { it.value }.take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── All-time summary ──────────────────────────────────────
            item {
                Text("All-Time Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Total Revenue",  "₹${String.format(Locale.getDefault(), "%,.0f", totalRevenue)}",              Icons.Filled.TrendingUp,    ComposeColor(0xFF2E7D32), Modifier.weight(1f))
                        StatCard("Taker Costs",    "₹${String.format(Locale.getDefault(), "%,.0f", totalTakerCost)}",            Icons.Filled.Groups,        BalanceDue,              Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard("Net Income",     "₹${String.format(Locale.getDefault(), "%,.0f", totalRevenue - totalTakerCost)}", Icons.Filled.AttachMoney, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        StatCard("Pending Dues",   "₹${String.format(Locale.getDefault(), "%,.0f", totalBalance)}",              Icons.Filled.AccountBalance, if (totalBalance > 0) BalanceDue else BalancePaid, Modifier.weight(1f))
                    }
                }
            }

            // ── This month ────────────────────────────────────────────
            item {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CalendarToday, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("This Month — ${monthFmt.format(Date())}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (thisMonth == null) {
                            Text("No activity this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            @Composable
                            fun StatLine(label: String, amount: Double, color: ComposeColor) {
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), Arrangement.SpaceBetween) {
                                    Text(label, style = MaterialTheme.typography.bodyMedium)
                                    Text("₹${String.format(Locale.getDefault(), "%,.2f", amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                            StatLine("Revenue Received", thisMonth.revenue,       ComposeColor(0xFF2E7D32))
                            StatLine("Taker Payments",   thisMonth.takerPayouts,  BalanceDue)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            StatLine("Net Income",        thisMonth.netIncome, if (thisMonth.netIncome >= 0) MaterialTheme.colorScheme.primary else BalanceDue)
                        }
                    }
                }
            }

            // ── Monthly revenue trend (last 6 months) ─────────────────
            if (monthlyStats.isNotEmpty()) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Monthly Trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            val maxRevenue = monthlyStats.maxOfOrNull { it.revenue }?.coerceAtLeast(1.0) ?: 1.0
                            monthlyStats.take(6).forEach { stat ->
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                        Text(stat.month, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("₹${String.format(Locale.getDefault(), "%,.0f", stat.revenue)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ComposeColor(0xFF2E7D32))
                                    }
                                    Spacer(Modifier.height(3.dp))
                                    val pct = (stat.revenue / maxRevenue).toFloat().coerceIn(0f, 1f)
                                    val animPct by animateFloatAsState(pct, tween(600, easing = FastOutSlowInEasing), label = "bar_${stat.month}")
                                    LinearProgressIndicator(
                                        progress   = { animPct },
                                        modifier   = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                        color      = ComposeColor(0xFF2E7D32),
                                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                                        strokeCap  = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Top 5 takers by cost ──────────────────────────────────
            if (topTakers.isNotEmpty()) {
                item {
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Top Takers by Cost", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            val maxCost = topTakers.maxOfOrNull { it.value }?.coerceAtLeast(1.0) ?: 1.0
                            topTakers.forEachIndexed { idx, (name, cost) ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${idx + 1}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    InitialsAvatar(name = name, size = 32.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        val pct    = (cost / maxCost).toFloat()
                                        val animPct by animateFloatAsState(pct, tween(600, easing = FastOutSlowInEasing), label = "taker_$name")
                                        LinearProgressIndicator(
                                            progress   = { animPct },
                                            modifier   = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                            color      = BalanceDue,
                                            trackColor = MaterialTheme.colorScheme.outlineVariant,
                                            strokeCap  = StrokeCap.Round
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text("₹${String.format(Locale.getDefault(), "%,.0f", cost)}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BalanceDue)
                                }
                            }
                        }
                    }
                }
            }

            // ── Taker payout detail ───────────────────────────────────
            item {
                Text("Taker Payout Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (takerPayouts.isEmpty()) {
                item { EmptyState(Icons.Outlined.Groups, "No taker data", "Add takers to event days to see payout analysis.", modifier = Modifier.fillMaxWidth()) }
            } else {
                items(takerPayouts) { payout ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            InitialsAvatar(name = payout.takerName, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(payout.takerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${payout.dayTitle} · ${payout.eventTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(payout.dayDate, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${String.format(Locale.getDefault(), "%,.0f", payout.amountDue)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = BalanceDue)
                                val bal = payout.amountDue - payout.amountPaid
                                if (bal > 0) Text("Bal: ₹${String.format(Locale.getDefault(), "%,.0f", bal)}", style = MaterialTheme.typography.labelSmall, color = BalancePartial)
                                else StatusChip("Paid", BalancePaid)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
