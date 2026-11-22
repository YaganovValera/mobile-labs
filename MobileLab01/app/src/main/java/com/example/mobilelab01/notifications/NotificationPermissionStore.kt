package com.example.mobilelab01.notifications

import android.content.Context

/**
 * Хранит признак, что runtime-запрос POST_NOTIFICATIONS уже показывался,
 * чтобы не запрашивать разрешение бесконечно.
 */
class NotificationPermissionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var hasRequestedPermission: Boolean
        get() = prefs.getBoolean(KEY_REQUESTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_REQUESTED, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "notification_permission_prefs"
        private const val KEY_REQUESTED = "post_notifications_requested"
    }
}
