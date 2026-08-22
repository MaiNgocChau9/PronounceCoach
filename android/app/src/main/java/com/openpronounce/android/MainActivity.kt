package com.openpronounce.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openpronounce.android.ui.*

class MainActivity : ComponentActivity() {

    private var hasMicPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasMicPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MainViewModel = viewModel()
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                    when (val screen = currentScreen) {
                        is Screen.Home -> {
                            HomeScreen(
                                categories = viewModel.getCategories(),
                                onCategoryClick = { id ->
                                    viewModel.setCategory(id)
                                    currentScreen = Screen.Practice
                                },
                                onQuickPractice = {
                                    viewModel.loadNextWord()
                                    currentScreen = Screen.Practice
                                }
                            )
                        }
                        is Screen.Practice -> {
                            val word by viewModel.currentWord.collectAsState()
                            val isRecording by viewModel.isRecording.collectAsState()
                            val audioLevel by viewModel.audioLevel.collectAsState()
                            val result by viewModel.result.collectAsState()
                            val modelState by viewModel.modelState.collectAsState()
                            val customText by viewModel.customText.collectAsState()
                            val isCustomMode by viewModel.isCustomMode.collectAsState()

                            PracticeScreen(
                                word = word,
                                isRecording = isRecording,
                                audioLevel = audioLevel,
                                result = result,
                                modelState = modelState,
                                customText = customText,
                                isCustomMode = isCustomMode,
                                onCustomTextChange = { viewModel.setCustomText(it) },
                                onStartRecording = {
                                    if (hasMicPermission) viewModel.startRecording()
                                },
                                onStopRecording = { viewModel.stopRecording() },
                                onListen = { },
                                onNextWord = { viewModel.loadNextWord() }
                            )
                        }
                        is Screen.Category -> {
                            CategoryScreen(
                                onCategoryClick = { id ->
                                    viewModel.setCategory(id)
                                    currentScreen = Screen.Practice
                                },
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                        is Screen.History -> {
                            val history by viewModel.history.collectAsState()
                            HistoryScreen(
                                history = history,
                                onBack = { currentScreen = Screen.Home }
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class Screen {
    object Home : Screen()
    object Practice : Screen()
    object Category : Screen()
    object History : Screen()
}
