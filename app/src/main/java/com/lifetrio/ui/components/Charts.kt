package com.lifetrio.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lifetrio.core.data.db.dao.CategoryTotal
import com.lifetrio.core.data.db.dao.MonthTotal
import com.lifetrio.core.data.db.entity.toYuanText
import com.lifetrio.ui.theme.LocalExtendedColors

@Composable
fun PieChart(values: List<CategoryTotal>) {
    if (values.isEmpty()) {
        EmptyState("暂无支出数据", "记一笔支出后会生成分类图", Icons.Outlined.PieChartOutline)
        return
    }
    val palette = LocalExtendedColors.current.chartPalette
    // Aggregate anything beyond the first 5 categories into an "其他" slice.
    val aggregated = if (values.size > 6) {
        val top = values.take(5)
        val restTotal = values.drop(5).sumOf { it.totalCents }
        top + CategoryTotal("其他", restTotal)
    } else {
        values
    }
    val total = aggregated.sumOf { it.totalCents }.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(180.dp)) {
            Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                val diameter = 150.dp.toPx()
                val stroke = 30.dp.toPx()
                val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
                var start = -90f
                aggregated.forEachIndexed { index, item ->
                    val sweep = item.totalCents.toFloat() / total * 360f
                    drawArc(
                        color = palette[index % palette.size],
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("总支出", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text("${total.toYuanText()} 元", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
        }
        aggregated.forEachIndexed { index, item ->
            val percent = (item.totalCents.toFloat() / total * 100).toInt()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.size(10.dp).background(palette[index % palette.size], CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(item.category, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                Text("${item.totalCents.toYuanText()} 元 · $percent%", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun LineChart(values: List<MonthTotal>) {
    if (values.size < 2) {
        EmptyState("暂无年度数据", "至少两个月有流水后会生成趋势图", Icons.Outlined.ShowChart)
        return
    }
    val ext = LocalExtendedColors.current
    val expenseColor = ext.expense
    val incomeColor = ext.income
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val maxValue = values.maxOf { maxOf(it.expenseCents, it.incomeCents) }.coerceAtLeast(1)
    val labelStyle = TextStyle(fontSize = 10.sp, color = labelColor)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
            val leftPad = 36.dp.toPx()
            val bottomPad = 18.dp.toPx()
            val topPad = 6.dp.toPx()
            val chartWidth = size.width - leftPad
            val chartHeight = size.height - bottomPad - topPad
            val step = chartWidth / (values.size - 1).coerceAtLeast(1)
            fun x(index: Int) = leftPad + index * step
            fun y(value: Long) = topPad + chartHeight - (value.toFloat() / maxValue * chartHeight)

            // horizontal gridlines + left amount labels (0, mid, max)
            listOf(0L, maxValue / 2, maxValue).forEach { value ->
                val gy = y(value)
                drawLine(gridColor, Offset(leftPad, gy), Offset(size.width, gy), strokeWidth = 1f)
                val label = textMeasurer.measure("${value.toYuanText()}", labelStyle)
                drawText(label, topLeft = Offset(0f, gy - label.size.height / 2))
            }

            // expense + income polylines
            values.zipWithNext().forEachIndexed { index, pair ->
                drawLine(expenseColor, Offset(x(index), y(pair.first.expenseCents)), Offset(x(index + 1), y(pair.second.expenseCents)), strokeWidth = 5f, cap = StrokeCap.Round)
                drawLine(incomeColor, Offset(x(index), y(pair.first.incomeCents)), Offset(x(index + 1), y(pair.second.incomeCents)), strokeWidth = 5f, cap = StrokeCap.Round)
            }
            // data point dots
            values.forEachIndexed { index, item ->
                drawCircle(expenseColor, radius = 4f, center = Offset(x(index), y(item.expenseCents)))
                drawCircle(incomeColor, radius = 4f, center = Offset(x(index), y(item.incomeCents)))
            }
            // first / middle / last month labels
            val labelIndices = setOf(0, values.size / 2, values.size - 1)
            labelIndices.forEach { index ->
                val monthText = values[index].month.substringAfter('-').trimStart('0').ifBlank { "1" } + "月"
                val measured = textMeasurer.measure(monthText, labelStyle)
                drawText(measured, topLeft = Offset((x(index) - measured.size.width / 2).coerceIn(0f, size.width - measured.size.width), size.height - measured.size.height))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(expenseColor, "支出")
            LegendDot(incomeColor, "收入")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}
