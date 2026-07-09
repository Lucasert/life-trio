package com.lifetrio.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.lifetrio.navigateToTab
import com.lifetrio.core.data.AppContainer
import com.lifetrio.core.data.ThemeMode
import com.lifetrio.core.data.db.entity.LedgerType
import com.lifetrio.core.data.db.entity.toYuanText
import com.lifetrio.ui.components.AppCard
import com.lifetrio.ui.components.AppPage
import com.lifetrio.ui.components.EmptyState
import com.lifetrio.ui.components.FieldLabel
import com.lifetrio.ui.components.Meter
import com.lifetrio.ui.components.PrimaryButton
import com.lifetrio.ui.components.ScreenHeader
import com.lifetrio.ui.components.SectionTitle
import com.lifetrio.ui.theme.LocalExtendedColors
import com.lifetrio.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.YearMonth

@Composable
fun HomeScreen(container: AppContainer, navController: NavHostController, ledgerRoute: String) {
    val month = remember { YearMonth.now() }
    val budget by container.ledgerRepository.observeBudgetState(month).collectAsState(initial = null)
    val todayPlans by container.planRepository.observeToday().collectAsState(initial = emptyList())
    val memos by container.memoRepository.observeAll().collectAsState(initial = emptyList())
    val entries by container.ledgerRepository.observeThisMonthEntries().collectAsState(initial = emptyList())

    // ── Import / Export state ──
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImportConfirmDialog by remember { mutableStateOf(false) }
    var pendingImportJson by remember { mutableStateOf<String?>(null) }

    // Export launcher: user picks where to save the file
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = container.exportImportManager.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                toast(context, "导出成功")
            } catch (e: Exception) {
                toast(context, "导出失败: ${e.message}")
            }
        }
    }

    // Import launcher: user picks a file to restore
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { `in` ->
                    `in`.readBytes().toString(Charsets.UTF_8)
                } ?: throw IllegalStateException("无法读取文件")
                pendingImportJson = json
                showImportConfirmDialog = true
            } catch (e: Exception) {
                toast(context, "读取文件失败: ${e.message}")
            }
        }
    }

    // ── Import confirmation dialog ──
    if (showImportConfirmDialog && pendingImportJson != null) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                pendingImportJson = null
            },
            title = { Text("导入数据") },
            text = { Text("导入将覆盖所有现有数据（备忘、记账、计划、密码箱），此操作不可撤销。确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    val json = pendingImportJson ?: return@TextButton
                    showImportConfirmDialog = false
                    pendingImportJson = null
                    scope.launch {
                        try {
                            container.exportImportManager.importFromJson(json)
                            toast(context, "导入成功")
                        } catch (e: Exception) {
                            toast(context, "导入失败: ${e.message}")
                        }
                    }
                }) {
                    Text("确定导入")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportConfirmDialog = false
                    pendingImportJson = null
                }) {
                    Text("取消")
                }
            }
        )
    }

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

        // ── Appearance section ──
        item { Spacer(Modifier.size(Spacing.sm)) }
        item { SectionTitle("外观") }
        item {
            val currentTheme by container.themePreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "选择主题模式",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    SingleChoiceSegmentedButtonRow {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = currentTheme == mode,
                                onClick = {
                                    scope.launch {
                                        container.themePreferences.setThemeMode(mode)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                                icon = { },  // 移除默认的打勾图标
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text(
                                    when (mode) {
                                        ThemeMode.SYSTEM -> "系统"
                                        ThemeMode.LIGHT -> "亮色"
                                        ThemeMode.DARK -> "暗色"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Data Management section ──
        item { Spacer(Modifier.size(Spacing.sm)) }
        item { SectionTitle("数据管理") }
        item {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        "导出全部数据为 JSON 文件，可在换机后导入恢复。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    PrimaryButton(
                        text = "导出数据",
                        onClick = { exportLauncher.launch("life-trio-backup.json") }
                    )
                    PrimaryButton(
                        text = "导入数据",
                        onClick = { importLauncher.launch(arrayOf("application/json")) }
                    )
                }
            }
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

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
