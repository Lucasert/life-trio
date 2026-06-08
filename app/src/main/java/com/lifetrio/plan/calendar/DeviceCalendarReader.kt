package com.lifetrio.plan.calendar

import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class DeviceCalendarDayOverride(
    val date: LocalDate,
    val isWorkday: Boolean,
    val title: String
)

data class DeviceCalendarReadResult(
    val overrides: List<DeviceCalendarDayOverride>,
    val scannedEventCount: Int,
    val calendarCount: Int
)

class DeviceCalendarReader(
    private val context: Context,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun readWorkdayOverrides(start: LocalDate, end: LocalDate): List<DeviceCalendarDayOverride> {
        return readWorkdayOverrideResult(start, end).overrides
    }

    fun readWorkdayOverrideResult(start: LocalDate, end: LocalDate): DeviceCalendarReadResult {
        if (end.isBefore(start)) return DeviceCalendarReadResult(emptyList(), 0, 0)

        val beginMillis = start.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val calendarCount = readableCalendarCount()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(beginMillis.toString())
            .appendPath(endMillis.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END
        )
        val results = linkedMapOf<LocalDate, DeviceCalendarDayOverride>()
        var scannedEventCount = 0

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descriptionIndex = cursor.getColumnIndex(CalendarContract.Instances.DESCRIPTION)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)

            while (cursor.moveToNext()) {
                scannedEventCount += 1
                val title = cursor.getString(titleIndex).orEmpty()
                val description = if (descriptionIndex >= 0) cursor.getString(descriptionIndex).orEmpty() else ""
                val isWorkday = classifyWorkday("$title $description") ?: continue
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

        if (scannedEventCount == 0) {
            scannedEventCount = readEventsFallback(beginMillis, endMillis, start, end, results)
        }

        return DeviceCalendarReadResult(results.values.toList(), scannedEventCount, calendarCount)
    }

    private fun readableCalendarCount(): Int {
        val projection = arrayOf(CalendarContract.Calendars._ID)
        return context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { cursor ->
            cursor.count
        } ?: 0
    }

    private fun readEventsFallback(
        beginMillis: Long,
        endMillis: Long,
        start: LocalDate,
        end: LocalDate,
        results: MutableMap<LocalDate, DeviceCalendarDayOverride>
    ): Int {
        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
        )
        val selection = """
            ${CalendarContract.Events.DELETED} = 0
            AND ${CalendarContract.Events.DTSTART} < ?
            AND (${CalendarContract.Events.DTEND} IS NULL OR ${CalendarContract.Events.DTEND} > ?)
        """.trimIndent()
        val args = arrayOf(endMillis.toString(), beginMillis.toString())
        var scannedEventCount = 0

        context.contentResolver.query(CalendarContract.Events.CONTENT_URI, projection, selection, args, null)?.use { cursor ->
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val descriptionIndex = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIndex = cursor.getColumnIndex(CalendarContract.Events.DTEND)

            while (cursor.moveToNext()) {
                scannedEventCount += 1
                val title = cursor.getString(titleIndex).orEmpty()
                val description = if (descriptionIndex >= 0) cursor.getString(descriptionIndex).orEmpty() else ""
                val isWorkday = classifyWorkday("$title $description") ?: continue
                val eventBeginMillis = cursor.getLong(beginIndex)
                val rawEndMillis = if (endIndex >= 0 && !cursor.isNull(endIndex)) cursor.getLong(endIndex) else eventBeginMillis + 1
                val eventEndMillis = rawEndMillis.coerceAtLeast(eventBeginMillis + 1) - 1
                val eventStart = Instant.ofEpochMilli(eventBeginMillis).atZone(zoneId).toLocalDate()
                val eventEnd = Instant.ofEpochMilli(eventEndMillis).atZone(zoneId).toLocalDate()

                generateSequence(maxOf(eventStart, start)) { it.plusDays(1) }
                    .takeWhile { !it.isAfter(minOf(eventEnd, end)) }
                    .forEach { date ->
                        results[date] = DeviceCalendarDayOverride(date, isWorkday, title)
                    }
            }
        }

        return scannedEventCount
    }

    private fun classifyWorkday(text: String): Boolean? {
        val normalized = text.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), "")
        if (normalized.isBlank()) return null

        val workdayWords = listOf(
            "补班",
            "调休上班",
            "调班",
            "上班",
            "工作日",
            "班",
            "make-upworkday",
            "makeupworkday",
            "workday",
            "adjustedworkday"
        )
        val holidayWords = listOf(
            "休息",
            "休",
            "假",
            "放假",
            "节假日",
            "法定节假日",
            "元旦",
            "春节",
            "除夕",
            "清明",
            "清明节",
            "劳动节",
            "端午",
            "端午节",
            "中秋",
            "中秋节",
            "国庆",
            "国庆节",
            "holiday",
            "publicholiday",
            "festival"
        )

        return when {
            workdayWords.any { normalized.contains(it) } -> true
            holidayWords.any { normalized.contains(it) } -> false
            else -> null
        }
    }
}
