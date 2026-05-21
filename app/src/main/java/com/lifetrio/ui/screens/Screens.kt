package com.lifetrio.ui.screens

import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.lifetrio.core.data.db.entity.WorkdayOverrideEntity
import com.lifetrio.core.data.db.entity.toAmountCents
import com.lifetrio.core.data.db.entity.toYuanText
import com.lifetrio.password.PasswordRecord
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.DashedUploadBox
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.FilterPill
import com.lifetrio.ui.components.MiniLine
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
        item { ScreenHeader("life-trio", "把记录、账目、计划和密码收进一个地方", "🔍") }
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
    var filter by remember { mutableStateOf("全部") }
    var editingId by remember { mutableStateOf<Long?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val memos by container.memoRepository.search(query).collectAsState(initial = emptyList())
    val visibleMemos = memos.filter { memoMatchesFilter(it, filter) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch { imageUris = imageUris + copyImageToPrivateStorage(context, uri) }
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (text.isNotBlank()) body = "$body$text"
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        AppPage(modifier = Modifier.padding(padding)) {
            item { ScreenHeader("随手记", "捕捉灵感・管理日常", "⋮") }
            item { PillSearchField(query, { query = it }, "搜索标题、正文或标签") }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("全部", "随手记", "记账", "计划").forEach { item ->
                        FilterPill(item, filter == item, { filter = item })
                    }
                }
            }
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
                    onImage = { imagePicker.launch("image/*") },
                    onVoice = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE.toLanguageTag())
                        }
                        try {
                            speechLauncher.launch(intent)
                        } catch (_: ActivityNotFoundException) {
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
            if (visibleMemos.isEmpty()) {
                item { EmptyState("暂无备忘", "先写下一条随手记", "📝") }
            }
            items(visibleMemos, key = { "memo-${it.id}" }) { memo ->
                MemoCard(
                    memo = memo,
                    onEdit = {
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
        item { ScreenHeader("记账", "3 秒记录一笔收支", "＋") }
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
    val today = LocalDate.now()
    val plans by container.planRepository.observePlans().collectAsState(initial = emptyList())
    val todayItems by container.planRepository.observeToday(today).collectAsState(initial = emptyList())
    val heatmap by container.planRepository.observeHeatmap(today.minusDays(90), today).collectAsState(initial = emptyList())
    val overrides by container.planRepository.observeWorkdayOverrides().collectAsState(initial = emptyList())
    var title by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var rule by remember { mutableStateOf(PlanRuleType.Daily) }
    var interval by remember { mutableStateOf("2") }
    var weekdays by remember { mutableStateOf(setOf(today.dayOfWeek.value)) }
    var monthDays by remember { mutableStateOf(setOf(today.dayOfMonth)) }
    var carry by remember { mutableStateOf(CarryStrategy.CarryNextDay) }
    var overrideDate by remember { mutableStateOf(today.toString()) }

    AppPage {
        item {
            ScreenHeader("计划", "周期任务和每日待办", "📅")
            if (!container.planRepository.hasWorkdayCalendarFor(today.year)) {
                Text("当前年份缺少中国法定工作日表，请维护节假日数据。", color = Color(0xFFB45309))
            }
        }
        item {
            PlanEditor(
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
                        container.planRepository.addPlan(title, note, rule, weekdays, monthDays, interval.toIntOrNull() ?: 1, today, carry)
                        title = ""
                        note = ""
                    }
                }
            )
        }
        item { SectionTitle("今日待办") }
        if (todayItems.isEmpty()) {
            item { EmptyState("今天还没有计划", "新增一个周期计划后自动生成待办", "✅") }
        }
        items(todayItems, key = { "plan-today-${it.occurrenceId}" }) { item ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(false, onCheckedChange = { scope.launch { container.planRepository.completeOccurrence(item.occurrenceId, today) } })
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = AppColors.Text, fontWeight = FontWeight.SemiBold)
                        if (item.note.isNotBlank()) Text(item.note, color = AppColors.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { scope.launch { container.planRepository.skipOccurrence(item.occurrenceId) } }) { Text("跳过") }
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("未完成顺延", selected = false, onClick = { scope.launch { container.planRepository.carryUnfinished(today, today.plusDays(1)) } })
                FilterPill("刷新未来计划", selected = false, onClick = { scope.launch { container.planRepository.generateOccurrences(today, today.plusDays(31)) } })
            }
        }
        item { SectionTitle("打卡热力图") }
        item { AppCard { Heatmap(heatmap.associate { it.date to it.count }, today.minusDays(90), today) } }
        item {
            WorkdayOverrideEditor(
                dateText = overrideDate,
                overrides = overrides,
                onDate = { overrideDate = it },
                onSetWorkday = {
                    parseLocalDateOrNull(overrideDate)?.let { date ->
                        scope.launch {
                            container.planRepository.setWorkdayOverride(date, true)
                            container.planRepository.generateOccurrences(today, today.plusDays(31))
                        }
                    }
                },
                onSetHoliday = {
                    parseLocalDateOrNull(overrideDate)?.let { date ->
                        scope.launch {
                            container.planRepository.setWorkdayOverride(date, false)
                            container.planRepository.generateOccurrences(today, today.plusDays(31))
                        }
                    }
                },
                onClear = {
                    parseLocalDateOrNull(overrideDate)?.let { date ->
                        scope.launch {
                            container.planRepository.clearWorkdayOverride(date)
                            container.planRepository.generateOccurrences(today, today.plusDays(31))
                        }
                    }
                }
            )
        }
        item { SectionTitle("全部计划") }
        if (plans.isEmpty()) {
            item { EmptyState("暂无计划", "创建每日、每周或法定工作日计划", "📅") }
        }
        items(plans, key = { "plan-${it.id}" }) { plan -> PlanRow(plan) }
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
    onImage: () -> Unit,
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
            DashedUploadBox("+ 添加图片", if (imageCount == 0) "📷" else "$imageCount 张", onImage)
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
            tags.forEach { tag -> SoftChip("$tag ×") }
            SoftChip("+ 添加标签")
        }
        UnderlineField(value, onValueChange, "生活，账单")
    }
}

@Composable
private fun MemoCard(memo: MemoEntity, onEdit: () -> Unit, onDelete: () -> Unit, onToPlan: () -> Unit) {
    AppCard {
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
    onSave: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FieldLabel("📌", "计划内容")
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
            PrimaryButton("保存计划", onSave, enabled = title.isNotBlank())
        }
    }
}

@Composable
private fun WorkdayOverrideEditor(
    dateText: String,
    overrides: List<WorkdayOverrideEntity>,
    onDate: (String) -> Unit,
    onSetWorkday: () -> Unit,
    onSetHoliday: () -> Unit,
    onClear: () -> Unit
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FieldLabel("🗓️", "法定工作日维护")
            UnderlineField(dateText, onDate, "日期 yyyy-MM-dd")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("设为工作日", false, onSetWorkday)
                FilterPill("设为休息日", false, onSetHoliday)
                TextButton(onClick = onClear) { Text("清除") }
            }
            if (overrides.isNotEmpty()) {
                Text(overrides.take(3).joinToString("  ") { "${it.date}:${if (it.isWorkday) "班" else "休"}" }, color = AppColors.Muted)
            }
        }
    }
}

@Composable
private fun PlanRow(plan: PlanEntity) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EventRepeat, contentDescription = null, tint = AppColors.Blue)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(plan.title, color = AppColors.Text, fontWeight = FontWeight.SemiBold)
                Text(plan.ruleType.label(), color = AppColors.Muted)
            }
        }
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
            item { ScreenHeader("密码", "本机验证后访问保险库", "🔒") }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("密码", style = MaterialTheme.typography.headlineMedium, color = AppColors.Text, fontWeight = FontWeight.Black)
                    Text("账号与密钥保险库", color = AppColors.Muted)
                }
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
        MiniLine()
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

private fun memoMatchesFilter(memo: MemoEntity, filter: String): Boolean = when (filter) {
    "全部", "随手记" -> true
    "记账" -> memo.title.contains("账") || memo.body.contains("账")
    "计划" -> memo.title.contains("计划") || memo.body.contains("计划")
    else -> true
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

private fun parseLocalDateOrNull(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()

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
    val dir = File(context.filesDir, "memo_images").apply { mkdirs() }
    val file = File(dir, "${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    file.absolutePath
}
