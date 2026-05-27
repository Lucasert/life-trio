package com.lifetrio.ui.screens

import android.Manifest
import android.app.KeyguardManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.lifetrio.core.data.AppContainer
import com.lifetrio.core.data.db.dao.CategoryTotal
import com.lifetrio.core.data.db.dao.MonthTotal
import com.lifetrio.core.data.db.entity.CarryStrategy
import com.lifetrio.core.data.db.entity.LedgerEntryEntity
import com.lifetrio.core.data.db.entity.LedgerType
import com.lifetrio.core.data.db.entity.MemoEntity
import com.lifetrio.core.data.db.entity.PlanEntity
import com.lifetrio.core.data.db.entity.PlanRuleType
import com.lifetrio.core.data.db.entity.toAmountCents
import com.lifetrio.core.data.db.entity.toYuanText
import com.lifetrio.password.PasswordRecord
import com.lifetrio.plan.calendar.DeviceCalendarDayOverride
import com.lifetrio.plan.calendar.DeviceCalendarReader
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.DashedUploadBox
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.FilterPill
import com.lifetrio.ui.components.PillSearchField
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SoftChip
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(container: AppContainer, navController: NavHostController, ledgerRoute: String) {
    val month = remember { YearMonth.now() }
    val budget by container.ledgerRepository.observeBudgetState(month).collectAsState(initial = null)
    val todayPlans by container.planRepository.observeToday().collectAsState(initial = emptyList())
    val memos by container.memoRepository.observeAll().collectAsState(initial = emptyList())
    val entries by container.ledgerRepository.observeThisMonthEntries().collectAsState(initial = emptyList())

    AppPage {
        item { ScreenHeader("life-trio", "把记录、账目、计划和密码收进一个地方") }
        item {
            AppCard(danger = budget?.isWarning == true, onClick = { navController.navigate(ledgerRoute) }) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FieldLabel("💰", "本月预算")
                    Text(
                        budget?.let { "剩余 ${it.remainingCents.toYuanText()} / ${it.budgetCents.toYuanText()} 元" } ?: "尚未设置预算",
                        color = AppColors.Text,
                        fontWeight = FontWeight.SemiBold
                    )
                    val progress = budget?.let { it.spentCents.toFloat() / it.budgetCents.coerceAtLeast(1) }?.coerceIn(0f, 1f) ?: 0f
                    Meter(progress, budget?.isWarning == true)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard("📝", "备忘", memos.size.toString(), Modifier.weight(1f))
                StatCard("💸", "支出", entries.filter { it.type == LedgerType.Expense }.sumOf { it.amountCents }.toYuanText(), Modifier.weight(1f))
                StatCard("📅", "待办", todayPlans.size.toString(), Modifier.weight(1f))
            }
        }
        item { SectionTitle("今日待办") }
        if (todayPlans.isEmpty()) {
            item { EmptyState("今天没有待办", "去计划页添加一个周期任务", "✅") }
        }
        items(todayPlans.take(5), key = { "home-plan-${it.occurrenceId}" }) { item ->
            CompactPlanItem(item.title, item.note, item.status.name)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoScreen(container: AppContainer, navController: NavHostController, planRoute: String) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingCameraImagePath by remember { mutableStateOf<String?>(null) }
    var selectedMemo by remember { mutableStateOf<MemoEntity?>(null) }
    val memos by container.memoRepository.search(query).collectAsState(initial = emptyList())

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val uri = result.data?.data
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val copyResult = runCatching { copyImageToPrivateStorage(context, uri) }
            copyResult.onSuccess { imageUris = imageUris + it }
            copyResult.onFailure { error ->
                Log.e("LifeTrioMemo", "Image copy failed", error)
                snackbarHostState.showSnackbar("图片添加失败：${error.javaClass.simpleName}")
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        val path = pendingCameraImagePath
        if (result.resultCode == android.app.Activity.RESULT_OK && path != null) {
            imageUris = imageUris + path
        } else if (path != null) {
            File(path).delete()
        }
        pendingCameraImagePath = null
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (text.isNotBlank()) body = "$body$text"
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        AppPage(modifier = Modifier.padding(padding)) {
            item { ScreenHeader("备忘", "捕捉灵感・管理日常") }
            if (selectedMemo == null) {
                item { PillSearchField(query, { query = it }, "搜索标题、正文或标签") }
                item {
                    MemoEditorCard(
                        title = title,
                        body = body,
                        tags = tags,
                        pinned = pinned,
                        imageCount = imageUris.size,
                        onTitle = { title = it },
                        onBody = { body = it },
                        onTags = { tags = it },
                        onPinned = { pinned = it },
                        onBold = { body += "**加粗文本**" },
                        onList = { body += "\n- " },
                        onTodo = { body += "\n- [ ] " },
                        onPickImage = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "image/*"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            }
                            runCatching { imagePicker.launch(Intent.createChooser(intent, "选择图片")) }
                                .onFailure { error ->
                                    Log.e("LifeTrioMemo", "Image picker launch failed", error)
                                    scope.launch { snackbarHostState.showSnackbar("图片选择失败：${error.javaClass.simpleName}") }
                                }
                        },
                        onTakePhoto = {
                            val photoFile = createMemoImageFile(context)
                            val photoUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            val cameraIntent = createCameraIntent(context, photoUri)
                            pendingCameraImagePath = photoFile.absolutePath
                            runCatching { cameraLauncher.launch(cameraIntent) }
                                .onFailure {
                                    pendingCameraImagePath = null
                                    photoFile.delete()
                                    scope.launch { snackbarHostState.showSnackbar("当前设备不可用相机") }
                                }
                        },
                        onVoice = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toLanguageTag())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "说出要记录的内容")
                            }
                            if (intent.resolveActivity(context.packageManager) == null) {
                                scope.launch { snackbarHostState.showSnackbar("当前设备不可用语音识别") }
                                return@MemoEditorCard
                            }
                            runCatching { speechLauncher.launch(intent) }
                                .onFailure {
                                    scope.launch { snackbarHostState.showSnackbar("当前设备不可用语音识别") }
                                }
                        },
                        onSave = {
                            scope.launch {
                                container.memoRepository.saveMemo(
                                    id = editingId,
                                    title = title,
                                    body = body,
                                    tags = tags.split(",", "，"),
                                    isPinned = pinned,
                                    imageUris = imageUris
                                )
                                editingId = null
                                title = ""
                                body = ""
                                tags = ""
                                pinned = false
                                imageUris = emptyList()
                                snackbarHostState.showSnackbar("已保存备忘")
                            }
                        }
                    )
                }
                if (memos.isEmpty()) {
                    item { EmptyState("暂无备忘", "先写下一条随手记", "📝") }
                }
                items(memos, key = { "memo-${it.id}" }) { memo ->
                    MemoCard(
                        memo = memo,
                        onOpen = { selectedMemo = memo },
                        onEdit = {
                            selectedMemo = null
                            editingId = memo.id
                            title = memo.title
                            body = memo.body
                            pinned = memo.isPinned
                            imageUris = memo.imageUris.split("|").filter { it.isNotBlank() }
                            scope.launch { tags = container.memoRepository.tagsForMemo(memo.id).joinToString(",") { it.name } }
                        },
                        onDelete = {
                            scope.launch {
                                container.memoRepository.deleteMemo(memo.id)
                                if (selectedMemo?.id == memo.id) selectedMemo = null
                                if (editingId == memo.id) {
                                    editingId = null
                                    title = ""
                                    body = ""
                                    tags = ""
                                    pinned = false
                                    imageUris = emptyList()
                                }
                                snackbarHostState.showSnackbar("已删除备忘")
                            }
                        },
                        onToPlan = {
                            scope.launch {
                                container.planRepository.addPlan(
                                    title = memo.title,
                                    note = memo.body.take(120),
                                    ruleType = PlanRuleType.Daily,
                                    weekdays = emptySet(),
                                    monthDays = emptySet(),
                                    intervalDays = 1,
                                    startDate = LocalDate.now(),
                                    carryStrategy = CarryStrategy.CarryNextDay,
                                    sourceMemoId = memo.id
                                )
                                navController.navigate(planRoute)
                            }
                        }
                    )
                }
            } else {
                selectedMemo?.let { memo ->
                    item(key = "memo-detail-${memo.id}") {
                        MemoDetailCard(
                            memo = memo,
                            onBack = { selectedMemo = null },
                            onEdit = {
                                selectedMemo = null
                                editingId = memo.id
                                title = memo.title
                                body = memo.body
                                pinned = memo.isPinned
                                imageUris = memo.imageUris.split("|").filter { it.isNotBlank() }
                                scope.launch { tags = container.memoRepository.tagsForMemo(memo.id).joinToString(",") { it.name } }
                            },
                            onDelete = {
                                scope.launch {
                                    container.memoRepository.deleteMemo(memo.id)
                                    selectedMemo = null
                                    if (editingId == memo.id) {
                                        editingId = null
                                        title = ""
                                        body = ""
                                        tags = ""
                                        pinned = false
                                        imageUris = emptyList()
                                    }
                                    snackbarHostState.showSnackbar("已删除备忘")
                                }
                            },
                            onToPlan = {
                                scope.launch {
                                    container.planRepository.addPlan(
                                        title = memo.title,
                                        note = memo.body.take(120),
                                        ruleType = PlanRuleType.Daily,
                                        weekdays = emptySet(),
                                        monthDays = emptySet(),
                                        intervalDays = 1,
                                        startDate = LocalDate.now(),
                                        carryStrategy = CarryStrategy.CarryNextDay,
                                        sourceMemoId = memo.id
                                    )
                                    selectedMemo = null
                                    navController.navigate(planRoute)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LedgerScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val month = remember { YearMonth.now() }
    val entries by container.ledgerRepository.observeThisMonthEntries().collectAsState(initial = emptyList())
    val budget by container.ledgerRepository.observeBudgetState(month).collectAsState(initial = null)
    val categories by container.ledgerRepository.observeCategoryTotals(month).collectAsState(initial = emptyList())
    val yearTotals by container.ledgerRepository.observeYearTotals(LocalDate.now().year).collectAsState(initial = emptyList())
    var type by remember { mutableStateOf(LedgerType.Expense) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("餐饮") }
    var note by remember { mutableStateOf("") }
    var budgetText by remember { mutableStateOf("") }
    var warningRatio by remember { mutableFloatStateOf(0.8f) }

    AppPage {
        item { ScreenHeader("记账", "3 秒记录一笔收支") }
        item {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterPill("支出", selected = type == LedgerType.Expense, onClick = {
                            type = LedgerType.Expense
                            category = "餐饮"
                        })
                        FilterPill("收入", selected = type == LedgerType.Income, onClick = {
                            type = LedgerType.Income
                            category = "工资"
                        })
                    }
                    CategoryChips(selected = category, onSelected = { category = it }, type = type)
                    UnderlineField(amount, { amount = it }, "金额", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    UnderlineField(note, { note = it }, "备注")
                    PrimaryButton("💾 记一笔", {
                        scope.launch {
                            container.ledgerRepository.addEntry(type, category, amount.toAmountCents(), note, LocalDate.now())
                            amount = ""
                            note = ""
                            type = LedgerType.Expense
                            category = "餐饮"
                        }
                    }, enabled = amount.toDoubleOrNull() != null)
                }
            }
        }
        item {
            BudgetCard(
                current = budget,
                budgetText = budgetText,
                warningRatio = warningRatio,
                onBudgetText = { budgetText = it },
                onWarningRatio = { warningRatio = it },
                onSave = {
                    scope.launch {
                        container.ledgerRepository.setBudget(month, budgetText.toAmountCents(), warningRatio)
                        budgetText = ""
                    }
                }
            )
        }
        item { SectionTitle("月度支出分类") }
        item { AppCard { PieChart(categories) } }
        item { SectionTitle("年度收支") }
        item { AppCard { LineChart(yearTotals) } }
        item { SectionTitle("本月流水") }
        if (entries.isEmpty()) {
            item { EmptyState("暂无流水", "添加一笔收支后会在这里显示", "💰") }
        }
        items(entries, key = { "ledger-${it.id}" }) { entry ->
            LedgerEntryRow(entry) { scope.launch { container.ledgerRepository.deleteEntry(entry.id) } }
        }
    }
}

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
    val heatmap by container.planRepository.observeHeatmap(today.minusDays(179), today).collectAsState(initial = emptyList())
    val overrides by container.planRepository.observeWorkdayOverrides().collectAsState(initial = emptyList())
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

    val calendarPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCalendarPermission = granted
        calendarMessage = if (granted) "已授权，准备同步手机日历" else "未授权手机日历，法定工作日使用内置规则"
    }

    suspend fun syncDeviceCalendar() {
        if (!hasCalendarPermission) {
            calendarMessage = "未授权手机日历，法定工作日使用内置规则"
            return
        }
        val start = monthStart
        val end = maxOf(monthEnd, previewEnd)
        val entries = withContext(Dispatchers.IO) {
            DeviceCalendarReader(context).readWorkdayOverrides(start, end)
        }
        calendarOverrides = entries
        entries.forEach { container.planRepository.setWorkdayOverride(it.date, it.isWorkday) }
        container.planRepository.generateOccurrences(start, end)
        calendarMessage = if (entries.isEmpty()) "手机日历没有识别到班休事件" else "已同步 ${entries.size} 条手机日历班休"
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
            ScreenHeader("计划", "周期计划 · 打卡追踪 · 热点洞察")
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
                            completionsByDate = heatmap.associate { it.date to it.count },
                            workdayOverrides = overrides.associate { it.date to it.isWorkday },
                            calendarOverrides = calendarOverrides.associateBy { it.date },
                            selectedItems = selectedItems,
                            previewCounts = previewItems.associate { it.date to it.count },
                            calendarMessage = calendarMessage,
                            hasCalendarPermission = hasCalendarPermission,
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
                            completionsByDate = heatmap.associate { it.date to it.count },
                            workdayOverrides = overrides.associate { it.date to it.isWorkday },
                            calendarOverrides = calendarOverrides.associateBy { it.date },
                            selectedItems = selectedItems,
                            previewCounts = previewItems.associate { it.date to it.count },
                            calendarMessage = calendarMessage,
                            hasCalendarPermission = hasCalendarPermission,
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
fun PasswordScreen(container: AppContainer) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val repository = container.passwordVaultRepository
    val isUnlocked by repository.isUnlocked.collectAsState()
    val records by repository.records.collectAsState()
    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<PasswordRecord?>(null) }
    var selected by remember { mutableStateOf<PasswordRecord?>(null) }

    DisposableEffect(lifecycleOwner, repository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                repository.lock()
                editing = null
                selected = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            repository.lock()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        if (!isUnlocked) {
            LockedPasswordScreen(
                modifier = Modifier.padding(padding),
                onUnlock = {
                    if (activity == null) {
                        scope.launch { snackbarHostState.showSnackbar("无法启动本机验证") }
                    } else {
                        requestPasswordVaultAuth(
                            activity,
                            onSuccess = {
                                scope.launch {
                                    runCatching { repository.unlock() }
                                        .onFailure { snackbarHostState.showSnackbar("保险库无法解密") }
                                }
                            },
                            onError = { scope.launch { snackbarHostState.showSnackbar(it) } }
                        )
                    }
                }
            )
        } else {
            PasswordVaultContent(
                modifier = Modifier.padding(padding),
                records = records.filter { it.matches(query) },
                query = query,
                onQuery = { query = it },
                editing = editing,
                selected = selected,
                onNew = {
                    selected = null
                    editing = PasswordRecord(name = "", account = "", secret = "", target = "")
                },
                onEdit = {
                    selected = null
                    editing = it
                },
                onSelect = {
                    editing = null
                    selected = it
                },
                onCancelEdit = { editing = null },
                onSave = {
                    scope.launch {
                        repository.save(it)
                        editing = null
                        snackbarHostState.showSnackbar("已保存")
                    }
                },
                onDelete = {
                    scope.launch {
                        repository.delete(it.id)
                        selected = null
                        editing = null
                        snackbarHostState.showSnackbar("已删除")
                    }
                },
                onCopyAccount = {
                    copyToClipboard(context, "life-trio 账号", it)
                    scope.launch { snackbarHostState.showSnackbar("账号已复制") }
                },
                onCopySecret = {
                    copyToClipboard(context, "life-trio 密码", it)
                    scope.launch {
                        snackbarHostState.showSnackbar("密码已复制，30 秒后清空剪贴板")
                        delay(30_000)
                        clearClipboardIfValueMatches(context, it)
                    }
                }
            )
        }
    }
}

@Composable
private fun StatCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(emoji)
            Text(label, color = AppColors.Muted, style = MaterialTheme.typography.labelMedium)
            Text(value, color = AppColors.Text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = AppColors.Text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoEditorCard(
    title: String,
    body: String,
    tags: String,
    pinned: Boolean,
    imageCount: Int,
    onTitle: (String) -> Unit,
    onBody: (String) -> Unit,
    onTags: (String) -> Unit,
    onPinned: (Boolean) -> Unit,
    onBold: () -> Unit,
    onList: () -> Unit,
    onTodo: () -> Unit,
    onPickImage: () -> Unit,
    onTakePhoto: () -> Unit,
    onVoice: () -> Unit,
    onSave: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            FieldLabel("📌", "标题")
            UnderlineField(title, onTitle, "咖啡馆备忘")
            FieldLabel("✍️", "正文")
            UnderlineField(body, onBody, "随手记", modifier = Modifier.height(110.dp), singleLine = false)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBold) { Text("B", fontWeight = FontWeight.Bold) }
                IconButton(onClick = onList) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "列表") }
                IconButton(onClick = onTodo) { Icon(Icons.Default.CheckBox, contentDescription = "待办") }
                IconButton(onClick = onVoice) { Icon(Icons.Default.Mic, contentDescription = "语音") }
                IconButton(onClick = { onPinned(!pinned) }) {
                    Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = if (pinned) AppColors.Blue else AppColors.Muted)
                }
            }
            FieldLabel("🏷️", "标签（点击 + 添加）")
            TagEditor(tags, onTags)
            FieldLabel("🖼️", "图片附件")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                DashedUploadBox(
                    "+ 添加图片",
                    if (imageCount == 0) "🖼️" else "$imageCount 张",
                    onPickImage,
                    modifier = Modifier.weight(1f)
                )
                DashedUploadBox(
                    "拍照",
                    "📷",
                    onTakePhoto,
                    modifier = Modifier.weight(1f)
                )
            }
            PrimaryButton("💾 保存笔记", onSave, enabled = title.isNotBlank() || body.isNotBlank())
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagEditor(value: String, onValueChange: (String) -> Unit) {
    val tags = value.split(",", "，").map { it.trim() }.filter { it.isNotBlank() }.distinct()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tags.forEach { tag -> SoftChip(tag) }
        }
        UnderlineField(value, onValueChange, "生活，账单")
    }
}

@Composable
private fun MemoCard(memo: MemoEntity, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onToPlan: () -> Unit) {
    AppCard(onClick = onOpen) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (memo.isPinned) Icon(Icons.Default.Star, contentDescription = "置顶", tint = AppColors.Yellow, modifier = Modifier.size(18.dp))
                Text(memo.title, color = AppColors.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Text(memo.body, maxLines = 4, overflow = TextOverflow.Ellipsis, color = AppColors.Text)
            if (memo.imageUris.isNotBlank()) Text("含 ${memo.imageUris.split("|").count { it.isNotBlank() }} 张图片", color = AppColors.Muted)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = AppColors.Red) }
                TextButton(onClick = onToPlan) { Text("转计划") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoDetailCard(
    memo: MemoEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToPlan: () -> Unit
) {
    val images = memo.imageUris.split("|").filter { it.isNotBlank() }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBack) { Text("返回") }
                Spacer(Modifier.width(6.dp))
                Text("备忘详情", color = AppColors.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (memo.isPinned) Icon(Icons.Default.Star, contentDescription = "置顶", tint = AppColors.Yellow, modifier = Modifier.size(20.dp))
            }
            Text(memo.title.ifBlank { "未命名备忘" }, color = AppColors.Text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            if (memo.body.isBlank()) {
                Text("暂无正文", color = AppColors.Muted)
            } else {
                Text(memo.body, color = AppColors.Text)
            }
            Text("更新于 ${memo.updatedAt.toString().replace("T", " ").substringBefore(".")}", color = AppColors.Muted)
            FieldLabel("🖼️", "图片附件")
            if (images.isEmpty()) {
                Text("暂无图片", color = AppColors.Muted)
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    images.forEach { path -> MemoImagePreview(path) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterPill("编辑", false, onEdit)
                FilterPill("转计划", false, onToPlan)
                TextButton(onClick = onDelete) { Text("删除", color = AppColors.Red) }
            }
        }
    }
}

@Composable
private fun MemoImagePreview(path: String) {
    val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
    if (bitmap == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(AppColors.BlueSoft, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("图片无法读取", color = AppColors.Muted)
        }
    } else {
        Image(
            bitmap = bitmap,
            contentDescription = "备忘图片",
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(selected: String, onSelected: (String) -> Unit, type: LedgerType) {
    val options = if (type == LedgerType.Expense) {
        listOf("🍜 餐饮", "🚇 交通", "🛍️ 购物", "🏠 住房", "🎮 娱乐", "💊 医疗", "📚 学习")
    } else {
        listOf("💼 工资", "🎁 奖金", "🧾 报销", "📈 理财", "✨ 其他")
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val text = option.substringAfter(" ")
            FilterPill(option, selected = selected == text, onClick = { onSelected(text) })
        }
    }
}

@Composable
private fun BudgetCard(
    current: com.lifetrio.core.data.repository.BudgetState?,
    budgetText: String,
    warningRatio: Float,
    onBudgetText: (String) -> Unit,
    onWarningRatio: (Float) -> Unit,
    onSave: () -> Unit
) {
    AppCard(danger = current?.isWarning == true) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("🚨", "预算预警")
            Text(current?.let { "剩余 ${it.remainingCents.toYuanText()} 元，已用 ${it.spentCents.toYuanText()} 元" } ?: "尚未设置本月预算", color = AppColors.Text)
            Meter(current?.let { it.spentCents.toFloat() / it.budgetCents.coerceAtLeast(1) }?.coerceIn(0f, 1f) ?: 0f, current?.isWarning == true)
            UnderlineField(budgetText, onBudgetText, "本月预算金额", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            Text("预警阈值 ${(warningRatio * 100).toInt()}%", color = AppColors.Muted)
            Slider(warningRatio, onValueChange = onWarningRatio, valueRange = 0.5f..1f)
            PrimaryButton("保存预算", onSave, enabled = budgetText.toDoubleOrNull() != null)
        }
    }
}

@Composable
private fun LedgerEntryRow(entry: LedgerEntryEntity, onDelete: () -> Unit) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.category, color = AppColors.Text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text((if (entry.type == LedgerType.Expense) "-" else "+") + entry.amountCents.toYuanText(), color = if (entry.type == LedgerType.Expense) AppColors.Red else AppColors.Green, fontWeight = FontWeight.Bold)
            }
            if (entry.note.isNotBlank()) Text(entry.note, color = AppColors.Muted)
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDelete) { Text("删除", color = AppColors.Red) }
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

@Composable
private fun PlanCalendarColumn(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    plansByDate: Map<LocalDate, Int>,
    completionsByDate: Map<LocalDate, Int>,
    workdayOverrides: Map<LocalDate, Boolean>,
    calendarOverrides: Map<LocalDate, DeviceCalendarDayOverride>,
    selectedItems: List<com.lifetrio.core.data.db.dao.PlanWithOccurrence>,
    previewCounts: Map<LocalDate, Int>,
    calendarMessage: String,
    hasCalendarPermission: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onRequestCalendar: () -> Unit,
    onComplete: (Long) -> Unit,
    onSkip: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = modifier) {
        CalendarPanel(
            visibleMonth = visibleMonth,
            selectedDate = selectedDate,
            today = today,
            plansByDate = plansByDate,
            workdayOverrides = workdayOverrides,
            calendarOverrides = calendarOverrides,
            calendarMessage = calendarMessage,
            hasCalendarPermission = hasCalendarPermission,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectDate = onSelectDate,
            onRequestCalendar = onRequestCalendar
        )
        SelectedDatePlanPanel(selectedDate, selectedItems, onComplete, onSkip)
        WeekPreviewPanel(selectedDate, previewCounts)
        HeatmapPanel(today = today, completionsByDate = completionsByDate, selectedDate = selectedDate, onSelectDate = onSelectDate)
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
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onRequestCalendar: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("🗓️", "日历 & 打卡")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                FilterPill("◀ 上月", selected = false, onClick = onPreviousMonth)
                Text("${visibleMonth.year}年 ${visibleMonth.monthValue}月", color = AppColors.Text, fontWeight = FontWeight.Black)
                FilterPill("下月 ▶", selected = false, onClick = onNextMonth)
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
                Text(calendarMessage, color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextButton(onClick = onRequestCalendar) {
                    Text(if (hasCalendarPermission) "同步手机日历" else "连接手机日历")
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
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                Text(day, color = AppColors.Muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        weeks.forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
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
    val borderColor = when {
        selected -> AppColors.Blue
        isToday -> Color(0xFF93C5FD)
        else -> AppColors.Border
    }
    val background = when {
        selected -> AppColors.BlueSoft
        planCount > 0 -> Color(0xFFF0FDF4)
        else -> AppColors.Surface
    }
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(background, CircleShape)
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = CircleShape)
            .clickable(enabled = date != null, onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (date != null) {
            Text(date.dayOfMonth.toString(), color = AppColors.Text, fontWeight = if (selected || isToday) FontWeight.Bold else FontWeight.Normal)
            if (workdayOverride != null) {
                Text(if (workdayOverride) "班" else "休", color = if (workdayOverride) AppColors.Blue else AppColors.Red, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopEnd))
            }
            if (planCount > 0) {
                Text(planCount.toString(), color = AppColors.Green, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomCenter))
            }
            if (!deviceTitle.isNullOrBlank()) {
                Box(Modifier.size(5.dp).background(AppColors.Yellow, CircleShape).align(Alignment.TopStart))
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
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("✅", "${selectedDate} 待办")
            if (items.isEmpty()) {
                Text("无计划", color = AppColors.Muted, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                items.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(false, onCheckedChange = { onComplete(item.occurrenceId) })
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = AppColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (item.note.isNotBlank()) Text(item.note, color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FieldLabel("🔮", "未来一周计划预览")
            (0L..6L).forEach { offset ->
                val date = selectedDate.plusDays(offset)
                val count = counts[date] ?: 0
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(date.toString(), color = AppColors.Text, fontWeight = FontWeight.SemiBold)
                    Text(if (count == 0) "无计划" else "${count} 项计划", color = if (count == 0) AppColors.Muted else AppColors.Blue)
                }
            }
        }
    }
}

@Composable
private fun HeatmapPanel(
    today: LocalDate,
    completionsByDate: Map<LocalDate, Int>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val start = today.minusDays(179)
    val dates = generateSequence(start) { it.plusDays(1) }.take(180).toList()
    val columns = dates.chunked(7)
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("🔥", "打卡热点图（近180天）")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                columns.forEach { column ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                        column.forEach { date ->
                            val count = completionsByDate[date] ?: 0
                            val color = when {
                                date == selectedDate -> AppColors.Blue
                                count >= 4 -> Color(0xFF166534)
                                count >= 2 -> Color(0xFF22C55E)
                                count == 1 -> Color(0xFFBBF7D0)
                                else -> Color(0xFFE5E7EB)
                            }
                            Box(
                                Modifier
                                    .aspectRatio(1f)
                                    .background(color, CircleShape)
                                    .clickable { onSelectDate(date) }
                            )
                        }
                    }
                }
            }
            Text("颜色越深，打卡次数越多。点击热力格会联动到日历日期。", color = AppColors.Muted, style = MaterialTheme.typography.labelSmall)
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

@Composable
private fun CompactPlanItem(title: String, note: String, status: String) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.Green)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AppColors.Text, fontWeight = FontWeight.SemiBold)
                if (note.isNotBlank()) Text(note, color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AssistChip(onClick = {}, label = { Text(status) })
        }
    }
}

@Composable
private fun LockedPasswordScreen(modifier: Modifier = Modifier, onUnlock: () -> Unit) {
    Box(modifier = modifier.background(AppColors.Background)) {
        AppPage {
            item { ScreenHeader("密码", "本机验证后访问保险库") }
            item {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(Modifier.size(62.dp).background(AppColors.BlueSoft, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Password, contentDescription = null, tint = AppColors.Blue)
                        }
                        Text("需要验证本机密码或指纹", color = AppColors.Text, fontWeight = FontWeight.Bold)
                        Text("密码保险库只在验证通过后解密，离开页面或退到后台会立即锁定。", color = AppColors.Muted)
                        PrimaryButton("解锁密码管理", onUnlock)
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordVaultContent(
    modifier: Modifier,
    records: List<PasswordRecord>,
    query: String,
    onQuery: (String) -> Unit,
    editing: PasswordRecord?,
    selected: PasswordRecord?,
    onNew: () -> Unit,
    onEdit: (PasswordRecord) -> Unit,
    onSelect: (PasswordRecord) -> Unit,
    onCancelEdit: () -> Unit,
    onSave: (PasswordRecord) -> Unit,
    onDelete: (PasswordRecord) -> Unit,
    onCopyAccount: (String) -> Unit,
    onCopySecret: (String) -> Unit
) {
    AppPage(modifier = modifier) {
        item {
            ScreenHeader("密码", "账号与密钥保险库") {
                FilterPill("+ 新增", false, onNew)
            }
        }
        item { PillSearchField(query, onQuery, "搜索名称、账号、网站或应用") }
        if (editing != null) {
            item(key = "password-editor-${editing.id}") { PasswordEditor(editing, onSave, onCancelEdit) }
        }
        if (selected != null) {
            item(key = "password-detail-${selected.id}") {
                PasswordDetail(
                    record = selected,
                    onEdit = { onEdit(selected) },
                    onDelete = { onDelete(selected) },
                    onCopyAccount = { onCopyAccount(selected.account) },
                    onCopySecret = { onCopySecret(selected.secret) }
                )
            }
        }
        item { SectionTitle("全部密码") }
        if (records.isEmpty()) {
            item { EmptyState("暂无密码", "新增一个账号密码后会在这里显示", "🔐") }
        }
        items(records, key = { "password-${it.id}" }) { PasswordRow(it) { onSelect(it) } }
    }
}

@Composable
private fun PasswordEditor(record: PasswordRecord, onSave: (PasswordRecord) -> Unit, onCancel: () -> Unit) {
    var name by remember(record.id) { mutableStateOf(record.name) }
    var account by remember(record.id) { mutableStateOf(record.account) }
    var secret by remember(record.id) { mutableStateOf(record.secret) }
    var target by remember(record.id) { mutableStateOf(record.target) }
    var note by remember(record.id) { mutableStateOf(record.note) }
    var showSecret by remember(record.id) { mutableStateOf(false) }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("🔐", if (record.name.isBlank()) "新增密码" else "编辑密码")
            UnderlineField(name, { name = it }, "名称")
            UnderlineField(account, { account = it }, "账号")
            UnderlineField(
                secret,
                { secret = it },
                "密码",
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "显示或隐藏密码")
                    }
                }
            )
            UnderlineField(target, { target = it }, "网站或应用")
            UnderlineField(note, { note = it }, "备注")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PrimaryButton("保存", {
                    onSave(record.copy(name = name.trim(), account = account.trim(), secret = secret, target = target.trim(), note = note.trim()))
                }, modifier = Modifier.weight(1f), enabled = name.isNotBlank() && secret.isNotBlank())
                TextButton(onClick = onCancel) { Text("取消") }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PasswordDetail(record: PasswordRecord, onEdit: () -> Unit, onDelete: () -> Unit, onCopyAccount: () -> Unit, onCopySecret: () -> Unit) {
    var showSecret by remember(record.id) { mutableStateOf(false) }
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(record.name, color = AppColors.Text, fontWeight = FontWeight.Bold)
            Text("账号：${record.account.ifBlank { "未填写" }}", color = AppColors.Text)
            Text("网站/应用：${record.target.ifBlank { "未填写" }}", color = AppColors.Muted)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("密码：${if (showSecret) record.secret else "••••••••"}", color = AppColors.Text, modifier = Modifier.weight(1f))
                IconButton(onClick = { showSecret = !showSecret }) {
                    Icon(if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = "显示或隐藏密码")
                }
            }
            if (record.note.isNotBlank()) Text(record.note, color = AppColors.Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("复制账号", false, onCopyAccount)
                FilterPill("复制密码", false, onCopySecret)
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onDelete) { Text("删除", color = AppColors.Red) }
            }
        }
    }
}

@Composable
private fun PasswordRow(record: PasswordRecord, onClick: () -> Unit) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Password, contentDescription = null, tint = AppColors.Blue)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(record.name, color = AppColors.Text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(record.account, color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (record.target.isNotBlank()) SoftChip(record.target)
        }
    }
}

@Composable
private fun PieChart(values: List<CategoryTotal>) {
    if (values.isEmpty()) {
        EmptyState("暂无支出数据", "记一笔支出后会生成分类图", "🥧")
        return
    }
    val colors = listOf(AppColors.Blue, AppColors.Green, AppColors.Yellow, AppColors.Red, Color(0xFF7C3AED), Color(0xFF0891B2))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
            val total = values.sumOf { it.totalCents }.toFloat().coerceAtLeast(1f)
            var start = -90f
            values.forEachIndexed { index, item ->
                val sweep = item.totalCents / total * 360f
                drawArc(colors[index % colors.size], start, sweep, false, topLeft = Offset((size.width - 150.dp.toPx()) / 2, 10.dp.toPx()), size = Size(150.dp.toPx(), 150.dp.toPx()), style = Stroke(width = 30.dp.toPx()))
                start += sweep
            }
        }
        values.take(6).forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(colors[index % colors.size], CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("${item.category} ${item.totalCents.toYuanText()} 元", color = AppColors.Text)
            }
        }
    }
}

@Composable
private fun LineChart(values: List<MonthTotal>) {
    if (values.size < 2) {
        EmptyState("暂无年度数据", "至少两个月有流水后会生成趋势图", "📈")
        return
    }
    val maxValue = values.maxOfOrNull { maxOf(it.expenseCents, it.incomeCents) }?.coerceAtLeast(1) ?: 1
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        fun y(value: Long) = size.height - (value.toFloat() / maxValue * size.height)
        values.zipWithNext().forEachIndexed { index, pair ->
            drawLine(AppColors.Red, Offset(index * step, y(pair.first.expenseCents)), Offset((index + 1) * step, y(pair.second.expenseCents)), strokeWidth = 5f, cap = StrokeCap.Round)
            drawLine(AppColors.Green, Offset(index * step, y(pair.first.incomeCents)), Offset((index + 1) * step, y(pair.second.incomeCents)), strokeWidth = 5f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun Heatmap(values: Map<LocalDate, Int>, start: LocalDate, end: LocalDate) {
    val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val rows = 7
        val columns = ((dates.size + rows - 1) / rows).coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val cell = minOf((size.width - gap * (columns - 1)) / columns, (size.height - gap * (rows - 1)) / rows).coerceAtLeast(1f)
        dates.forEachIndexed { index, date ->
            val count = values[date] ?: 0
            val color = when {
                count >= 4 -> Color(0xFF166534)
                count >= 2 -> Color(0xFF22C55E)
                count == 1 -> Color(0xFFBBF7D0)
                else -> AppColors.Border
            }
            drawRect(color, topLeft = Offset((index / rows) * (cell + gap), (index % rows) * (cell + gap)), size = Size(cell, cell))
        }
    }
}

@Composable
private fun Meter(progress: Float, danger: Boolean) {
    Box(Modifier.fillMaxWidth().height(8.dp).background(AppColors.Border, CircleShape)) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .background(if (danger) AppColors.Red else AppColors.Blue, CircleShape)
        )
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

@Suppress("DEPRECATION")
private fun requestPasswordVaultAuth(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val manager = BiometricManager.from(activity)
    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        when (manager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                onError("请先在系统设置中录入指纹或设置锁屏密码")
                return
            }
            else -> {
                onError("当前设备不可用本机验证")
                return
            }
        }
    } else {
        val keyguard = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        val hasBiometric = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (!hasBiometric && !keyguard.isDeviceSecure) {
            onError("请先在系统设置中录入指纹或设置锁屏密码")
            return
        }
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onError(errString.toString().ifBlank { "验证已取消" })
            override fun onAuthenticationFailed() = onError("验证失败，请重试")
        }
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("解锁密码管理")
        .setSubtitle("使用本机密码或指纹验证")
        .apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                setAllowedAuthenticators(authenticators)
            } else {
                setDeviceCredentialAllowed(true)
            }
        }
        .build()
    prompt.authenticate(info)
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun clearClipboardIfValueMatches(context: Context, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val current = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
    if (current == value) clipboard.setPrimaryClip(ClipData.newPlainText("life-trio", ""))
}

private suspend fun copyImageToPrivateStorage(context: Context, source: Uri): String = withContext(Dispatchers.IO) {
    val file = createMemoImageFile(context)
    context.contentResolver.openInputStream(source)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    } ?: error("Cannot open image")
    file.absolutePath
}

private fun createCameraIntent(context: Context, output: Uri): Intent =
    Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
        putExtra(MediaStore.EXTRA_OUTPUT, output)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        context.packageManager.queryIntentActivities(this, 0).forEach { resolved ->
            context.grantUriPermission(
                resolved.activityInfo.packageName,
                output,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

private fun createMemoImageFile(context: Context): File {
    val dir = File(context.filesDir, "memo_images").apply { mkdirs() }
    return File(dir, "${System.currentTimeMillis()}_${System.nanoTime()}.jpg")
}
