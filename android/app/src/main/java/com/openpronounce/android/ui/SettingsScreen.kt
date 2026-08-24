package com.openpronounce.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.openpronounce.android.ui.theme.PaletteBlue
import com.openpronounce.android.ui.theme.PaletteGreen
import com.openpronounce.android.ui.theme.PalettePurple
import com.openpronounce.android.ui.theme.wallpaperPrimaryColor
import com.openpronounce.android.data.Prefs

private data class Option(val value: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: String,
    colorSource: String,
    language: String,
    onLanguageChange: (String) -> Unit,
    onThemeModeChange: (String) -> Unit,
    onColorSourceChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(t("Settings", "Cài đặt")) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back", "Quay lại"))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            SectionHeader(t("APPEARANCE", "GIAO DIỆN"))
            SettingCard {
                Text(t("Theme", "Chủ đề"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val sysLabel = t("System", "Hệ thống")
                val lightLabel = t("Light", "Sáng")
                val darkLabel = t("Dark", "Tối")
                val themeOptions = remember(sysLabel, lightLabel, darkLabel) {
                    listOf(
                        Option("system", sysLabel),
                        Option("light", lightLabel),
                        Option("dark", darkLabel)
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeOptions.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = themeMode == option.value,
                            onClick = { onThemeModeChange(option.value) },
                            shape = SegmentedButtonDefaults.itemShape(index, 3),
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingCard {
                Text(t("Color theme", "Màu sắc"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (colorSource == Prefs.COLOR_SYSTEM)
                        t("Colors follow your wallpaper", "Màu theo hình nền của bạn")
                    else
                        t("Fixed palette", "Bảng màu cố định"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Swatch(PaletteGreen, t("Green", "Xanh lá"), colorSource == Prefs.COLOR_GREEN) {
                        onColorSourceChange(Prefs.COLOR_GREEN)
                    }
                    Swatch(PaletteBlue, t("Blue", "Xanh dương"), colorSource == Prefs.COLOR_BLUE) {
                        onColorSourceChange(Prefs.COLOR_BLUE)
                    }
                    Swatch(PalettePurple, t("Violet", "Tím"), colorSource == Prefs.COLOR_PURPLE) {
                        onColorSourceChange(Prefs.COLOR_PURPLE)
                    }
                    val wallpaperPrimary = wallpaperPrimaryColor()
                    DynamicSwatch(color = wallpaperPrimary, selected = colorSource == Prefs.COLOR_SYSTEM) {
                        onColorSourceChange(Prefs.COLOR_SYSTEM)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingCard {
                Text(t("Language", "Ngôn ngữ"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                val langOptions = remember {
                    listOf(
                        Option("en", "English"),
                        Option("vi", "Tiếng Việt")
                    )
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    langOptions.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = language == option.value,
                            onClick = { onLanguageChange(option.value) },
                            shape = SegmentedButtonDefaults.itemShape(index, 2),
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingCard {
                Text(t("About", "Giới thiệu"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    buildAnnotatedString {
                        val vi = LocalLang.current == "vi"
                        if (vi) {
                            append("PronounceCoach · Dựa trên ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Halleck45/OpenPronounce") }
                            append(". Từ luyện tập được tạo ngay trên máy từ kho 10.000 từ có sẵn.")
                        } else {
                            append("PronounceCoach · Based on ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Halleck45/OpenPronounce") }
                            append(". Practice words are generated on-device from a built-in 10k-word lexicon.")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Tonal rounded container grouping related settings — M3 Expressive containment. */
@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 14.dp, bottom = 8.dp)
    )
}

private val swatchBorder = 2.5.dp

@Composable
private fun Swatch(color: Color, label: String, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(color, CircleShape)
                .border(
                    width = if (selected) swatchBorder else 0.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

/** The "Material You" option: swatch shows the wallpaper-derived primary color. */
@Composable
private fun DynamicSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .background(color, CircleShape)
                .border(
                    width = if (selected) swatchBorder else 0.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape
                )
                .clickable(onClick = onClick)
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(t("You", "Tự động"), style = MaterialTheme.typography.labelSmall)
    }
}
