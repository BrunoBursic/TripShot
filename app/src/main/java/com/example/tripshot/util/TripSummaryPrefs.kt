package com.example.tripshot.util

import android.content.Context

/**
 * Persists which trip completion summaries the user has already acknowledged,
 * keyed per user so multiple accounts on one device don't interfere.
 */
object TripSummaryPrefs {

    private const val PREFS_FILE = "trip_summary_ack"

    fun acknowledgedTripIds(context: Context, uid: String): Set<String> {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getStringSet("ack_$uid", emptySet()) ?: emptySet()
    }

    fun acknowledge(context: Context, uid: String, tripId: String) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet("ack_$uid", emptySet()) ?: emptySet()
        prefs.edit().putStringSet("ack_$uid", existing + tripId).apply()
    }
}
