package com.lifetrio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Upcoming
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lifetrio.plan.calendar.DeviceCalendarDayOverride
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.theme.LocalExtendedColors
import com.lifetrio.ui.theme.Spacing
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun PlanCalendarColumn(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    plansByDate: Map<LocalDate, Int>,
    workdayOverrides: Map<LocalDate, Boolean>,
    calendarOverrides: Map<LocalDate, DeviceCalendarDayOverride>,
    selectedItems: List<com.lifetrio.core.data.db.dao.PlanWithOccurrence>,
    previewCounts: Map<LocalDate, Int>,
    calendarMessage: String,
    hasCalendarPermission: Boolean,
    isCalendarSyncing: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onRequestCalendar: () -> Unit,
    onComplete: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = modifier) {
        CalendarPanel(
            visibleMonth = visibleMonth,
            selectedDate = selectedDate,
            today = today,
            plansByDate = plansByDate,
            workdayOverrides = workdayOverrides,
            calendarOverrides = calendarOverrides,
            calendarMessage = calendarMessage,
            hasCalendarPermission = hasCalendarPermission,
            isCalendarSyncing = isCalendarSyncing,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectDate = onSelectDate,
            onRequestCalendar = onRequestCalendar
        )
        SelectedDatePlanPanel(selectedDate, selectedItems, onComplete, onSkip)
        WeekPreviewPanel(selectedDate, previewCounts)
    }
}

@Composable
private fun CalendarPanel(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    plansByDate: Map<LocalDate, Int>,
    workdayOverrides: Map<LocalDate, Boolean>,
    calendarOverrides: Map<LocalDate, DeviceCalendarDayOverride>,
    calendarMessage: String,
    hasCalendarPermission: Boolean,
    isCalendarSyncing: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onRequestCalendar: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FieldLabel(Icons.Outlined.CalendarMonth, "日历 & 打卡")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onPreviousMonth) { Icon(Icons.Default.ChevronLeft, contentDescription = "上月") }
                Text("${visibleMonth.year}年 ${visibleMonth.monthValue}月", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onNextMonth) { Icon(Icons.Default.ChevronRight, contentDescription = "下月") }
            }
            CalendarMonthGrid(
                visibleMonth = visibleMonth,
                selectedDate = selectedDate,
                today = today,
                plansByDate = plansByDate,
                workdayOverrides = workdayOverrides,
                calendarOverrides = calendarOverrides,
                onSelectDate = onSelectDate
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(calendarMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextButton(onClick = onRequestCalendar, enabled = !isCalendarSyncing) {
                    Text(
                        when {
                            isCalendarSyncing -> "同步中..."
                            hasCalendarPermission -> "同步手机日历"
                            else -> "连接手机日历"
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    plansByDate: Map<LocalDate, Int>,
    workdayOverrides: Map<LocalDate, Boolean>,
    calendarOverrides: Map<LocalDate, DeviceCalendarDayOverride>,
    onSelectDate: (LocalDate) -> Unit
) {
    val firstDay = visibleMonth.atDay(1)
    val leading = firstDay.dayOfWeek.value % 7
    val days = (1..visibleMonth.lengthOfMonth()).map { visibleMonth.atDay(it) }
    val cells = List(leading) { null } + days
    val weeks = cells.chunked(7)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs), modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(day, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs), modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    CalendarDayCell(
                        date = date,
                        selected = date == selectedDate,
                        isToday = date == today,
                        planCount = date?.let { plansByDate[it] } ?: 0,
                        workdayOverride = date?.let { workdayOverrides[it] },
                        deviceTitle = date?.let { calendarOverrides[it]?.title },
                        onClick = { if (date != null) onSelectDate(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(7 - week.size) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate?,
    selected: Boolean,
    isToday: Boolean,
    planCount: Int,
    workdayOverride: Boolean?,
    deviceTitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val shape = RoundedCornerShape(12.dp)
    val background = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        planCount > 0 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderWidth = when {
        selected -> 2.dp
        isToday -> 1.5.dp
        else -> 0.dp
    }
    val cellModifier = modifier
        .aspectRatio(1f)
        .background(background, shape)
        .then(
            if (borderWidth > 0.dp) Modifier.border(width = borderWidth, color = MaterialTheme.colorScheme.primary, shape = shape)
            else Modifier
        )
        .clickable(enabled = date != null, onClick = onClick)
        .padding(Spacing.xxs)
    Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
        if (date != null) {
            Text(date.dayOfMonth.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal)
            if (workdayOverride != null) {
                Text(if (workdayOverride) "班" else "休", color = if (workdayOverride) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopEnd))
            }
            if (planCount > 0) {
                Text(planCount.toString(), color = ext.income, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter))
            }
            if (!deviceTitle.isNullOrBlank()) {
                Box(Modifier.size(5.dp).background(MaterialTheme.colorScheme.tertiary, CircleShape).align(Alignment.TopStart))
            }
        }
    }
}

@Composable
private fun SelectedDatePlanPanel(
    selectedDate: LocalDate,
    items: List<com.lifetrio.core.data.db.dao.PlanWithOccurrence>,
    onComplete: (Long) -> Unit,
    onSkip: (Long) -> Unit
) {
    // observeToday only returns Pending/Carried occurrences; a completed item
    // disappears from the flow. Track locally-checked ids so the tick is visible
    // until the next emission removes the row.
    var completing by remember { mutableStateOf(setOf<Long>()) }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FieldLabel(Icons.Outlined.TaskAlt, "$selectedDate 待办")
            if (items.isEmpty()) {
                Text("无计划", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(
                            checked = item.occurrenceId in completing,
                            onCheckedChange = {
                                completing = completing + item.occurrenceId
                                onComplete(item.occurrenceId)
                            }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (item.note.isNotBlank()) Text(item.note, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        TextButton(onClick = { onSkip(item.occurrenceId) }) { Text("跳过") }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekPreviewPanel(selectedDate: LocalDate, counts: Map<LocalDate, Int>) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            FieldLabel(Icons.Outlined.Upcoming, "未来一周计划预览")
            (0L..6L).forEach { offset ->
                val date = selectedDate.plusDays(offset)
                val count = counts[date] ?: 0
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(date.toString(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text(if (count == 0) "无计划" else "$count 项计划", color = if (count == 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
