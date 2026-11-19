package com.coc.builderwidget.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {
    fun formatRemaining(remainingMillis: Long): String {
        if (remainingMillis <= 0L) return "SELESAI!"
        val totalMinutes = remainingMillis / 60_000L
        val days = totalMinutes / (60 * 24)
        val hours = (totalMinutes % (60 * 24)) / 60
        val minutes = totalMinutes % 60

        return when {
            days > 0 -> "${days}h ${hours}j"
            hours > 0 -> "${hours}j ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }

    fun formatFinishClock(finishTimeMillis: Long): String {
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return sdf.format(Date(finishTimeMillis))
    }
}
