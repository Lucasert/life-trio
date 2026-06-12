package com.lifetrio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lifetrio.navigateToTab
import com.lifetrio.core.data.AppContainer
import com.lifetrio.core.data.db.entity.LedgerType
import com.lifetrio.core.data.db.entity.toYuanText
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.Meter
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SectionTitle
import com.lifetrio.ui.theme.LocalExtendedColors
import com.lifetrio.ui.theme.Spacing
import java.time.YearMonth

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
            AppCard(danger = budget?.isWarning == true, hero = true, onClick = { navController.navigateToTab(ledgerRoute) }) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FieldLabel(Icons.Filled.Savings, "本月预算")
                    Text(
                        budget?.let { "剩余 ${it.remainingCents.toYuanText()} / ${it.budgetCents.toYuanText()} 元" } ?: "尚未设置预算",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    val progress = budget?.let { it.spentCents.toFloat() / it.budgetCents.coerceAtLeast(1) }?.coerceIn(0f, 1f) ?: 0f
                    Meter(progress, budget?.isWarning == true)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                StatCard(Icons.Outlined.StickyNote2, "备忘", memos.size.toString(), Modifier.weight(1f))
                StatCard(Icons.Outlined.TrendingDown, "支出", entries.filter { it.type == LedgerType.Expense }.sumOf { it.amountCents }.toYuanText(), Modifier.weight(1f))
                StatCard(Icons.Outlined.TaskAlt, "待办", todayPlans.size.toString(), Modifier.weight(1f))
            }
        }
        item { SectionTitle("今日待办") }
        if (todayPlans.isEmpty()) {
            item { EmptyState("今天没有待办", "去计划页添加一个周期任务", Icons.Outlined.TaskAlt) }
        }
        items(todayPlans.take(5), key = { "home-plan-${it.occurrenceId}" }) { item ->
            CompactPlanItem(item.title, item.note, item.status.name)
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
            }
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CompactPlanItem(title: String, note: String, status: String) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Check, contentDescription = null, tint = LocalExtendedColors.current.income)
            Spacer(Modifier.width(Spacing.xs))
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                if (note.isNotBlank()) Text(note, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AssistChip(onClick = {}, label = { Text(status) })
        }
    }
}
