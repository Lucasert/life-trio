package com.lifetrio

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as LifeTrioApp).container
        setContent {
            LifeTrioTheme {
                LifeTrioApp(container)
            }
        }
    }
}

private enum class Destination(val route: String, val label: String) {
    Home("home", "首页"),
    Memo("memo", "备忘"),
    Ledger("ledger", "记账"),
    Plan("plan", "计划")
}

@Composable
private fun LifeTrioApp(container: AppContainer) {
    val navController = rememberNavController()
    val today = LocalDate.now()

    LaunchedEffect(today) {
        container.planRepository.generateOccurrences(today.minusDays(1), today.plusDays(31))
    }

    Scaffold(
        bottomBar = { BottomBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(container, navController)
            }
            composable(Destination.Memo.route) {
                MemoScreen(container, navController)
            }
            composable(Destination.Ledger.route) {
                LedgerScreen(container)
            }
            composable(Destination.Plan.route) {
                PlanScreen(container)
            }
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route ?: Destination.Home.route
    NavigationBar {
        Destination.entries.forEach { destination ->
            NavigationBarItem(
                selected = route == destination.route,
                onClick = { navController.navigate(destination.route) { launchSingleTop = true } },
                icon = {
                    Icon(
                        imageVector = when (destination) {
                            Destination.Home -> Icons.Default.Home
                            Destination.Memo -> Icons.Default.NoteAdd
                            Destination.Ledger -> Icons.Default.AccountBalanceWallet
                            Destination.Plan -> Icons.Default.EventRepeat
                        },
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

@Composable
private fun HomeScreen(container: AppContainer, navController: NavHostController) {
    val month = remember { YearMonth.now() }
    val budget by container.ledgerRepository.observeBudgetState(month).collectAsState(initial = null)
    val todayPlans by container.planRepository.observeToday().collectAsState(initial = emptyList())
    val memos by container.memoRepository.observeAll().collectAsState(initial = emptyList())
    val entries by container.ledgerRepository.observeThisMonthEntries().collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("life-trio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            BudgetSummaryCard(
                budgetState = budget,
                onClick = { navController.navigate(Destination.Ledger.route) }
            )
        }
        item {
            SummaryRow(
                memos = memos.size,
                monthExpense = entries.filter { it.type == LedgerType.Expense }.sumOf { it.amountCents },
                plans = todayPlans.size
            )
        }
        item {
            SectionTitle("今日待办")
            if (todayPlans.isEmpty()) {
                EmptyText("今天没有待办")
            }
        }
        items(todayPlans.take(5), key = { "home-plan-${it.occurrenceId}" }) { item ->
            CompactPlanItem(item.title, item.note, item.status.name)
        }
    }
}

@Composable
private fun BudgetSummaryCard(
    budgetState: com.lifetrio.core.data.repository.BudgetState?,
    onClick: () -> Unit
) {
    val warning = budgetState?.isWarning == true
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (warning) Color(0xFFFFE4E6) else Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("本月预算", fontWeight = FontWeight.SemiBold)
            if (budgetState == null) {
                Text("尚未设置预算", color = Color.Gray)
            } else {
                Text("剩余 ${budgetState.remainingCents.toYuanText()} / ${budgetState.budgetCents.toYuanText()} 元")
                val progress = (budgetState.spentCents.toFloat() / budgetState.budgetCents.coerceAtLeast(1)).coerceIn(0f, 1f)
                LinearMeter(progress = progress, danger = warning)
            }
        }
    }
}

@Composable
private fun SummaryRow(memos: Int, monthExpense: Long, plans: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("备忘", memos.toString(), Modifier.weight(1f))
        StatCard("本月支出", monthExpense.toYuanText(), Modifier.weight(1f))
        StatCard("今日计划", plans.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            Text(value, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemoScreen(container: AppContainer, navController: NavHostController) {
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
    val memos by container.memoRepository.search(query).collectAsState(initial = emptyList())

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            imageUris = imageUris + copyImageToPrivateStorage(context, uri)
        }
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (text.isNotBlank()) body = "$body$text"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingId = null
                title = ""
                body = ""
                tags = ""
                pinned = false
                imageUris = emptyList()
            }) {
                Icon(Icons.Default.Add, contentDescription = "新增备忘")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("备忘录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") },
                    label = { Text("搜索标题、正文或标签") }
                )
            }
            item {
                EditorCard(
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
                        }
                    }
                )
            }
            items(memos, key = { "memo-${it.id}" }) { memo ->
                MemoCard(
                    memo = memo,
                    onEdit = {
                        editingId = memo.id
                        title = memo.title
                        body = memo.body
                        pinned = memo.isPinned
                        imageUris = memo.imageUris.split("|").filter { it.isNotBlank() }
                        scope.launch {
                            tags = container.memoRepository.tagsForMemo(memo.id).joinToString(",") { it.name }
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
                            navController.navigate(Destination.Plan.route)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun EditorCard(
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
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitle,
                    modifier = Modifier.weight(1f),
                    label = { Text("标题") }
                )
                IconButton(onClick = { onPinned(!pinned) }) {
                    Icon(Icons.Default.PushPin, contentDescription = "置顶", tint = if (pinned) AppColors.Blue else Color.Gray)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onBold) { Text("B", fontWeight = FontWeight.Bold) }
                IconButton(onClick = onList) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "列表") }
                IconButton(onClick = onTodo) { Icon(Icons.Default.CheckBox, contentDescription = "待办") }
                IconButton(onClick = onImage) { Icon(Icons.Default.Image, contentDescription = "图片") }
                IconButton(onClick = onVoice) { Icon(Icons.Default.Mic, contentDescription = "语音") }
            }
            OutlinedTextField(
                value = body,
                onValueChange = onBody,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                label = { Text("随手记") }
            )
            OutlinedTextField(
                value = tags,
                onValueChange = onTags,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("标签，用逗号分隔") }
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("图片 $imageCount 张", color = Color.Gray)
                Button(onClick = onSave, enabled = title.isNotBlank() || body.isNotBlank()) {
                    Text("保存")
                }
            }
        }
    }
}

@Composable
private fun MemoCard(memo: MemoEntity, onEdit: () -> Unit, onToPlan: () -> Unit) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (memo.isPinned) Icon(Icons.Default.Star, contentDescription = "置顶", tint = AppColors.Yellow, modifier = Modifier.size(18.dp))
                Text(memo.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Text(memo.body, maxLines = 4, overflow = TextOverflow.Ellipsis, color = Color(0xFF444444))
            if (memo.imageUris.isNotBlank()) {
                Text("含 ${memo.imageUris.split("|").count { it.isNotBlank() }} 张图片", color = Color.Gray, style = MaterialTheme.typography.labelMedium)
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text("编辑") }
                TextButton(onClick = onToPlan) { Text("转计划") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun LedgerScreen(container: AppContainer) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("记账", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SingleChoiceSegmentedButtonRow {
                        LedgerType.entries.forEachIndexed { index, item ->
                            SegmentedButton(
                                selected = type == item,
                                onClick = { type = item },
                                shape = SegmentedButtonDefaults.itemShape(index, LedgerType.entries.size)
                            ) {
                                Text(if (item == LedgerType.Expense) "支出" else "收入")
                            }
                        }
                    }
                    CategoryChips(selected = category, onSelected = { category = it }, type = type)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("金额") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("备注") }
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                container.ledgerRepository.addEntry(type, category, amount.toAmountCents(), note, LocalDate.now())
                                amount = ""
                                note = ""
                                type = LedgerType.Expense
                            }
                        },
                        enabled = amount.toDoubleOrNull() != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("记一笔")
                    }
                }
            }
        }
        item {
            BudgetEditor(
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
        item {
            SectionTitle("月度支出分类")
            PieChart(categories)
        }
        item {
            SectionTitle("年度收支")
            LineChart(yearTotals)
        }
        item {
            SectionTitle("本月流水")
        }
        items(entries, key = { "ledger-${it.id}" }) { entry ->
            LedgerEntryRow(entry)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChips(selected: String, onSelected: (String) -> Unit, type: LedgerType) {
    val options = if (type == LedgerType.Expense) {
        listOf("餐饮", "交通", "购物", "住房", "娱乐", "医疗", "学习")
    } else {
        listOf("工资", "奖金", "报销", "理财", "其他")
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { item ->
            FilterChip(
                selected = selected == item,
                onClick = { onSelected(item) },
                label = { Text(item) }
            )
        }
    }
}

@Composable
private fun BudgetEditor(
    current: com.lifetrio.core.data.repository.BudgetState?,
    budgetText: String,
    warningRatio: Float,
    onBudgetText: (String) -> Unit,
    onWarningRatio: (Float) -> Unit,
    onSave: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = if (current?.isWarning == true) Color(0xFFFFE4E6) else Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("预算预警", fontWeight = FontWeight.SemiBold)
            Text(current?.let { "剩余 ${it.remainingCents.toYuanText()} 元，已用 ${it.spentCents.toYuanText()} 元" } ?: "尚未设置本月预算")
            LinearMeter(
                progress = current?.let { it.spentCents.toFloat() / it.budgetCents.coerceAtLeast(1) }?.coerceIn(0f, 1f) ?: 0f,
                danger = current?.isWarning == true
            )
            OutlinedTextField(
                value = budgetText,
                onValueChange = onBudgetText,
                label = { Text("本月预算金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Text("预警阈值 ${(warningRatio * 100).toInt()}%")
            Slider(value = warningRatio, onValueChange = onWarningRatio, valueRange = 0.5f..1f)
            Button(onClick = onSave, enabled = budgetText.toDoubleOrNull() != null) {
                Text("保存预算")
            }
        }
    }
}

@Composable
private fun LedgerEntryRow(entry: LedgerEntryEntity) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(entry.category, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text((if (entry.type == LedgerType.Expense) "-" else "+") + entry.amountCents.toYuanText())
        }
        if (entry.note.isNotBlank()) Text(entry.note, Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp), color = Color.Gray)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanScreen(container: AppContainer) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("计划", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
                onWeekdayToggle = { day -> weekdays = toggle(weekdays, day) },
                onMonthDayToggle = { day -> monthDays = toggle(monthDays, day) },
                onCarry = { carry = it },
                onSave = {
                    scope.launch {
                        container.planRepository.addPlan(
                            title = title,
                            note = note,
                            ruleType = rule,
                            weekdays = weekdays,
                            monthDays = monthDays,
                            intervalDays = interval.toIntOrNull() ?: 1,
                            startDate = today,
                            carryStrategy = carry
                        )
                        title = ""
                        note = ""
                    }
                }
            )
        }
        item {
            SectionTitle("今日待办")
            if (todayItems.isEmpty()) EmptyText("今天还没有计划")
        }
        items(todayItems, key = { "plan-today-${it.occurrenceId}" }) { item ->
            Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = false,
                        onCheckedChange = {
                            scope.launch { container.planRepository.completeOccurrence(item.occurrenceId, today) }
                        }
                    )
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.SemiBold)
                        if (item.note.isNotBlank()) Text(item.note, color = Color.Gray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { scope.launch { container.planRepository.skipOccurrence(item.occurrenceId) } }) {
                        Text("跳过")
                    }
                }
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { container.planRepository.carryUnfinished(today, today.plusDays(1)) } }) {
                    Text("未完成顺延")
                }
                Button(onClick = { scope.launch { container.planRepository.generateOccurrences(today, today.plusDays(31)) } }) {
                    Text("刷新未来计划")
                }
            }
        }
        item {
            SectionTitle("打卡热力图")
            Heatmap(heatmap.associate { it.date to it.count }, today.minusDays(90), today)
        }
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
        item {
            SectionTitle("全部计划")
        }
        items(plans, key = { "plan-${it.id}" }) { plan ->
            PlanRow(plan)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkdayOverrideEditor(
    dateText: String,
    overrides: List<com.lifetrio.core.data.db.entity.WorkdayOverrideEntity>,
    onDate: (String) -> Unit,
    onSetWorkday: () -> Unit,
    onSetHoliday: () -> Unit,
    onClear: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("法定工作日维护", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = dateText,
                onValueChange = onDate,
                label = { Text("日期 yyyy-MM-dd") },
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSetWorkday) { Text("设为工作日") }
                Button(onClick = onSetHoliday) { Text("设为休息日") }
                TextButton(onClick = onClear) { Text("清除") }
            }
            if (overrides.isNotEmpty()) {
                Text(
                    overrides.take(3).joinToString("  ") { "${it.date}:${if (it.isWorkday) "班" else "休"}" },
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelMedium
                )
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
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = title, onValueChange = onTitle, label = { Text("计划名称") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = note, onValueChange = onNote, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanRuleType.entries.forEach { item ->
                    FilterChip(
                        selected = rule == item,
                        onClick = { onRule(item) },
                        label = { Text(item.label()) }
                    )
                }
            }
            when (rule) {
                PlanRuleType.Weekly -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { day ->
                        FilterChip(selected = day in weekdays, onClick = { onWeekdayToggle(day) }, label = { Text("周${weekdayName(day)}") })
                    }
                }
                PlanRuleType.Monthly -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..31).forEach { day ->
                        FilterChip(selected = day in monthDays, onClick = { onMonthDayToggle(day) }, label = { Text(day.toString()) })
                    }
                }
                PlanRuleType.EveryNDays -> OutlinedTextField(
                    value = interval,
                    onValueChange = onInterval,
                    label = { Text("每 N 天") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                else -> Unit
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("未完成处理", modifier = Modifier.weight(1f))
                FilterChip(
                    selected = carry == CarryStrategy.CarryNextDay,
                    onClick = { onCarry(CarryStrategy.CarryNextDay) },
                    label = { Text("顺延") }
                )
                Spacer(Modifier.width(6.dp))
                FilterChip(
                    selected = carry == CarryStrategy.Skip,
                    onClick = { onCarry(CarryStrategy.Skip) },
                    label = { Text("跳过") }
                )
            }
            Button(onClick = onSave, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("保存计划")
            }
        }
    }
}

@Composable
private fun PlanRow(plan: PlanEntity) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.EventRepeat, contentDescription = null, tint = AppColors.Blue)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(plan.title, fontWeight = FontWeight.SemiBold)
                Text(plan.ruleType.label(), color = Color.Gray)
            }
        }
    }
}

@Composable
private fun CompactPlanItem(title: String, note: String, status: String) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AppColors.Green)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (note.isNotBlank()) Text(note, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AssistChip(onClick = {}, label = { Text(status) })
        }
    }
}

@Composable
private fun PieChart(values: List<CategoryTotal>) {
    if (values.isEmpty()) {
        EmptyText("暂无支出数据")
        return
    }
    val colors = listOf(AppColors.Blue, AppColors.Green, AppColors.Yellow, AppColors.Red, Color(0xFF7C3AED), Color(0xFF0891B2))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val total = values.sumOf { it.totalCents }.toFloat().coerceAtLeast(1f)
            var start = -90f
            values.forEachIndexed { index, item ->
                val sweep = item.totalCents / total * 360f
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset((size.width - 150.dp.toPx()) / 2, 15.dp.toPx()),
                    size = Size(150.dp.toPx(), 150.dp.toPx()),
                    style = Stroke(width = 30.dp.toPx(), cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        values.take(6).forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(colors[index % colors.size], RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text("${item.category} ${item.totalCents.toYuanText()} 元")
            }
        }
    }
}

@Composable
private fun LineChart(values: List<MonthTotal>) {
    val maxValue = values.maxOfOrNull { maxOf(it.expenseCents, it.incomeCents) }?.coerceAtLeast(1) ?: 1
    Canvas(Modifier.fillMaxWidth().height(180.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp)) {
        if (values.isEmpty()) return@Canvas
        val step = size.width / 11f
        fun y(value: Long) = size.height - (value.toFloat() / maxValue * size.height)
        values.zipWithNext().forEachIndexed { index, pair ->
            drawLine(AppColors.Red, Offset(index * step, y(pair.first.expenseCents)), Offset((index + 1) * step, y(pair.second.expenseCents)), strokeWidth = 4f)
            drawLine(AppColors.Green, Offset(index * step, y(pair.first.incomeCents)), Offset((index + 1) * step, y(pair.second.incomeCents)), strokeWidth = 4f)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Heatmap(values: Map<LocalDate, Int>, start: LocalDate, end: LocalDate) {
    val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.toList()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        if (dates.isEmpty()) return@Canvas

        val rows = 7
        val columns = ((dates.size + rows - 1) / rows).coerceAtLeast(1)
        val gap = 3.dp.toPx()
        val cell = minOf(
            (size.width - gap * (columns - 1)) / columns,
            (size.height - gap * (rows - 1)) / rows
        ).coerceAtLeast(1f)

        dates.forEachIndexed { index, date ->
            val column = index / rows
            val row = index % rows
            val count = values[date] ?: 0
            val color = when {
                count >= 4 -> Color(0xFF166534)
                count >= 2 -> Color(0xFF22C55E)
                count == 1 -> Color(0xFFBBF7D0)
                else -> Color(0xFFE5E7EB)
            }
            drawRect(
                color = color,
                topLeft = Offset(column * (cell + gap), row * (cell + gap)),
                size = Size(cell, cell)
            )
        }
    }
}

@Composable
private fun LinearMeter(progress: Float, danger: Boolean) {
    Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFE5E7EB), RoundedCornerShape(8.dp))) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(8.dp)
                .background(if (danger) AppColors.Red else AppColors.Blue, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun EmptyText(text: String) {
    Text(text, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun LifeTrioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = AppColors.Blue,
            secondary = AppColors.Green,
            background = AppColors.Background,
            surface = Color.White,
            error = AppColors.Red
        ),
        content = { Surface(content = content) }
    )
}

private object AppColors {
    val Background = Color(0xFFFAF9F6)
    val Blue = Color(0xFF2563EB)
    val Green = Color(0xFF16A34A)
    val Yellow = Color(0xFFEAB308)
    val Red = Color(0xFFDC2626)
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

private suspend fun copyImageToPrivateStorage(context: Context, source: Uri): String = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "memo_images").apply { mkdirs() }
    val file = File(dir, "${System.currentTimeMillis()}.jpg")
    context.contentResolver.openInputStream(source)?.use { input ->
        file.outputStream().use { output -> input.copyTo(output) }
    }
    file.absolutePath
}
