package com.example.tripshot.util

import android.content.Context

/**
 * Persists which photo-prompt notification IDs the user has already handled
 * (tapped or otherwise acted on), keyed per user so multiple accounts on one
 * device don't interfere.
 */
object PhotoPromptPrefs {

    private const val PREFS_FILE = "photo_prompt_handled"

    fun handledPromptIds(context: Context, uid: String): Set<String> {
        return context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            .getStringSet("handled_$uid", emptySet()) ?: emptySet()
    }

    fun markHandled(context: Context, uid: String, notifId: String) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet("handled_$uid", emptySet()) ?: emptySet()
        prefs.edit().putStringSet("handled_$uid", existing + notifId).apply()
    }

    /**
     * Marks all notification IDs whose tripId matches [tripId] as handled.
     * Used when a system-notification tap deep-links directly to the capture screen
     * (where only tripId is available from the FCM extras, not the notification doc id).
     */
    fun markHandledByTripId(context: Context, uid: String, tripId: String, allPrompts: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val existing = prefs.getStringSet("handled_$uid", emptySet()) ?: emptySet()
        prefs.edit().putStringSet("handled_$uid", existing + allPrompts).apply()
    }
}
