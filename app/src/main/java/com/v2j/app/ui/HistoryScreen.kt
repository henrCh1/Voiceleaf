package com.v2j.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2j.app.MainViewModel
import com.v2j.app.data.Entry
import com.v2j.app.data.PolishStatus
import com.v2j.app.sync.WeekMath
import java.time.LocalDate

@Composable
fun HistoryScreen(vm: MainViewModel, onBack: () -> Unit) {
    val entries by vm.allEntries.collectAsState()

    // Group by ISO week (newest week first); each week keeps its entries oldest-first for display.
    val weeks = remember(entries) {
        entries
            .groupBy { WeekMath.weekStartMillis(WeekMath.localDate(it.createdAt)) }
            .toList()
            .sortedByDescending { it.first }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Spacer(Modifier.width(2.dp))
                Text(
                    "历史记录",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            if (weeks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "还没有任何记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
                ) {
                    weeks.forEach { (weekStart, weekEntries) ->
                        val anchor = WeekMath.localDate(weekStart)
                        val weekId = WeekMath.weekId(anchor)
                        val range = "${WeekMath.mmDashDd(WeekMath.weekStartDate(anchor))} – " +
                            WeekMath.mmDashDd(WeekMath.weekEndDate(anchor))

                        item(key = "wk-$weekStart") {
                            WeekHeader(weekId = weekId, range = range, count = weekEntries.size)
                        }

                        // Newest day on top; within a day, newest entry on top.
                        val byDay = weekEntries
                            .groupBy { WeekMath.localDate(it.createdAt) }
                            .toSortedMap(compareByDescending<LocalDate> { it })

                        byDay.forEach { (day, dayEntries) ->
                            item(key = "day-$weekStart-$day") { DayHeader(day) }
                            items(dayEntries.sortedByDescending { it.createdAt }, key = { it.id }) { entry ->
                                HistoryEntryRow(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekHeader(weekId: String, range: String, count: Int) {
    Column(Modifier.padding(top = 18.dp, bottom = 8.dp)) {
        Text(
            weekId,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            "$range · $count 条",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DayHeader(day: LocalDate) {
    Text(
        "${WeekMath.mmDashDd(day)} ${WeekMath.zhWeekday(day.dayOfWeek)}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
    )
}

@Composable
private fun HistoryEntryRow(entry: Entry) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    WeekMath.hhmm(entry.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                HistoryStatusPill(entry.polishStatus)
            }
            Spacer(Modifier.size(6.dp))
            Text(
                (entry.polishedText ?: entry.rawText).trim(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun HistoryStatusPill(status: PolishStatus) {
    val color = historyStatusColor(status)
    val label = when (status) {
        PolishStatus.POLISHED -> "已润色"
        PolishStatus.POLISHING -> "润色中"
        PolishStatus.RAW -> "待润色"
        PolishStatus.FAILED -> "润色失败"
    }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.14f)) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun historyStatusColor(status: PolishStatus): Color = when (status) {
    PolishStatus.POLISHED -> MaterialTheme.colorScheme.primary
    PolishStatus.POLISHING -> MaterialTheme.colorScheme.tertiary
    PolishStatus.RAW -> MaterialTheme.colorScheme.onSurfaceVariant
    PolishStatus.FAILED -> MaterialTheme.colorScheme.error
}
