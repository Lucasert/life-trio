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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lifetrio.core.data.db.dao.CategoryTotal
import com.lifetrio.core.data.db.dao.MonthTotal
import com.lifetrio.core.data.db.entity.toYuanText
import com.lifetrio.ui.theme.AppColors

@Composable
fun PieChart(values: List<CategoryTotal>) {
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
fun LineChart(values: List<MonthTotal>) {
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
