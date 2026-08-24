# AGENTS.md — OpenPronounce Project Context

> **Original project**: [Halleck45/OpenPronounce](https://github.com/Halleck45/OpenPronounce)
> **This fork**: [MaiNgocChau9/OpenPronounce-fork](https://github.com/MaiNgocChau9/OpenPronounce-fork)
> **Author of original**: Jean-François Lépine (MIT License)
> **This fork was developed with AI-assisted development (vibecoding)** using OpenCode / Claude.

## Project Overview

OpenPronounce is an open-source, phoneme-level pronunciation assessment tool. The original project provides a Python library + web server + CLI for scoring English (and experimental multilingual) pronunciation from audio recordings. This fork adds a fully offline Android app with Material You UI.

### What changed in this fork

1. **Android app** (`android/`) — Native Jetpack Compose app that runs the entire pipeline offline on-device
2. **Web UI polish** — Local Tailwind CSS (no CDN), lazy-loaded Chart.js, haptic feedback
3. **Performance optimization** — Pre-computed IPA tokens, HorizontalPager tabs, SplashScreen API, Edge-to-edge, recomposition optimization across all screens
4. **Dark mode** — Full dark theme with `values-night/themes.xml`
5. **Vietnamese localization** — Bilingual EN/VI throughout the Android app via `t()` composable
6. **G2p singleton** — Dictionary loaded once at app startup, shared across ViewModel and Pipeline

## Architecture

### Python Backend (server/CLI — original project)

```
Audio → trim → Wav2Vec2 embeddings → DTW alignment → Scoring
         ↓
    Transcription (wav2vec2-large-960h)
    Phone recognition (wav2vec2-lv-60-espeak-cv-ft)
    Prosody (pYIN pitch + RMS energy)
    Reference synthesis (gTTS/Piper/Kokoro)
```

### Android App (this fork)

```
Mic → AudioRecorder (16kHz mono) → DeepFilterNet3 (denoise, optional)
    → Wav2Vec2 ONNX (wav2vec2-large-960h, CTC letter vocab)
    → CTC greedy decode → clean text
    → G2p.textToPhones() (espeak en-us dictionary)
    → Levenshtein align expected-vs-heard
    → PronunciationScorer → UI
```

**Critical**: The ONNX model in assets is `wav2vec2-large-960h` (CTC LETTER recognition, vocab 32 chars), NOT a phoneme model — despite the filename `wav2vec2_phoneme.onnx`. Never decode IPA directly from this model.

## Key Files

### Android (`android/app/src/main/java/com/openpronounce/android/`)

| File | Role |
|------|------|
| `MainActivity.kt` | Navigation, HorizontalPager (3 tabs), sub-screen overlays, haptics, edge-to-edge, splash |
| `OpenPronounceApp.kt` | App startup, G2p dictionary pre-loading |
| `ui/MainViewModel.kt` | All state management, `startDrill()` on background, `Pipeline.recognize()` on `Dispatchers.Default` |
| `ui/PracticeScreen.kt` | Heaviest UI — recording animation, Canvas, FlowRow, ELSA-style result panel, `coloredIpa()`, `WordDetailCard` |
| `ui/HomeScreen.kt` | Hero card, category LazyColumn with keyed items |
| `ui/SettingsScreen.kt` | Theme/language/color picker, Palette color constants |
| `ui/CreateFab.kt` | Expandable FAB with rotation animation, 3 actions |
| `ui/L10n.kt` | `compositionLocalOf` language system, `t()` composable |
| `ui/SoundPickerScreen.kt` | IPA phoneme grid with articulation tips |
| `ui/CategoryScreen.kt` | Word category list |
| `ui/PhoneTips.kt` | Per-phoneme articulation tips (static data) |
| `ui/theme/Theme.kt` | M3 color schemes (Green/Blue/Purple + dynamic), `wallpaperPrimaryColor()` |
| `ml/G2p.kt` | Dictionary-based grapheme-to-phoneme, singleton |
| `ml/EspeakWrapper.kt` | espeak-ng TTS wrapper |
| `ml/PhonemeRecognizer.kt` | Wav2Vec2 ONNX inference |
| `ml/PronunciationPipeline.kt` | Main analysis pipeline |
| `scoring/ScoringModels.kt` | Data classes: `WordError`, `PronunciationResult` |
| `scoring/PronunciationScorer.kt` | Final score computation |
| `scoring/LevenshteinAligner.kt` | Phone alignment |
| `scoring/PhoneNormalizer.kt` | IPA normalization |
| `data/WordDatabase.kt` | Built-in word database (50+ words, 5 categories) |
| `data/WordItem.kt` | Word data model |
| `data/Prefs.kt` | SharedPreferences wrapper |
| `data/PhonemeCatalog.kt` | Complete IPA phoneme catalog with articulation tips |
| `audio/AudioRecorder.kt` | Microphone recording |

### Python Backend

| File | Role |
|------|------|
| `server.py` | FastAPI web server |
| `openpronounce/speech.py` | Core scoring logic |
| `openpronounce/phones.py` | Phone normalization, alignment |
| `openpronounce/tts.py` | Text-to-speech backends |
| `templates/index.html` | Web UI |
| `static/ui.js` | Frontend logic |
| `static/tailwind.min.css` | Pre-built Tailwind (16KB) |

## Build & Deploy

### Android

```bash
cd android
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Python Server

```bash
pip install torch --index-url https://download.pytorch.org/whl/cpu
pip install openpronounce
# or
docker run -p 8000:8000 $(docker build -q .)
```

## Coding Conventions

### Kotlin/Compose

- **Language**: Bilingual EN/VI via `t("English", "Tiếng Việt")` composable from `L10n.kt`
- **Theme**: Material 3 Expressive with `compositionLocalOf` for language
- **State**: `collectAsStateWithLifecycle()` for ViewModel Flows
- **Performance**: Use `remember {}` for expensive computations, `key = { ... }` in LazyColumn/LazyGrid
- **Shapes**: Stable `private val` not `private fun` for Compose shapes
- **Haptics**: `HapticFeedbackConstants.CONFIRM` for user actions
- **Sub-screens**: Slide over pager via `AnimatedVisibility` + `slideInHorizontally` (never unmount pager)
- **Edge-to-edge**: Synced with app theme override, not just system

### IPA

- All IPA uses **espeak en-us** convention (e.g., `ɹ` not `r`, `ɔː` not `ɔ`)
- `dict_ipa.txt` (~10k words) generated by phonemizer espeak en-us
- Never invent IPA — always use `dict_ipa.txt` or phonemizer output

### Scoring

- Android: `0.6 × phone_accuracy + 0.4 × word_accuracy` (no acoustic term)
- Silence guard: clips <0.4s or peak RMS <0.005 → `NoSpeechCard`
- Per-word scoring: each word gets `accuracyPercent`, `phoneCorrect`, `phoneHeard`
- Feedback colors: `ScoreGood=#1FA05A`, `ScoreMid=#F39C12`, `ScoreBad=#E5484D` (semantic, not dynamic)

## Common Pitfalls

1. **ONNX model is letter-based, not phoneme-based** — don't decode IPA from its output
2. **`coloredIpa()` needs pre-computed tokens** — pass `ipaTokens` from ViewModel, don't split in composable
3. **`weight(1f)` inside `verticalScroll` Column** — infinite height makes weight meaningless; put scrollable content in inner Column
4. **DynamicSwatch must use `wallpaperPrimaryColor()`** — not `MaterialTheme.colorScheme.primary` which changes with palette selection
5. **`t()` is @Composable** — cannot be called inside `remember {}` blocks; call outside first, pass as key
6. **`HorizontalPager` with `beyondViewportPageCount = 2`** — all 3 pages always composed; never unmount them with `when` block
7. **`LocalLang` uses `compositionLocalOf`** — not `staticCompositionLocalOf` (language changes at runtime)
8. **`G2p` is a singleton** — dictionary loaded once in `OpenPronounceApp.onCreate()`, shared across ViewModel and Pipeline

## Testing

```bash
# Python tests
cd /home/aurora/Downloads/OpenPronounce
pytest

# Android build check
cd android && ./gradlew assembleDebug

# Verify no crashes on device
adb shell am start -n com.openpronounce.android/.MainActivity
```

## Git Conventions

- Commit messages: `type(scope): description`
- Types: `feat`, `fix`, `perf`, `chore`, `docs`
- Scopes: `android`, `web`, `server`, `ml`, `scoring`, `ui`, `data`
- Keep commits atomic and focused
