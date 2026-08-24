package com.pronouncecoach.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Current UI language, "en" or "vi", provided from MainActivity. */
val LocalLang = compositionLocalOf { "en" }

/** Inline bilingual string: t("Hello", "Xin chào") picks the active language. */
@Composable
fun t(en: String, vi: String): String =
    if (LocalLang.current == "vi") vi else en
