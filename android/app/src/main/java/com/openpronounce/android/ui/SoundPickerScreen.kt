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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.openpronounce.android.data.PhonemeCatalog
import com.openpronounce.android.data.PhonemeEntry

/** Grid of practiceable IPA sounds; tapping one shows its articulation guide first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerScreen(
    onBack: () -> Unit,
    onStart: (PhonemeEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var selected by remember { mutableStateOf<PhonemeEntry?>(null) }

    // Resolved in composable context (the grid's content lambda is not composable).
    val vowelsLabel = t("Vowels", "Nguyên âm")
    val diphthongsLabel = t("Diphthongs", "Nguyên âm kép")
    val consonantsLabel = t("Consonants", "Phụ âm")

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(t("Pick a sound", "Chọn một âm")) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = t("Back", "Quay lại"))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            section(vowelsLabel, PhonemeCatalog.vowels) { selected = it }
            section(diphthongsLabel, PhonemeCatalog.diphthongs) { selected = it }
            section(consonantsLabel, PhonemeCatalog.consonants) { selected = it }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    selected?.let { entry ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = {
                Column {
                    Text("/${entry.symbol}/", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        soundTypeLabel(entry.type),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column {
                    Text(
                        t("How to make this sound", "Cách phát âm âm này"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(if (LocalLang.current == "vi") entry.tipVi else entry.tip,
                         style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        t("You'll get a continuous stream of words containing /${entry.symbol}/.",
                          "Bạn sẽ được luyện liên tục các từ chứa /${entry.symbol}/."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selected = null
                    onStart(entry)
                }) { Text(t("Start practicing", "Bắt đầu luyện")) }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }) { Text(t("Cancel", "Hủy")) }
            }
        )
    }
}

private fun androidx.compose.foundation.lazy.grid.LazyGridScope.section(
    title: String,
    entries: List<PhonemeEntry>,
    onClick: (PhonemeEntry) -> Unit
) {
    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(4) }) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 14.dp, bottom = 2.dp)
        )
    }
    items(entries, key = { it.symbol }) { entry ->
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .aspectRatioCell()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    CircleShape
                )
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .clickable { onClick(entry) }
        ) {
            Text(
                entry.symbol,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun Modifier.aspectRatioCell(): Modifier = this.then(
    Modifier
        .fillMaxWidth()
        .height(56.dp)
)

@Composable
private fun soundTypeLabel(type: String): String = when (type) {
    "Vowel" -> t("Vowel", "Nguyên âm")
    "Diphthong" -> t("Diphthong", "Nguyên âm kép")
    "Consonant" -> t("Consonant", "Phụ âm")
    else -> t("Cluster", "Cụm phụ âm")
}
