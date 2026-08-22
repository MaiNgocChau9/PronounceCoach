package com.openpronounce.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openpronounce.android.data.WordItem
import com.openpronounce.android.scoring.PronunciationResult

@Composable
fun PracticeScreen(
    word: WordItem?,
    isRecording: Boolean,
    audioLevel: Float,
    result: PronunciationResult?,
    modelState: ModelState,
    customText: String,
    isCustomMode: Boolean,
    onCustomTextChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onListen: () -> Unit,
    onNextWord: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var textInput by remember { mutableStateOf("") }

    // Sync with external customText changes (e.g. when navigating)
    LaunchedEffect(customText) {
        if (textInput != customText) {
            textInput = customText
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Custom text input
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    onCustomTextChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Type your sentence (optional)", color = Color(0xFF8B8FA3)) },
                placeholder = { Text("e.g. Hello, how are you today?", color = Color(0xFF555)) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFF444),
                    cursorColor = Color(0xFF4CAF50)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = false,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Show pre-defined word when not in custom mode
            if (!isCustomMode || textInput.isBlank()) {
                word?.let { w ->
                    if (w.word.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A4A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(w.word, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                if (w.ipa.isNotEmpty()) {
                                    Text("/${w.ipa}/", fontSize = 14.sp, color = Color(0xFF4CAF50))
                                }
                                if (w.meaning.isNotEmpty()) {
                                    Text(w.meaning, fontSize = 12.sp, color = Color(0xFF8B8FA3))
                                }
                            }
                        }
                    }
                }
            }

            // ===== Record Button + Visualizer (centered together) =====
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Visualizer rings behind the button
                AudioVisualizer(
                    audioLevel = audioLevel,
                    isRecording = isRecording,
                    modifier = Modifier.size(200.dp)
                )
                // Record button on top
                Button(
                    onClick = {
                        if (isRecording) onStopRecording() else onStartRecording()
                    },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                text = if (isRecording) "Recording... tap to stop" else "Tap to record",
                color = Color(0xFF8B8FA3),
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ===== DEBUG STATUS =====
            DebugStatusBar(
                modelState = modelState,
                isRecording = isRecording,
                audioLevel = audioLevel,
                hasResult = result != null,
                wordIpa = word?.ipa ?: "",
                isCustomMode = isCustomMode
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Next button
            OutlinedButton(
                onClick = onNextWord,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
            ) {
                Icon(Icons.Filled.SkipNext, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Next")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Result
            AnimatedVisibility(
                visible = result != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                result?.let { r ->
                    ResultCard(result = r)
                }
            }
        }
    }
}

@Composable
fun DebugStatusBar(
    modelState: ModelState,
    isRecording: Boolean,
    audioLevel: Float,
    hasResult: Boolean,
    wordIpa: String,
    isCustomMode: Boolean
) {
    val stateText = when (modelState) {
        is ModelState.Loading -> "MODEL: loading..."
        is ModelState.Ready -> "MODEL: ready"
        is ModelState.Error -> "MODEL: ERROR - ${modelState.message}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111122)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Text(
                text = "DEBUG",
                color = Color(0xFF4CAF50),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = buildString {
                    appendLine("$stateText | mic=${if (isRecording) "ON" else "OFF"}")
                    appendLine("level=%.3f | result=%s".format(audioLevel, if (hasResult) "YES" else "NO"))
                    appendLine("mode=%s | ipa=%s".format(
                        if (isCustomMode) "CUSTOM" else "WORD",
                        wordIpa.ifEmpty { "(none)" }
                    ))
                },
                color = Color(0xFF666688),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun AudioVisualizer(
    audioLevel: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2
        val centerY = size.height / 2

        for (i in 3 downTo 0) {
            val radius = 30.dp.toPx() + (50.dp.toPx() * i / 3f)
            val alpha = if (isRecording) 0.15f * (4 - i) / 4f else 0.05f
            drawCircle(
                color = Color(0xFF4CAF50).copy(alpha = alpha),
                radius = radius * pulseScale,
                center = Offset(centerX, centerY)
            )
        }

        if (isRecording && audioLevel > 0.01f) {
            val levelRadius = 30.dp.toPx() + (50.dp.toPx() * audioLevel)
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = levelRadius * pulseScale,
                center = Offset(centerX, centerY),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = Offset(centerX, centerY)
        )
    }
}

@Composable
fun ResultCard(result: PronunciationResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A4A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Score", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "${result.overallPercent}%",
                    color = when {
                        result.overallPercent >= 80 -> Color(0xFF4CAF50)
                        result.overallPercent >= 50 -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (result.expectedIpa.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Expected", color = Color(0xFF8B8FA3), fontSize = 11.sp)
                        Text(result.expectedIpa, color = Color(0xFF4CAF50), fontSize = 13.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("You said", color = Color(0xFF8B8FA3), fontSize = 11.sp)
                        Text(
                            result.heardIpa.ifEmpty { "(no speech)" },
                            color = if (result.heardIpa.isNotEmpty()) Color(0xFF2196F3) else Color(0xFFF44336),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(result.feedback, color = Color(0xFF8B8FA3), fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
