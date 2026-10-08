package com.silifton.textwright.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** User preferences. Held as Compose state so the UI follows a change at once, and mirrored to disk. */
object AppSettings {

    enum class ThemeMode(val label: String) {
        System("System default"),
        Light("Light"),
        Dark("Dark"),
    }

    private const val PREFS = "settings"
    private const val KEY_THEME = "theme"
    private const val KEY_DYNAMIC = "dynamic_color"
    private const val KEY_PREVIEW = "notification_preview"
    private const val KEY_SECURE = "block_screenshots"

    var themeMode by mutableStateOf(ThemeMode.System)
        private set

    /** Take colours from the wallpaper (Android 12+) instead of the Textwright palette. */
    var dynamicColor by mutableStateOf(false)
        private set

    var notificationPreview by mutableStateOf(true)
        private set

    var blockScreenshots by mutableStateOf(false)
        private set

    fun load(context: Context) {
        val prefs = prefs(context)
        themeMode = ThemeMode.entries.getOrElse(prefs.getInt(KEY_THEME, 0)) { ThemeMode.System }
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, false)
        notificationPreview = prefs.getBoolean(KEY_PREVIEW, true)
        blockScreenshots = prefs.getBoolean(KEY_SECURE, false)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        themeMode = mode
        prefs(context).edit().putInt(KEY_THEME, mode.ordinal).apply()
    }

    fun setDynamicColor(context: Context, on: Boolean) {
        dynamicColor = on
        prefs(context).edit().putBoolean(KEY_DYNAMIC, on).apply()
    }

    fun setNotificationPreview(context: Context, on: Boolean) {
        notificationPreview = on
        prefs(context).edit().putBoolean(KEY_PREVIEW, on).apply()
    }

    fun setBlockScreenshots(context: Context, on: Boolean) {
        blockScreenshots = on
        prefs(context).edit().putBoolean(KEY_SECURE, on).apply()
    }

    /** Read straight from disk: notifications are posted from a receiver, where [load] may not have run. */
    fun notificationPreview(context: Context): Boolean = prefs(context).getBoolean(KEY_PREVIEW, true)

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
