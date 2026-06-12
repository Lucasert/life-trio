package com.lifetrio.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.lifetrio.ui.components.EditorSheet
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.FilterPill
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.components.WarningBanner
import com.lifetrio.ui.theme.Spacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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
    var isCalendarSyncing by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<PlanEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

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

    val onEditPlan: (PlanEntity) -> Unit = { plan ->
        editingPlan = plan
        showEditor = true
    }
    val onDeletePlan: (PlanEntity) -> Unit = { plan ->
        scope.launch {
            container.planRepository.deletePlan(plan.id)
            if (editingPlan?.id == plan.id) {
                editingPlan = null
                showEditor = false
            }
        }
    }
    val onRequestCalendar: () -> Unit = {
        if (hasCalendarPermission) {
            scope.launch { syncDeviceCalendar() }
        } else {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    @Composable
    fun CalendarColumn(modifier: Modifier = Modifier) {
        PlanCalendarColumn(
            modifier = modifier,
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
            onRequestCalendar = onRequestCalendar,
            onComplete = { occurrenceId -> scope.launch { container.planRepository.completeOccurrence(occurrenceId, selectedDate) } },
            onSkip = { occurrenceId -> scope.launch { container.planRepository.skipOccurrence(occurrenceId) } }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingPlan = null
                showEditor = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增计划")
            }
        }
    ) { padding ->
        AppPage(modifier = Modifier.padding(padding)) {
            item { ScreenHeader("计划", "周期计划 · 日历同步 · 待办追踪") }
            if (!container.planRepository.hasWorkdayCalendarFor(today.year)) {
                item { WarningBanner("当前年份缺少中国法定工作日表，请维护节假日数据。") }
            }
            item {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val wide = maxWidth >= 760.dp
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg), modifier = Modifier.fillMaxWidth()) {
                            CalendarColumn(Modifier.weight(1.12f))
                            PlanListPanel(plans, onEditPlan, onDeletePlan, Modifier.weight(0.72f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md), modifier = Modifier.fillMaxWidth()) {
                            CalendarColumn()
                            PlanListPanel(plans, onEditPlan, onDeletePlan)
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        PlanEditorSheet(
            editingPlan = editingPlan,
            today = today,
            onDismiss = {
                showEditor = false
                editingPlan = null
            },
            onSubmit = { title, note, rule, weekdays, monthDays, interval, carry ->
                scope.launch {
                    val current = editingPlan
                    if (current == null) {
                        container.planRepository.addPlan(title, note, rule, weekdays, monthDays, interval, selectedDate, carry)
                    } else {
                        container.planRepository.updatePlan(current, title, note, rule, weekdays, monthDays, interval, carry)
                    }
                    showEditor = false
                    editingPlan = null
                }
            }
        )
    }
}

@Composable
private fun PlanListPanel(
    plans: List<PlanEntity>,
    onEdit: (PlanEntity) -> Unit,
    onDelete: (PlanEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FieldLabel(Icons.Outlined.ListAlt, "我的计划")
            if (plans.isEmpty()) {
                Text("暂无计划，点击右下角按钮添加", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
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
private fun PlanEditorSheet(
    editingPlan: PlanEntity?,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSubmit: (String, String, PlanRuleType, Set<Int>, Set<Int>, Int, CarryStrategy) -> Unit
) {
    var title by remember(editingPlan?.id) { mutableStateOf(editingPlan?.title ?: "") }
    var note by remember(editingPlan?.id) { mutableStateOf(editingPlan?.note ?: "") }
    var rule by remember(editingPlan?.id) { mutableStateOf(editingPlan?.ruleType ?: PlanRuleType.Daily) }
    var interval by remember(editingPlan?.id) { mutableStateOf(editingPlan?.intervalDays?.toString() ?: "2") }
    var weekdays by remember(editingPlan?.id) {
        mutableStateOf(editingPlan?.let { parseIntSet(it.selectedWeekdays) }?.ifEmpty { setOf(today.dayOfWeek.value) } ?: setOf(today.dayOfWeek.value))
    }
    var monthDays by remember(editingPlan?.id) {
        mutableStateOf(editingPlan?.let { parseIntSet(it.selectedMonthDays) }?.ifEmpty { setOf(today.dayOfMonth) } ?: setOf(today.dayOfMonth))
    }
    var carry by remember(editingPlan?.id) { mutableStateOf(editingPlan?.carryStrategy ?: CarryStrategy.CarryNextDay) }

    EditorSheet(title = if (editingPlan == null) "新增计划" else "编辑计划", onDismiss = onDismiss) {
        UnderlineField(title, { title = it }, "计划名称")
        UnderlineField(note, { note = it }, "备注")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PlanRuleType.entries.forEach { item -> FilterPill(item.label(), selected = rule == item, onClick = { rule = item }) }
        }
        when (rule) {
            PlanRuleType.Weekly -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                (1..7).forEach { day -> FilterPill("周${weekdayName(day)}", selected = day in weekdays, onClick = { weekdays = toggle(weekdays, day) }) }
            }
            PlanRuleType.Monthly -> FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                (1..31).forEach { day -> FilterPill(day.toString(), selected = day in monthDays, onClick = { monthDays = toggle(monthDays, day) }) }
            }
            PlanRuleType.EveryNDays -> UnderlineField(interval, { interval = it }, "每 N 天", keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            else -> Unit
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text("未完成处理", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            FilterPill("顺延", selected = carry == CarryStrategy.CarryNextDay, onClick = { carry = CarryStrategy.CarryNextDay })
            FilterPill("跳过", selected = carry == CarryStrategy.Skip, onClick = { carry = CarryStrategy.Skip })
        }
        PrimaryButton(
            if (editingPlan == null) "保存计划" else "保存修改",
            { onSubmit(title, note, rule, weekdays, monthDays, interval.toIntOrNull() ?: 1, carry) },
            enabled = title.isNotBlank()
        )
    }
}

@Composable
private fun PlanRow(plan: PlanEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .padding(start = Spacing.sm, top = Spacing.xxs, end = Spacing.xxs, bottom = Spacing.xxs)
    ) {
        Icon(Icons.Outlined.EventRepeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(plan.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(plan.ruleType.label(), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "编辑") }
        IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
    }
}

internal fun PlanRuleType.label(): String = when (this) {
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
