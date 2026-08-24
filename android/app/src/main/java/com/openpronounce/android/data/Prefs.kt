package com.openpronounce.android.data

import android.content.Context

/** App appearance preferences, persisted in SharedPreferences. */
object Prefs {

    private const val FILE = "openpronounce_settings"

    const val COLOR_SYSTEM = "system"
    const val COLOR_GREEN = "green"
    const val COLOR_BLUE = "blue"
    const val COLOR_PURPLE = "purple"
    private const val KEY_THEME = "theme_mode"        // system | light | dark
    private const val KEY_COLOR = "color_source"      // system | green | blue | purple

    fun themeMode(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_THEME, "system")!!

    fun setThemeMode(ctx: Context, value: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_THEME, value).apply()
    }

    fun colorSource(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_COLOR, "system")!!

    fun setColorSource(ctx: Context, value: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_COLOR, value).apply()
    }

    private const val KEY_LANG = "language"   // en | vi

    fun language(ctx: Context): String =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY_LANG, "en")!!

    fun setLanguage(ctx: Context, value: String) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_LANG, value).apply()
    }
}
