package com.pronouncecoach.android

import android.Manifest
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
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
import com.pronouncecoach.android.ui.LocalLang
import com.pronouncecoach.android.ui.t
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pronouncecoach.android.data.Prefs
import com.pronouncecoach.android.ui.CategoryScreen
import com.pronouncecoach.android.ui.HomeScreen
import com.pronouncecoach.android.ui.MainViewModel
import com.pronouncecoach.android.ui.PracticeScreen
import com.pronouncecoach.android.ui.SettingsScreen
import com.pronouncecoach.android.ui.SoundPickerScreen
import com.pronouncecoach.android.ui.theme.PronounceCoachTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

private enum class Tab(val label: String) { HOME("Home"), PRACTICE("Practice"), SETTINGS("Settings") }
private enum class SubScreen { CATEGORY, SOUNDS }

class MainActivity : ComponentActivity() {

    private var hasMicPermission by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
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

            val darkOverride = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> null
            }
            // System bar icon contrast must follow the APP's effective theme
            // (Settings override included), not just the system setting.
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = darkOverride ?: systemDark
            val view = LocalView.current
            // One subtle tick for navigation interactions — native-app feel, ~0 cost.
            val tick = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }
            LaunchedEffect(effectiveDark) {
                val activity = view.context as ComponentActivity
                if (effectiveDark) {
                    activity.enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    )
                } else {
                    activity.enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT
                        )
                    )
                }
            }

            CompositionLocalProvider(LocalLang provides language) {

            PronounceCoachTheme(
                darkOverride = darkOverride,
                colorSource = colorSource
            ) {
                val viewModel: MainViewModel = viewModel()
                val pagerState = rememberPagerState(initialPage = 0) { 3 }
                val coroutineScope = rememberCoroutineScope()
                var subScreen by remember { mutableStateOf<SubScreen?>(null) }
                var showCustomInput by remember { mutableStateOf(false) }

                // Cache categories to avoid recomputing on every recomposition
                val categories = remember { viewModel.getCategories() }

                LaunchedEffect(pagerState.currentPage, hasMicPermission) {
                    if (!hasMicPermission && pagerState.currentPage == 1) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }

                BackHandler(enabled = subScreen != null || pagerState.currentPage != 0) {
                    if (subScreen != null) {
                        subScreen = null
                    } else if (pagerState.currentPage != 0) {
                        coroutineScope.launch { pagerState.scrollToPage(0) }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (subScreen == null) {
                            NavigationBar {
                                for (tab in Tab.entries) {
                                    val selected = when (tab) {
                                        Tab.HOME -> pagerState.currentPage == 0
                                        Tab.PRACTICE -> pagerState.currentPage == 1
                                        Tab.SETTINGS -> pagerState.currentPage == 2
                                    }
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = {
                                            if (!selected) {
                                                tick()
                                                coroutineScope.launch {
                                                    val target = when (tab) {
                                                        Tab.HOME -> 0
                                                        Tab.PRACTICE -> 1
                                                        Tab.SETTINGS -> 2
                                                    }
                                                    pagerState.scrollToPage(target)
                                                }
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = when (tab) {
                                                    Tab.HOME -> Icons.Filled.Home
                                                    Tab.PRACTICE -> Icons.Filled.Mic
                                                    Tab.SETTINGS -> Icons.Filled.Settings
                                                },
                                                contentDescription = when (tab) {
                                                    Tab.HOME -> t("Home", "Trang chủ")
                                                    Tab.PRACTICE -> t("Practice", "Luyện tập")
                                                    Tab.SETTINGS -> t("Settings", "Cài đặt")
                                                }
                                            )
                                        },
                                        label = {
                                            Text(
                                                when (tab) {
                                                    Tab.HOME -> t("Home", "Trang chủ")
                                                    Tab.PRACTICE -> t("Practice", "Luyện tập")
                                                    Tab.SETTINGS -> t("Settings", "Cài đặt")
                                                }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    // The pager NEVER leaves composition — sub-screens slide over it as an
                    // overlay, so switching to Category/Sounds doesn't re-mount all 3 pages.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            beyondViewportPageCount = 2,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> HomeScreen(
                                    categories = categories,
                                    onCategoryClick = { id ->
                                        viewModel.stopDrill()
                                        viewModel.setCategory(id)
                                        showCustomInput = false
                                        coroutineScope.launch { pagerState.scrollToPage(1) }
                                    },
                                    onCustomText = {
                                        viewModel.stopDrill()
                                        viewModel.startFreePractice()
                                        showCustomInput = true
                                        coroutineScope.launch { pagerState.scrollToPage(1) }
                                    },
                                    onRandomWord = {
                                        viewModel.stopDrill()
                                        viewModel.loadRandomWord()
                                        showCustomInput = false
                                        coroutineScope.launch { pagerState.scrollToPage(1) }
                                    },
                                    onPickSound = {
                                        tick()
                                        subScreen = SubScreen.SOUNDS
                                    }
                                )
                                1 -> PracticeRoute(
                                    viewModel = viewModel,
                                    showCustomInput = showCustomInput
                                )
                                2 -> SettingsScreen(
                                    themeMode = themeMode,
                                    colorSource = colorSource,
                                    language = language,
                                    onThemeModeChange = {
                                        themeMode = it
                                        Prefs.setThemeMode(this@MainActivity, it)
                                    },
                                    onColorSourceChange = {
                                        colorSource = it
                                        Prefs.setColorSource(this@MainActivity, it)
                                    },
                                    onLanguageChange = {
                                        language = it
                                        Prefs.setLanguage(this@MainActivity, it)
                                    },
                                    onBack = {
                                        coroutineScope.launch { pagerState.scrollToPage(0) }
                                    }
                                )
                            }
                        }

                        // Full-screen sub-screens slide in over the pager (transform-only:
                        // translation is GPU-cheap and never re-measures the underlying UI).
                        AnimatedVisibility(
                            visible = subScreen != null,
                            enter = slideInHorizontally(
                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                initialOffsetX = { it }
                            ),
                            exit = slideOutHorizontally(
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                                targetOffsetX = { it }
                            ),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                when (subScreen) {
                                    SubScreen.CATEGORY -> CategoryScreen(
                                        onCategoryClick = { id ->
                                            viewModel.stopDrill()
                                            viewModel.setCategory(id)
                                            showCustomInput = false
                                            subScreen = null
                                            coroutineScope.launch { pagerState.scrollToPage(1) }
                                        },
                                        onBack = {
                                            tick()
                                            subScreen = null
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    SubScreen.SOUNDS -> SoundPickerScreen(
                                        onBack = {
                                            tick()
                                            subScreen = null
                                        },
                                        onStart = { entry ->
                                            viewModel.startDrill(entry)
                                            showCustomInput = false
                                            subScreen = null
                                            coroutineScope.launch { pagerState.scrollToPage(1) }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    null -> {}
                                }
                            }
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun PracticeRoute(
    viewModel: MainViewModel,
    showCustomInput: Boolean,
    modifier: Modifier = Modifier
) {
    val word by viewModel.currentWord.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    val customText by viewModel.customText.collectAsStateWithLifecycle()
    val isCustomMode by viewModel.isCustomMode.collectAsStateWithLifecycle()
    val drillPhone by viewModel.drillPhone.collectAsStateWithLifecycle()
    val ipaTokens by viewModel.ipaTokens.collectAsStateWithLifecycle()

    PracticeScreen(
        word = word,
        ipaTokens = ipaTokens,
        isRecording = isRecording,
        isAnalyzing = isAnalyzing,
        result = result,
        modelState = modelState,
        customText = customText,
        isCustomMode = isCustomMode,
        showCustomInput = showCustomInput,
        drillPhone = drillPhone,
        onCustomTextChange = { viewModel.setCustomText(it) },
        onStartRecording = { viewModel.startRecording() },
        onStopRecording = { viewModel.stopRecording() },
        onListen = { viewModel.listen() },
        onSpeakWord = { viewModel.speakWord(it) },
        onNextWord = {
            if (viewModel.drillPhone.value != null) viewModel.nextDrillWord()
            else viewModel.loadNextWord()
        },
        modifier = modifier
    )
}
