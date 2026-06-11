package com.lifetrio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lifetrio.core.data.AppContainer
import com.lifetrio.core.data.db.entity.LedgerEntryEntity
import com.lifetrio.core.data.db.entity.LedgerType
import com.lifetrio.core.data.db.entity.toAmountCents
import com.lifetrio.core.data.db.entity.toYuanText
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.FilterPill
import com.lifetrio.ui.components.LineChart
import com.lifetrio.ui.components.Meter
import com.lifetrio.ui.components.PieChart
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SectionTitle
import com.lifetrio.ui.components.UnderlineField
import com.lifetrio.ui.theme.AppColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

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
