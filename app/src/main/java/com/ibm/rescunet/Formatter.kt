package com.ibm.rescunet

import kotlin.math.absoluteValue

object Formatter {
    fun formatCoord(coord: Int, positiveIndicator: String, negativeIndicator: String): String {
        val d = (coord.toDouble() / 100000.0).absoluteValue
        return String.format("%.5f %s", d, if (coord > 0) positiveIndicator else negativeIndicator)
    }


    fun formatTimeSinceSeen(time: Long): String {
        val secs = time / 1000

        return when {
            secs < 30 -> "Less than 30 seconds ago"
            secs < 60 -> "${secs/10}0 seconds ago"
            secs < 90 -> "1 minute ago"
            secs < 120 -> "1.5 minutes ago"
            secs < 600 -> "${secs / 60} minutes ago"
            else -> "More than 10 minutes ago"
        }
    }
}