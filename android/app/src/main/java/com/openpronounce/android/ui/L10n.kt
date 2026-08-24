package com.openpronounce.android.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Current UI language, "en" or "vi", provided from MainActivity. */
val LocalLang = staticCompositionLocalOf { "en" }

/** Inline bilingual string: t("Hello", "Xin chào") picks the active language. */
@Composable
fun t(en: String, vi: String): String =
    if (LocalLang.current == "vi") vi else en
