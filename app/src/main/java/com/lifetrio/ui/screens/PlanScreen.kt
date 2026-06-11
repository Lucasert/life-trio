package com.lifetrio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.lifetrio.core.data.AppContainer
import com.lifetrio.core.data.db.entity.CarryStrategy
import com.lifetrio.core.data.db.entity.PlanEntity
import com.lifetrio.core.data.db.entity.PlanRuleType
import com.lifetrio.plan.calendar.DeviceCalendarDayOverride
import com.lifetrio.plan.calendar.DeviceCalendarReader
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.FilterPill
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val today = LocalDate.now()
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDate by remember { mutableStateOf(today) }
    val monthStart = visibleMonth.atDay(1)
    val monthEnd = visibleMonth.atEndOfMonth()
    val previewEnd = selectedDate.plusDays(6)
    val plans by container.planRepository.observePlans().collectAsState(initial = emptyList())
    val selectedItems by container.planRepository.observeToday(selectedDate).collectAsState(initial = emptyList())
    val previewItems by container.planRepository.observeOccurrenceCounts(selectedDate, previewEnd).collectAsState(initial = emptyList())
    val monthCounts by container.planRepository.observeOccurrenceCounts(monthStart, monthEnd).collectAsState(initial = emptyList())
    val overrides by container.planRepository.observeWorkdayOverrides().collectAsState(initial = emptyList())
    val builtInWorkdayOverrides = remember(monthStart, monthEnd) {
        container.planRepository.legalWorkdayOverrides(monthStart, monthEnd)
    }
    val displayedWorkdayOverrides = builtInWorkdayOverrides + overrides.associate { it.date to it.isWorkday }
    var calendarOverrides by remember { mutableStateOf<List<DeviceCalendarDayOverride>>(emptyList()) }
    var calendarMessage by remember { mutableStateOf("未连接手机日历") }
    var hasCalendarPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    var editingPlan by remember { mutableStateOf<PlanEntity?>(null) }
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf(PlanRuleType.Daily) }
    var interval by remember { mutableStateOf("2") }
    var weekdays by remember { mutableStateOf(setOf(today.dayOfWeek.value)) }
    var monthDays by remember { mutableStateOf(setOf(today.dayOfMonth)) }
    var carry by remember { mutableStateOf(CarryStrategy.CarryNextDay) }
    var isCalendarSyncing by remember { mutableStateOf(false) }

    suspend fun syncDeviceCalendar() {
        if (!hasCalendarPermission) {
            calendarMessage = "请授权读取手机日历后再同步"
            return
        }
        if (isCalendarSyncing) {
            return
        }
        val start = monthStart
        val end = maxOf(monthEnd, previewEnd)
        isCalendarSyncing = true
        calendarMessage = "正在同步手机日历..."
        try {
            val result = withContext(Dispatchers.IO) {
                DeviceCalendarReader(context).readWorkdayOverrideResult(start, end)
            }
            val entries = result.overrides
            calendarOverrides = entries
            entries.forEach { container.planRepository.setWorkdayOverride(it.date, it.isWorkday) }
            container.planRepository.generateOccurrences(start, end)
            calendarMessage = when {
                entries.isNotEmpty() -> "已同步 ${entries.size} 天法定节假日/调休"
                result.scannedEventCount > 0 -> "读取到 ${result.scannedEventCount} 条日历事件，但未识别到法定节假日/调休，已使用内置规则"
                result.calendarCount > 0 -> "手机日历有 ${result.calendarCount} 个可读日历源，但本月无事件，已使用内置规则"
                else -> "手机未暴露可读日历事件，已使用内置规则"
            }
        } catch (error: SecurityException) {
            hasCalendarPermission = false
            calendarMessage = "没有读取日历权限，请重新授权"
            Log.e("LifeTrioPlan", "Calendar permission denied while syncing", error)
        } catch (error: Exception) {
            calendarMessage = "手机日历同步失败：${error.javaClass.simpleName}"
            Log.e("LifeTrioPlan", "Calendar sync failed", error)
        } finally {
            isCalendarSyncing = false
        }
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCalendarPermission = granted
        if (granted) {
            scope.launch { syncDeviceCalendar() }
        } else {
            calendarMessage = "未授权手机日历，法定工作日使用内置规则"
        }
    }

    LaunchedEffect(hasCalendarPermission, visibleMonth, selectedDate) {
        syncDeviceCalendar()
    }

    fun resetPlanEditor() {
        editingPlan = null
        title = ""
        note = ""
        rule = PlanRuleType.Daily
        interval = "2"
        weekdays = setOf(today.dayOfWeek.value)
        monthDays = setOf(today.dayOfMonth)
        carry = CarryStrategy.CarryNextDay
    }

    AppPage {
        item {
            ScreenHeader("计划", "周期计划 · 日历同步 · 待办追踪")
            if (!container.planRepository.hasWorkdayCalendarFor(today.year)) {
                Text("当前年份缺少中国法定工作日表，请维护节假日数据。", color = Color(0xFFB45309))
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 760.dp
                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.weight(0.72f)) {
                            PlanEditorPanel(
                                editingPlan = editingPlan,
                                title = title,
                                note = note,
                                rule = rule,
                                interval = interval,
                                weekdays = weekdays,
                                monthDays = monthDays,
                                carry = carry,
                                onTitle = { title = it },
                                onNote = { note = it },
                                onRule = { rule = it },
                                onInterval = { interval = it },
                                onWeekdayToggle = { weekdays = toggle(weekdays, it) },
                                onMonthDayToggle = { monthDays = toggle(monthDays, it) },
                                onCarry = { carry = it },
                                onSave = {
                                    scope.launch {
                                        val current = editingPlan
                                        if (current == null) {
                                            container.planRepository.addPlan(title, note, rule, weekdays, monthDays, interval.toIntOrNull() ?: 1, selectedDate, carry)
                                        } else {
                                            container.planRepository.updatePlan(current, title, note, rule, weekdays, monthDays, interval.toIntOrNull() ?: 1, carry)
                                        }
                                        resetPlanEditor()
                                    }
                                },
                                onCancel = if (editingPlan == null) null else ::resetPlanEditor
                            )
                            PlanListPanel(
                                plans = plans,
                                onEdit = { plan ->
                                    editingPlan = plan
                                    title = plan.title
                                    note = plan.note
                                    rule = plan.ruleType
                                    interval = plan.intervalDays.toString()
                                    weekdays = parseIntSet(plan.selectedWeekdays).ifEmpty { setOf(today.dayOfWeek.value) }
                                    monthDays = parseIntSet(plan.selectedMonthDays).ifEmpty { setOf(today.dayOfMonth) }
                                    carry = plan.carryStrategy
                                },
                                onDelete = { plan ->
                                    scope.launch {
                                        container.planRepository.deletePlan(plan.id)
                                        if (editingPlan?.id == plan.id) resetPlanEditor()
                                    }
                                }
                            )
                        }
                        PlanCalendarColumn(
                            modifier = Modifier.weight(1.12f),
                            visibleMonth = visibleMonth,
                            selectedDate = selectedDate,
                            today = today,
                            plansByDate = monthCounts.associate { it.date to it.count },
                            workdayOverrides = displayedWorkdayOverrides,
                            calendarOverrides = calendarOverrides.associateBy { it.date },
                            selectedItems = selectedItems,
                            previewCounts = previewItems.associate { it.date to it.count },
                            calendarMessage = calendarMessage,
                            hasCalendarPermission = hasCalendarPermission,
                            isCalendarSyncing = isCalendarSyncing,
                            onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                            onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                            onSelectDate = { selectedDate = it },
                            onRequestCalendar = {
                                if (hasCalendarPermission) {
                                    scope.launch { syncDeviceCalendar() }
                                } else {
                                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }
                            },
                            onComplete = { occurrenceId -> scope.launch { container.planRepository.completeOccurrence(occurrenceId, selectedDate) } },
                            onSkip = { occurrenceId -> scope.launch { container.planRepository.skipOccurrence(occurrenceId) } }
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        PlanEditorPanel(
                            editingPlan = editingPlan,
                            title = title,
                            note = note,
                            rule = rule,
                            interval = interval,
                            weekdays = weekdays,
                            monthDays = monthDays,
                            carry = carry,
                            onTitle = { title = it },
                            onNote = { note = it },
                            onRule = { rule = it },
                            onInterval = { interval = it },
                            onWeekdayToggle = { weekdays = toggle(weekdays, it) },
                            onMonthDayToggle = { monthDays = toggle(monthDays, it) },
                            onCarry = { carry = it },
                            onSave = {
                                scope.launch {
                                    val current = editingPlan
                                    if (current == null) {
                                        container.planRepository.addPlan(title, note, rule, weekdays, monthDays, interval.toIntOrNull() ?: 1, selectedDate, carry)
                                    } else {
                                        container.planRepository.updatePlan(current, title, note, rule, weekdays, monthDays, interval.toIntOrNull() ?: 1, carry)
                                    }
                                    resetPlanEditor()
                                }
                            },
                            onCancel = if (editingPlan == null) null else ::resetPlanEditor
                        )
                        PlanListPanel(
                            plans = plans,
                            onEdit = { plan ->
                                editingPlan = plan
                                title = plan.title
                                note = plan.note
                                rule = plan.ruleType
                                interval = plan.intervalDays.toString()
                                weekdays = parseIntSet(plan.selectedWeekdays).ifEmpty { setOf(today.dayOfWeek.value) }
                                monthDays = parseIntSet(plan.selectedMonthDays).ifEmpty { setOf(today.dayOfMonth) }
                                carry = plan.carryStrategy
                            },
                            onDelete = { plan ->
                                scope.launch {
                                    container.planRepository.deletePlan(plan.id)
                                    if (editingPlan?.id == plan.id) resetPlanEditor()
                                }
                            }
                        )
                        PlanCalendarColumn(
                            visibleMonth = visibleMonth,
                            selectedDate = selectedDate,
                            today = today,
                            plansByDate = monthCounts.associate { it.date to it.count },
                            workdayOverrides = displayedWorkdayOverrides,
                            calendarOverrides = calendarOverrides.associateBy { it.date },
                            selectedItems = selectedItems,
                            previewCounts = previewItems.associate { it.date to it.count },
                            calendarMessage = calendarMessage,
                            hasCalendarPermission = hasCalendarPermission,
                            isCalendarSyncing = isCalendarSyncing,
                            onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
                            onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
                            onSelectDate = { selectedDate = it },
                            onRequestCalendar = {
                                if (hasCalendarPermission) {
                                    scope.launch { syncDeviceCalendar() }
                                } else {
                                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }
                            },
                            onComplete = { occurrenceId -> scope.launch { container.planRepository.completeOccurrence(occurrenceId, selectedDate) } },
                            onSkip = { occurrenceId -> scope.launch { container.planRepository.skipOccurrence(occurrenceId) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanEditorPanel(
    editingPlan: PlanEntity?,
    title: String,
    note: String,
    rule: PlanRuleType,
    interval: String,
    weekdays: Set<Int>,
    monthDays: Set<Int>,
    carry: CarryStrategy,
    onTitle: (String) -> Unit,
    onNote: (String) -> Unit,
    onRule: (PlanRuleType) -> Unit,
    onInterval: (String) -> Unit,
    onWeekdayToggle: (Int) -> Unit,
    onMonthDayToggle: (Int) -> Unit,
    onCarry: (CarryStrategy) -> Unit,
    onSave: () -> Unit,
    onCancel: (() -> Unit)?
) {
    PlanEditor(
        title = title,
        note = note,
        rule = rule,
        interval = interval,
        weekdays = weekdays,
        monthDays = monthDays,
        carry = carry,
        onTitle = onTitle,
        onNote = onNote,
        onRule = onRule,
        onInterval = onInterval,
        onWeekdayToggle = onWeekdayToggle,
        onMonthDayToggle = onMonthDayToggle,
        onCarry = onCarry,
        onSave = onSave,
        onCancel = onCancel,
        label = if (editingPlan == null) "计划编辑" else "编辑计划"
    )
}

@Composable
private fun PlanListPanel(
    plans: List<PlanEntity>,
    onEdit: (PlanEntity) -> Unit,
    onDelete: (PlanEntity) -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("📋", "我的计划")
            if (plans.isEmpty()) {
                Text("暂无计划，请添加", color = AppColors.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                plans.forEach { plan ->
                    PlanRow(
                        plan = plan,
                        onEdit = { onEdit(plan) },
                        onDelete = { onDelete(plan) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanEditor(
    title: String,
    note: String,
    rule: PlanRuleType,
    interval: String,
    weekdays: Set<Int>,
    monthDays: Set<Int>,
    carry: CarryStrategy,
    onTitle: (String) -> Unit,
    onNote: (String) -> Unit,
    onRule: (PlanRuleType) -> Unit,
    onInterval: (String) -> Unit,
    onWeekdayToggle: (Int) -> Unit,
    onMonthDayToggle: (Int) -> Unit,
    onCarry: (CarryStrategy) -> Unit,
    onSave: () -> Unit,
    onCancel: (() -> Unit)? = null,
    label: String = if (onCancel == null) "计划内容" else "编辑计划"
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("📌", label)
            UnderlineField(title, onTitle, "计划名称")
            UnderlineField(note, onNote, "备注")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanRuleType.entries.forEach { item -> FilterPill(item.label(), selected = rule == item, onClick = { onRule(item) }) }
            }
            when (rule) {
                PlanRuleType.Weekly -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { day -> FilterPill("周${weekdayName(day)}", selected = day in weekdays, onClick = { onWeekdayToggle(day) }) }
                }
                PlanRuleType.Monthly -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..31).forEach { day -> FilterPill(day.toString(), selected = day in monthDays, onClick = { onMonthDayToggle(day) }) }
                }
                PlanRuleType.EveryNDays -> UnderlineField(interval, onInterval, "每 N 天", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                else -> Unit
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("未完成处理", color = AppColors.Text, modifier = Modifier.weight(1f))
                FilterPill("顺延", selected = carry == CarryStrategy.CarryNextDay, onClick = { onCarry(CarryStrategy.CarryNextDay) })
                FilterPill("跳过", selected = carry == CarryStrategy.Skip, onClick = { onCarry(CarryStrategy.Skip) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(if (onCancel == null) "保存计划" else "保存修改", onSave, enabled = title.isNotBlank(), modifier = Modifier.weight(1f))
                if (onCancel != null) {
                    TextButton(onClick = onCancel) { Text("取消") }
                }
            }
        }
    }
}

@Composable
private fun PlanRow(plan: PlanEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppColors.Border, CircleShape)
            .padding(start = 12.dp, top = 8.dp, end = 6.dp, bottom = 8.dp)
    ) {
        Icon(Icons.Default.EventRepeat, contentDescription = null, tint = AppColors.Blue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(plan.title, color = AppColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(plan.ruleType.label(), color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = onEdit) { Text("编辑") }
        TextButton(onClick = onDelete) { Text("删除", color = AppColors.Red) }
    }
}

private fun PlanRuleType.label(): String = when (this) {
    PlanRuleType.Daily -> "每天"
    PlanRuleType.Weekly -> "每周"
    PlanRuleType.Monthly -> "每月"
    PlanRuleType.EveryNDays -> "每N天"
    PlanRuleType.LegalWorkday -> "法定工作日"
}

private fun weekdayName(day: Int): String =
    java.time.DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, Locale.CHINESE).removePrefix("周")

private fun <T> toggle(set: Set<T>, value: T): Set<T> =
    if (value in set) set - value else set + value

private fun parseIntSet(value: String): Set<Int> =
    value.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
