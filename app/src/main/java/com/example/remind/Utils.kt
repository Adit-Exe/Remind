package com.example.remind

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object Utils {
    fun formatDateTime(epochMilli: Long): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm")
        return dt.format(formatter)
    }

    fun getRemainingTimeText(endMilli: Long, currentMilli: Long): String {
        if (currentMilli >= endMilli) return "Completed"
        
        val diff = endMilli - currentMilli
        val days = TimeUnit.MILLISECONDS.toDays(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff) % 60
        
        return buildString {
            if (days > 0) append("${days}d ")
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            append("${seconds}s")
        }.trim()
    }

    fun calculateProgress(startMilli: Long, endMilli: Long, currentMilli: Long): Float {
        if (currentMilli >= endMilli) return 1.0f
        if (currentMilli <= startMilli) return 0.0f
        
        val total = (endMilli - startMilli).toFloat()
        val elapsed = (currentMilli - startMilli).toFloat()
        return (elapsed / total).coerceIn(0f, 1f)
    }
}
