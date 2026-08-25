package com.pronouncecoach.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronouncecoach.android.data.PhonemeEntry
import com.pronouncecoach.android.data.WordItem
import com.pronouncecoach.android.scoring.PronunciationResult
import com.pronouncecoach.android.scoring.WordError

/**
 * Minimal practice flow, mirroring the web UI: the sentence, a record button,
 * an animated score ring and per-phone chips highlighting what went wrong.
 */
@Composable
fun PracticeScreen(
    word: WordItem?,
    ipaTokens: List<String>,
    isRecording: Boolean,
    isAnalyzing: Boolean,
    result: PronunciationResult?,
    modelState: ModelState,
    customText: String,
    isCustomMode: Boolean,
    showCustomInput: Boolean,
    drillPhone: PhonemeEntry? = null,
    onCustomTextChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onListen: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onNextWord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(modifier = modifier.fillMaxSize()) {
        // ---- Scrollable content: slides UNDER the frosted bottom bar ----------
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // ---- Drill banner: which sound + how to make it ----------------------
            if (drillPhone != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ScoreMid.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScoreMid.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "/${drillPhone.symbol}/",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ScoreMid
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (LocalLang.current == "vi") drillPhone.tipVi else drillPhone.tip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // ---- Target sentence -------------------------------------------------
            if (word != null) {
                val w = word
                val wordErrs = remember(result) {
                    result?.wordErrors?.filter { it.word.isNotEmpty() } ?: emptyList()
                }
                // Single-word target: its own entry in the result.
                val myResult = remember(wordErrs) {
                    if (wordErrs.size <= 1) {
                        wordErrs.firstOrNull()
                    } else null
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (wordErrs.size > 1) {
                        // Sentence with a result: show ONLY the colored sentence
                        // (no duplicate plain text above it).
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            wordErrs.forEachIndexed { i, we ->
                                ColoredWord(we, big = true) { onSpeakWord(we.word) }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            t("Tap a word to hear it", "Chạm vào từ để nghe lại"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // Plain target (or single colored word once scored).
                        Text(
                            w.word,
                            style = MaterialTheme.typography.headlineMedium,
                            color = myResult?.let { scoreColor(it.accuracyPercent) }
                                ?: MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                if (w.ipa.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        coloredIpa(
                            ipa = w.ipa,
                            tokens = ipaTokens,
                            phoneCorrect = myResult?.phoneCorrect ?: emptyList()
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                        if (!isCustomMode && w.meaning.isNotEmpty()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                w.meaning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (showCustomInput) {
                Text(
                    t("Type any word or sentence, then read it aloud",
                      "Nhập từ hoặc câu bất kỳ, rồi đọc to"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // ---- Custom sentence input (free practice only, never in category practice) ----
            if (showCustomInput) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customText,
                    onValueChange = {
                        onCustomTextChange(it.trimStart('\n'))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(t("Type your own word or sentence…", "Nhập từ hoặc câu của bạn…")) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
            }

            // ---- Result ----------------------------------------------------------
            AnimatedVisibility(visible = result != null) {
                result?.let { r ->
                    if (r.noSpeechDetected) {
                        NoSpeechCard()
                    } else {
                        ResultPanel(result = r, onSpeakWord = onSpeakWord)
                    }
                }
            }

            // breathing room so the last card can scroll out from under the bar
            Spacer(Modifier.height(110.dp))
        }

        // ---- Frosted bottom overlay: gradient fade + control bar -----------------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(surfaceColor.copy(alpha = 0f), surfaceColor)
                        )
                    )
            )
            // Same color as the gradient above it, so bar and fade read as one sheet.
            Surface(color = surfaceColor) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)
                    ) {
                        FilledTonalIconButton(
                            onClick = onListen,
                            enabled = modelState is ModelState.Ready && !isAnalyzing
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = t("Listen", "Nghe"))
                        }
                        if (!isCustomMode) {
                            FilledTonalIconButton(
                                onClick = onNextWord,
                                enabled = !isAnalyzing && !isRecording
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = t("Next", "Từ tiếp"))
                            }
                        }
                        RecordButton(
                            isRecording = isRecording,
                            enabled = modelState is ModelState.Ready && !isAnalyzing,
                            onClick = { if (isRecording) onStopRecording() else onStartRecording() }
                        )
                    }
                    Text(
                        text = when {
                            modelState is ModelState.Loading -> t("Loading the model…", "Đang tải mô hình…")
                            modelState is ModelState.Error -> modelState.message
                            isAnalyzing -> t("Analyzing…", "Đang phân tích…")
                            isRecording -> t("Recording — tap to stop", "Đang ghi âm — chạm để dừng")
                            else -> t("Tap the mic and read the sentence aloud", "Chạm mic và đọc to câu")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }
    }
}

/** Compact mic button — same footprint as the tonal buttons flanking it. */
@Composable
private fun RecordButton(
    isRecording: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = if (isRecording) {
        val pulse = rememberInfiniteTransition(label = "pulse")
        val pulseScale by pulse.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "scale"
        )
        pulseScale
    } else {
        1f
    }

    // Follow the Material scheme like its neighbors: primary when idle, error when recording.
    val container = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    val iconTint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary
    val view = androidx.compose.ui.platform.LocalView.current

    FilledIconButton(
        onClick = {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
            onClick()
        },
        enabled = enabled,
        modifier = modifier
            .size(48.dp)
            .scale(scale),
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = container)
    ) {
        Icon(
            if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = if (isRecording) t("Stop", "Dừng") else t("Record", "Ghi âm"),
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// ELSA-style result panel
// ---------------------------------------------------------------------------

@Composable
private fun ResultPanel(
    result: PronunciationResult,
    onSpeakWord: (String) -> Unit
) {
    val percent = result.overallPercent

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        ScoreBanner(percent)

        // Sound-by-sound breakdown for every word that isn't perfect.
        val imperfectWords = remember(result) {
            result.wordErrors
                .filter { it.accuracyPercent < 100 && it.phoneCorrect.isNotEmpty() }
                .sortedBy { it.accuracyPercent }
        }
        if (imperfectWords.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                imperfectWords.forEach { WordDetailCard(it, onSpeakWord) }
            }
        }
    }
}

/** Banner card: emoji + verdict + "You sound X% like a native speaker!" + % circle. */
@Composable
private fun ScoreBanner(percent: Int) {
    val color = scoreColor(percent)
    val (emoji, title) = when {
        percent >= 90 -> "\ud83c\udf89" to t("Excellent!", "Xuất sắc!")
        percent >= 75 -> "\ud83d\ude04" to t("Great job!", "Làm tốt lắm!")
        percent >= 50 -> "\ud83e\uddd0" to t("Almost correct", "Sắp đúng rồi")
        else -> "\ud83d\udcaa" to t("Keep practicing", "Tiếp tục nào")
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, fontSize = 26.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = color)
                Text(
                    t("You sound $percent% like a native speaker!",
                      "Bạn phát âm chuẩn $percent% như người bản xứ!"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                // Ring shows the actual percent as an arc (ELSA-style), not always full.
                Canvas(modifier = Modifier.size(48.dp)) {
                    val stroke = 5.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(inset, inset)
                    drawArc(
                        color = color.copy(alpha = 0.15f),
                        startAngle = -90f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = color,
                        startAngle = -90f, sweepAngle = 360f * percent / 100f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }
                Text(
                    "$percent",
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
            }
        }
    }
}

/** One inline word of the sentence, colored by accuracy, underline like ELSA. */
@Composable
private fun ColoredWord(
    word: WordError,
    big: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        word.word,
        style = if (big) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        color = scoreColor(word.accuracyPercent),
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/**
 * Per-word breakdown card, ELSA-style: EVERY phone of the word gets a row with its
 * status — green check when said right, red heard-phone (plus articulation tip) when
 * wrong, "not heard" when missing.
 */
@Composable
private fun WordDetailCard(
    word: WordError,
    onSpeakWord: (String) -> Unit
) {
    // Pre-split IPA once per card recomposition instead of inside the loop.
    val phones = remember(word.expectedIpa) {
        word.expectedIpa.split(" ").filter { it.isNotEmpty() }
    }
    val correct = word.phoneCorrect
    val heard = word.phoneHeard

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    word.word,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${word.accuracyPercent}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = scoreColor(word.accuracyPercent)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { onSpeakWord(word.word) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Hear \u201c${word.word}\u201d",
                        tint = ScoreGood,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (phones.isEmpty()) {
                Text(
                    t("An extra sound slipped in while reading.", "Có âm thừa khi đọc."),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            phones.forEachIndexed { i, phone ->
                val ok = correct.getOrNull(i) ?: true
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "/$phone/",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        if (ok) {
                            Text(
                                "\u2713 " + t("said well", "chuẩn"),
                                style = MaterialTheme.typography.labelLarge,
                                color = ScoreGood
                            )
                        } else {
                            val h = heard.getOrNull(i).orEmpty()
                            if (h.isEmpty()) {
                                Text(
                                    t("not heard", "không nghe được"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ScoreBad
                                )
                            } else {
                                Text(
                                    t("you said /$h/", "bạn đọc /$h/"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = ScoreBad
                                )
                            }
                        }
                    }
                    if (!ok) {
                        PhoneTips.tipFor(phone, LocalLang.current)?.let { tip ->
                            Text(
                                tip,
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Friendly retry prompt instead of a bogus score when nothing was heard. */
@Composable
private fun NoSpeechCard() {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(t("We couldn't hear you", "Chúng tôi không nghe rõ"), style = MaterialTheme.typography.titleMedium)
            Text(
                t("Speak louder and closer to the mic, then try again.",
                  "Hãy nói to hơn và gần mic hơn, rồi thử lại."),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Vivid, semantic feedback colors (ELSA-style) — deliberately NOT the dynamic
// Material palette: feedback must be unmistakable in both light and dark themes.
private val ScoreGood = Color(0xFF1FA05A)
private val ScoreMid = Color(0xFFF39C12)
private val ScoreBad = Color(0xFFE5484D)

private fun scoreColor(percent: Int): Color = when {
    percent >= 80 -> ScoreGood
    percent >= 60 -> ScoreMid
    else -> ScoreBad
}

/**
 * IPA string with each phone token colored by correctness (green = said right,
 * red = wrong/missing), like ELSA's per-sound feedback.
 * [tokens] is the pre-split list from the ViewModel (avoids splitting on every recomposition).
 */
@Composable
private fun coloredIpa(
    ipa: String,
    tokens: List<String>,
    phoneCorrect: List<Boolean>
) = buildAnnotatedString {
    val t = if (tokens.isNotEmpty()) tokens else ipa.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (phoneCorrect.isEmpty() || t.size != phoneCorrect.size) {
        withStyle(
            SpanStyle(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        ) { append("/$ipa/") }
        return@buildAnnotatedString
    }
    append("/")
    t.forEachIndexed { i, token ->
        if (i > 0) append(" ")
        withStyle(
            SpanStyle(
                color = if (phoneCorrect[i]) ScoreGood else ScoreBad,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        ) { append(token) }
    }
    append("/")
}
