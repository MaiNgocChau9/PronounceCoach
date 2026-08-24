package com.openpronounce.android

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.CompositionLocalProvider
import com.openpronounce.android.ui.LocalLang
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.openpronounce.android.data.Prefs
import com.openpronounce.android.ui.CategoryScreen
import com.openpronounce.android.ui.CreateFab
import com.openpronounce.android.ui.HomeScreen
import com.openpronounce.android.ui.MainViewModel
import com.openpronounce.android.ui.PracticeScreen
import com.openpronounce.android.ui.SettingsScreen
import com.openpronounce.android.ui.SoundPickerScreen
import com.openpronounce.android.ui.theme.OpenPronounceTheme

class MainActivity : ComponentActivity() {

    private var hasMicPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hasMicPermission = checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

        // Appearance prefs are read once here; Settings updates them live.
        val initialThemeMode = Prefs.themeMode(this)
        val initialColorSource = Prefs.colorSource(this)
        val initialLanguage = Prefs.language(this)

        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }
            var colorSource by remember { mutableStateOf(initialColorSource) }
            var language by remember { mutableStateOf(initialLanguage) }

            CompositionLocalProvider(LocalLang provides language) {

            OpenPronounceTheme(
                darkOverride = when (themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> null
                },
                colorSource = colorSource
            ) {
                val viewModel: MainViewModel = viewModel()
                var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

                if (!hasMicPermission && currentScreen is Screen.Practice) {
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    floatingActionButton = {
                        if (currentScreen is Screen.Home) {
                            CreateFab(
                                onCustomText = {
                                    viewModel.stopDrill()
                                    viewModel.startFreePractice()
                                    currentScreen = Screen.Practice(showCustomInput = true)
                                },
                                onRandomWord = {
                                    viewModel.stopDrill()
                                    viewModel.loadRandomWord()
                                    currentScreen = Screen.Practice(showCustomInput = false)
                                },
                                onPickSound = { currentScreen = Screen.Sounds }
                            )
                        }
                    },
                    bottomBar = {
                        if (currentScreen !is Screen.Category) {
                            NavigationBarItemRow(currentScreen) { currentScreen = it }
                        }
                    }
                ) { padding ->
                    when (val screen = currentScreen) {
                        is Screen.Home -> HomeScreen(
                            categories = viewModel.getCategories(),
                            onCategoryClick = { id ->
                                viewModel.stopDrill()
                                viewModel.setCategory(id)
                                currentScreen = Screen.Practice(showCustomInput = false)
                            },
                            modifier = Modifier.padding(padding)
                        )
                        is Screen.Category -> CategoryScreen(
                            onCategoryClick = { id ->
                                viewModel.stopDrill()
                                viewModel.setCategory(id)
                                currentScreen = Screen.Practice(showCustomInput = false)
                            },
                            onBack = { currentScreen = Screen.Home },
                            modifier = Modifier.padding(padding)
                        )
                        is Screen.Sounds -> SoundPickerScreen(
                            onBack = { currentScreen = Screen.Home },
                            onStart = { entry ->
                                viewModel.startDrill(entry)
                                currentScreen = Screen.Practice(showCustomInput = false)
                            },
                            modifier = Modifier.padding(padding)
                        )
                        is Screen.Settings -> SettingsScreen(
                            themeMode = themeMode,
                            colorSource = colorSource,
                            language = language,
                            onThemeModeChange = {
                                themeMode = it
                                Prefs.setThemeMode(this, it)
                            },
                            onColorSourceChange = {
                                colorSource = it
                                Prefs.setColorSource(this, it)
                            },
                            onLanguageChange = {
                                language = it
                                Prefs.setLanguage(this, it)
                            },
                            onBack = { currentScreen = Screen.Home },
                            modifier = Modifier.padding(padding)
                        )
                        is Screen.Practice -> PracticeScreen(
                            word = viewModel.currentWord.collectAsState().value,
                            isRecording = viewModel.isRecording.collectAsState().value,
                            isAnalyzing = viewModel.isAnalyzing.collectAsState().value,
                            result = viewModel.result.collectAsState().value,
                            modelState = viewModel.modelState.collectAsState().value,
                            customText = viewModel.customText.collectAsState().value,
                            isCustomMode = viewModel.isCustomMode.collectAsState().value,
                            showCustomInput = screen.showCustomInput,
                            drillPhone = viewModel.drillPhone.collectAsState().value,
                            onCustomTextChange = { viewModel.setCustomText(it) },
                            onStartRecording = { viewModel.startRecording() },
                            onStopRecording = { viewModel.stopRecording() },
                            onListen = { viewModel.listen() },
                            onSpeakWord = { viewModel.speakWord(it) },
                            onNextWord = {
                                if (viewModel.drillPhone.value != null) viewModel.nextDrillWord()
                                else viewModel.loadNextWord()
                            },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
            }
        }
    }
}

private enum class Tab(val label: String) { HOME("Home"), PRACTICE("Practice"), SETTINGS("Settings") }

@Composable
private fun NavigationBarItemRow(current: Screen, onSelect: (Screen) -> Unit) {
    val activeTab = when (current) {
        is Screen.Home, is Screen.Category, is Screen.Sounds -> Tab.HOME
        is Screen.Practice -> Tab.PRACTICE
        is Screen.Settings -> Tab.SETTINGS
    }
    NavigationBar {
        for (tab in Tab.entries) {
            val selected = tab == activeTab
            NavigationBarItem(
                selected = selected,
                onClick = {
                    onSelect(
                        when (tab) {
                            Tab.HOME -> Screen.Home
                            Tab.PRACTICE -> Screen.Practice()
                            Tab.SETTINGS -> Screen.Settings
                        }
                    )
                },
                icon = {
                    Icon(
                        imageVector = when (tab) {
                            Tab.HOME -> Icons.Filled.Home
                            Tab.PRACTICE -> Icons.Filled.Mic
                            Tab.SETTINGS -> Icons.Filled.Settings
                        },
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label) }
            )
        }
    }
}

sealed class Screen {
    object Home : Screen()
    data class Practice(val showCustomInput: Boolean = false) : Screen()
    object Category : Screen()
    object Sounds : Screen()
    object Settings : Screen()
}
