package com.example.ui.components

import java.util.Locale

object TimeFormatter {
    fun formatMs(ms: Long, includeMillis: Boolean = true): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = ((ms % 1000) / 10).coerceAtLeast(0)

        return if (includeMillis) {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, hundredths)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}
