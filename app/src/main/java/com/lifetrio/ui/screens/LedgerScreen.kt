package com.lifetrio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.lifetrio.core.data.AppContainer
import com.lifetrio.core.data.db.entity.LedgerEntryEntity
import com.lifetrio.core.data.db.entity.LedgerType
import com.lifetrio.core.data.db.entity.toAmountCents
import com.lifetrio.core.data.db.entity.toYuanText
import com.lifetrio.core.data.repository.BudgetState
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.EditorSheet
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.FilterPill
import com.lifetrio.ui.components.LineChart
import com.lifetrio.ui.components.Meter
import com.lifetrio.ui.components.PieChart
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SectionTabs
import com.lifetrio.ui.components.SectionTitle
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.theme.LocalExtendedColors
import com.lifetrio.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun LedgerScreen(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val month = remember { YearMonth.now() }
    val entries by container.ledgerRepository.observeThisMonthEntries().collectAsState(initial = emptyList())
    val budget by container.ledgerRepository.observeBudgetState(month).collectAsState(initial = null)
    val categories by container.ledgerRepository.observeCategoryTotals(month).collectAsState(initial = emptyList())
    val yearTotals by container.ledgerRepository.observeYearTotals(LocalDate.now().year).collectAsState(initial = emptyList())
    var sectionIndex by rememberSaveable { mutableStateOf(0) }
    var showEntrySheet by remember { mutableStateOf(false) }
    var showBudgetSheet by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (sectionIndex == 0) {
                FloatingActionButton(onClick = { showEntrySheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "记一笔")
                }
            }
        }
    ) { padding ->
        AppPage(modifier = Modifier.padding(padding)) {
            item { ScreenHeader("记账", "3 秒记录一笔收支") }
            item { SectionTabs(listOf("明细", "统计"), sectionIndex, { sectionIndex = it }) }
            if (sectionIndex == 0) {
                item { BudgetSummaryCard(budget) { showBudgetSheet = true } }
                item { SectionTitle("本月流水") }
                if (entries.isEmpty()) {
                    item { EmptyState("暂无流水", "点击右下角按钮记一笔收支", Icons.Outlined.AccountBalanceWallet) }
                }
                items(entries, key = { "ledger-${it.id}" }) { entry ->
                    LedgerEntryRow(entry) { scope.launch { container.ledgerRepository.deleteEntry(entry.id) } }
                }
            } else {
                item { SectionTitle("月度支出分类") }
                item { AppCard { PieChart(categories) } }
                item { SectionTitle("年度收支") }
                item { AppCard { LineChart(yearTotals) } }
            }
        }
    }

    if (showEntrySheet) {
        LedgerEntrySheet(
            onDismiss = { showEntrySheet = false },
            onSave = { type, category, amount, note ->
                scope.launch {
                    container.ledgerRepository.addEntry(type, category, amount.toAmountCents(), note, LocalDate.now())
                    showEntrySheet = false
                }
            }
        )
    }
    if (showBudgetSheet) {
        BudgetSheet(
            current = budget,
            onDismiss = { showBudgetSheet = false },
            onSave = { budgetText, warningRatio ->
                scope.launch {
                    container.ledgerRepository.setBudget(month, budgetText.toAmountCents(), warningRatio)
                    showBudgetSheet = false
                }
            }
        )
    }
}

@Composable
private fun BudgetSummaryCard(current: BudgetState?, onClick: () -> Unit) {
    AppCard(danger = current?.isWarning == true, hero = true, onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            FieldLabel(Icons.Outlined.Savings, "本月预算")
            Text(
                current?.let { "剩余 ${it.remainingCents.toYuanText()} 元，已用 ${it.spentCents.toYuanText()} 元" } ?: "点击设置本月预算",
                color = MaterialTheme.colorScheme.onSurface
            )
            Meter(current?.let { it.spentCents.toFloat() / it.budgetCents.coerceAtLeast(1) }?.coerceIn(0f, 1f) ?: 0f, current?.isWarning == true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LedgerEntrySheet(
    onDismiss: () -> Unit,
    onSave: (LedgerType, String, String, String) -> Unit
) {
    var type by remember { mutableStateOf(LedgerType.Expense) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("餐饮") }
    var note by remember { mutableStateOf("") }
    EditorSheet(title = "记一笔", onDismiss = onDismiss) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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
        PrimaryButton("记一笔", { onSave(type, category, amount, note) }, enabled = amount.toDoubleOrNull() != null)
    }
}

@Composable
private fun BudgetSheet(
    current: BudgetState?,
    onDismiss: () -> Unit,
    onSave: (String, Float) -> Unit
) {
    var budgetText by remember { mutableStateOf("") }
    var warningRatio by remember { mutableFloatStateOf(current?.warningRatio ?: 0.8f) }
    EditorSheet(title = "设置预算", onDismiss = onDismiss) {
        Text(
            current?.let { "当前预算 ${it.budgetCents.toYuanText()} 元，已用 ${it.spentCents.toYuanText()} 元" } ?: "尚未设置本月预算",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        UnderlineField(budgetText, { budgetText = it }, "本月预算金额", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
        Text("预警阈值 ${(warningRatio * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(warningRatio, onValueChange = { warningRatio = it }, valueRange = 0.5f..1f)
        PrimaryButton("保存预算", { onSave(budgetText, warningRatio) }, enabled = budgetText.toDoubleOrNull() != null)
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        options.forEach { option ->
            FilterPill(option, selected = selected == option, onClick = { onSelected(option) })
        }
    }
}

@Composable
private fun LedgerEntryRow(entry: LedgerEntryEntity, onDelete: () -> Unit) {
    val ext = LocalExtendedColors.current
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
                Text(entry.category, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                if (entry.note.isNotBlank()) Text(entry.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                (if (entry.type == LedgerType.Expense) "-" else "+") + entry.amountCents.toYuanText(),
                color = if (entry.type == LedgerType.Expense) ext.expense else ext.income,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
        }
    }
}
