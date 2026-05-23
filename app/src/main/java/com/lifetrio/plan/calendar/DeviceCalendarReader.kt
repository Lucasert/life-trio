package com.lifetrio.plan.calendar

import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DeviceCalendarDayOverride(
    val date: LocalDate,
    val isWorkday: Boolean,
    val title: String
)

class DeviceCalendarReader(
    private val context: Context,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun readWorkdayOverrides(start: LocalDate, end: LocalDate): List<DeviceCalendarDayOverride> {
        if (end.isBefore(start)) return emptyList()
        val beginMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(beginMillis.toString())
            .appendPath(endMillis.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
        )
        val results = linkedMapOf<LocalDate, DeviceCalendarDayOverride>()
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIndex).orEmpty()
                val isWorkday = classifyWorkday(title) ?: continue
                val eventBeginMillis = cursor.getLong(beginIndex)
                val eventEndMillis = cursor.getLong(endIndex).coerceAtLeast(eventBeginMillis + 1) - 1
                val eventStart = Instant.ofEpochMilli(eventBeginMillis).atZone(zoneId).toLocalDate()
                val eventEnd = Instant.ofEpochMilli(eventEndMillis).atZone(zoneId).toLocalDate()
                generateSequence(maxOf(eventStart, start)) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(minOf(eventEnd, end)) }
                    .forEach { date ->
                        results[date] = DeviceCalendarDayOverride(date, isWorkday, title)
                    }
            }
        }
        return results.values.toList()
    }

    private fun classifyWorkday(title: String): Boolean? {
        val normalized = title.trim()
        if (normalized.isBlank()) return null
        val workdayWords = listOf("班", "上班", "工作日", "调休上班")
        val holidayWords = listOf("休", "休息", "假", "放假", "节假日")
        return when {
            workdayWords.any { normalized.contains(it) } -> true
            holidayWords.any { normalized.contains(it) } -> false
            else -> null
        }
    }
}
