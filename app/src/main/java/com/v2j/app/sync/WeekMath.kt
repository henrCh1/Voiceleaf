package com.v2j.app.sync

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.IsoFields

/** All week/day math is anchored to Asia/Shanghai and ISO weeks (Monday = day 1). */
object WeekMath {

    val ZONE: ZoneId = ZoneId.of("Asia/Shanghai")
    private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZONE).toLocalDate()

    private fun zoned(millis: Long): ZonedDateTime = Instant.ofEpochMilli(millis).atZone(ZONE)

    fun weekId(date: LocalDate): String {
        val weekYear = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "%04d-W%02d".format(weekYear, week)
    }

    fun weekStartDate(date: LocalDate): LocalDate = date.with(ChronoField.DAY_OF_WEEK, 1) // Monday
    fun weekEndDate(date: LocalDate): LocalDate = date.with(ChronoField.DAY_OF_WEEK, 7)   // Sunday

    fun weekStartMillis(date: LocalDate): Long =
        weekStartDate(date).atStartOfDay(ZONE).toInstant().toEpochMilli()

    fun weekEndExclusiveMillis(date: LocalDate): Long =
        weekStartDate(date).plusDays(7).atStartOfDay(ZONE).toInstant().toEpochMilli()

    fun hhmm(millis: Long): String = zoned(millis).format(HHMM)

    /** "06-08" */
    fun mmDashDd(date: LocalDate): String = "%02d-%02d".format(date.monthValue, date.dayOfMonth)

    /** "0608" */
    fun mmdd(date: LocalDate): String = "%02d%02d".format(date.monthValue, date.dayOfMonth)

    fun zhWeekday(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }

    /** e.g. "2026-W24 周复盘 (0608-0614).md" — stable for the whole week, so overwrites hit one file. */
    fun fileName(weekId: String, start: LocalDate, end: LocalDate): String =
        "$weekId 周复盘 (${mmdd(start)}-${mmdd(end)}).md"
}
