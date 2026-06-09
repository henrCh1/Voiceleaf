package com.v2j.app.sync

import com.v2j.app.data.Entry
import java.time.LocalDate

/**
 * Pure function: a week's live entries -> the weekly Markdown file body.
 * Entries should already exclude tombstones; they are grouped by day and ordered by time.
 */
object WeekRenderer {

    fun render(weekId: String, start: LocalDate, end: LocalDate, entries: List<Entry>): String {
        val sb = StringBuilder()
        sb.append("---\n")
        sb.append("type: weekly-journal\n")
        sb.append("week: \"").append(weekId).append("\"\n")
        sb.append("start: ").append(start).append('\n')
        sb.append("end: ").append(end).append('\n')
        sb.append("managed_by: voice-weekly-app\n")
        sb.append("---\n\n")
        sb.append("<!-- 本文件由「口述周复盘」App 自动管理。当周请勿在 Obsidian 手改，否则下次同步会被覆盖。周一过后此文件封档，可随意编辑。 -->\n\n")
        sb.append("# ").append(weekId).append(" 周复盘（")
            .append(WeekMath.mmDashDd(start)).append(" ~ ").append(WeekMath.mmDashDd(end)).append("）\n")

        val live = entries.filter { it.deletedAt == null }.sortedBy { it.createdAt }
        if (live.isEmpty()) {
            sb.append("\n（本周暂无内容）\n")
            return sb.toString()
        }

        val byDay = live.groupBy { WeekMath.localDate(it.createdAt) }.toSortedMap()
        for ((day, dayEntries) in byDay) {
            sb.append("\n## ").append(WeekMath.mmDashDd(day)).append(' ')
                .append(WeekMath.zhWeekday(day.dayOfWeek)).append('\n')
            for (e in dayEntries) {
                val text = (e.polishedText ?: e.rawText).trim()
                // Time tag on its own line, content starts on the line(s) below it.
                sb.append("**").append(WeekMath.hhmm(e.createdAt)).append("**\n\n").append(text).append("\n\n")
            }
        }
        return sb.toString().trimEnd() + "\n"
    }
}
