# OpenPronounce Android

English pronunciation learning app with AI-powered phoneme-level assessment. Runs 100% offline on Android.

## Features

- **Phoneme-level assessment**: See exactly which sounds you mispronounce
- **AI-powered**: Uses Wav2Vec2 model for accurate speech recognition
- **Noise suppression**: DeepFilterNet3 for real-time noise reduction
- **Offline**: No internet required after first launch
- **5 categories**: Basics, Food, Travel, Work, Nature (50+ words)

## Architecture

```
Microphone → DeepFilterNet3 (denoise) → Wav2Vec2-Phoneme (ONNX) → Scoring Engine → UI
```

## Requirements

- Android 7.0+ (API 24)
- 4GB+ RAM recommended
- ~400MB storage (for models)

## Setup

### 1. Export Model (on your computer)

```bash
pip install torch transformers onnx onnxruntime
cd android
python export_model.py
```

This creates `wav2vec2_phoneme.onnx` and `vocab.txt`.

### 2. Download DeepFilterNet Models

Download from HuggingFace:
```bash
# Download these files to android/app/src/main/assets/models/deepfilter/
# - enc.onnx
# - erb_dec.onnx
# - df_dec.onnx

wget https://huggingface.co/soniqo/DeepFilterNet3-ONNX/resolve/main/enc.onnx
wget https://huggingface.co/soniqo/DeepFilterNet3-ONNX/resolve/main/erb_dec.onnx
wget https://huggingface.co/soniqo/DeepFilterNet3-ONNX/resolve/main/df_dec.onnx
```

### 3. Build APK

1. Open `android/` folder in Android Studio
2. Sync Gradle
3. Build → Build Bundle(s) / APK(s) → Build APK(s)

Or via command line:
```bash
cd android
./gradlew assembleDebug
```

APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

```
android/
├── app/src/main/java/com/openpronounce/android/
│   ├── audio/
│   │   ├── AudioRecorder.kt      # Microphone recording
│   │   └── NoiseSuppressor.kt    # DeepFilterNet wrapper
│   ├── ml/
│   │   ├── PhonemeRecognizer.kt  # Wav2Vec2 ONNX inference
│   │   ├── PronunciationPipeline.kt # Main analysis pipeline
│   │   └── EspeakWrapper.kt      # Text-to-IPA conversion
│   ├── scoring/
│   │   ├── CtcDecoder.kt         # CTC greedy decoding
│   │   ├── PhoneNormalizer.kt    # IPA normalization
│   │   ├── LevenshteinAligner.kt # Phone alignment
│   │   ├── ConfidenceScorer.kt   # Per-phone confidence
│   │   ├── PronunciationScorer.kt # Final score computation
│   │   └── ScoringModels.kt      # Data classes
│   ├── data/
│   │   ├── WordItem.kt           # Word data model
│   │   ├── WordDatabase.kt       # Built-in word database
│   │   └── WordDatabase.kt       # Word categories
│   ├── ui/
│   │   ├── MainViewModel.kt      # State management
│   │   ├── HomeScreen.kt         # Home screen
│   │   ├── PracticeScreen.kt     # Practice screen
│   │   ├── CategoryScreen.kt     # Category selection
│   │   └── HistoryScreen.kt      # History screen
│   ├── MainActivity.kt           # Main activity
│   └── OpenPronounceApp.kt       # Application class
├── export_model.py               # Model export script
└── app/src/main/assets/models/   # Model files (not in git)
```

## How It Works

1. **Record**: User speaks a word
2. **Denoise**: DeepFilterNet3 removes background noise
3. **Recognize**: Wav2Vec2 outputs IPA phonemes with confidence
4. **Align**: Compare expected vs heard phonemes using Levenshtein
5. **Score**: Weighted combination of acoustic, phoneme, and word accuracy
6. **Feedback**: Show which specific sounds were mispronounced

## Adding More Words

Edit `WordDatabase.kt` to add new words:

```kotlin
WordItem(
    word = "yourword",
    ipa = "y ʊər w ɜːr d",  // IPA with spaces
    meaning = "your definition",
    example = "Example sentence.",
    category = "basics",      // or create new category
    difficulty = 1            // 1=easy, 2=medium, 3=hard
)
```

## License

MIT License - Same as OpenPronounce
