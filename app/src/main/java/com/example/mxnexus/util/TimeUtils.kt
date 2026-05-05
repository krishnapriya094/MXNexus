package com.example.mxnexus.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    @JvmStatic
    fun getRelativeTime(timestamp: Timestamp?): String {
        if (timestamp == null) return ""

        val now = System.currentTimeMillis()
        val time = timestamp.toDate().time
        val diff = now - time

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60          -> "Just now"
            minutes < 60          -> "${minutes}m ago"
            hours < 24            -> "${hours}h ago"
            days == 1L            -> "Yesterday"
            days < 7              -> "${days}d ago"
            else                  -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = time
                val postYear = cal.get(Calendar.YEAR)
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)

                if (postYear == currentYear) {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(timestamp.toDate())
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(timestamp.toDate())
                }
            }
        }
    }

    @JvmStatic
    fun getScheduledLabel(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val formatted = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            .format(timestamp.toDate())
        return "Scheduled for $formatted"
    }
}
