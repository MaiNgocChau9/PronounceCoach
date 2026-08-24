# OpenPronounce Android

> **Fork of [Halleck45/OpenPronounce](https://github.com/Halleck45/OpenPronounce)** (MIT License) by Jean-François Lépine. The original project provides a Python library + web server for phoneme-level pronunciation assessment. This directory contains the Android app that runs the entire pipeline offline on-device.

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

## ⚠️ Kiến trúc thực tế sau refactor (2026-08-23) — ĐỌC KĨ TRƯỚC KHI SỬA

Những điều bắt buộc phải biết để không phá vỡ độ chính xác nhận diện:

1. **Model ONNX trong assets là `wav2vec2-large-960h` (CTC CHỮ CÁI, vocab 32 ký tự),
   KHÔNG PHẢI model phoneme** dù file tên `wav2vec2_phoneme.onnx`. `vocab.txt` cũng là
   vocab chữ cái (E, T, A... + `|` = word separator). Không bao giờ decode trực tiếp
   thành IPA từ model này.
2. **Luồng chấm điểm hiện tại** (mirror đường "words" của Python backend, xem
   `openpronounce/speech.py::compare_transcriptions`):
   ```
   Mic → ONNX 960h → CTC greedy (chữ cái) → clean text
       → G2p.textToPhones() (từ điển espeak en-us)
       → Levenshtein align expected-vs-heard (cùng hệ IPA espeak) → PronunciationScorer
   ```
3. **`assets/models/dict_ipa.txt`** (~10k từ phổ biến tiếng Anh, format
   `word<TAB>ipa`) được sinh bằng phonemizer espeak en-us — cùng nguồn với backend.
   Script sinh lại: dùng `phonemize(words, language='en-us', backend='espeak',
   separator=Separator(phone=' ', word='_'))` rồi thay `_` bằng space.
4. **IPA trong `WordDatabase.kt` đã được chuẩn hóa theo hệ espeak en-us** (ví dụ
   "water" = `w ɔː ɾ ɚ`, có `ɹ` không phải `r`). Khi thêm từ mới, lấy IPA từ
   `dict_ipa.txt` hoặc phonemizer, đừng tự viết kiểu khác (`ɜːr`, `r`, `ɒ`...) vì sẽ
   lệch hệ với phía heard.
5. **`G2p.kt`** (ml/) nạp dict_ipa.txt; OOV thì đánh vần từng chữ cái. `EspeakWrapper`
   chỉ còn vai trò TTS, không còn ipaMap.
6. **UI flow**: Home giữ danh mục category; nút Quick practice đã bỏ. Thay bằng
   **CreateFab** (ui/CreateFab.kt) ở bottom-right, shape "petal" (RoundedCornerShape
   bất đối xứng), mở ra 2 action: Custom text (→ Practice `showCustomInput=true`,
   ViewModel.startFreePractice) và Random (→ loadRandomWord, `showCustomInput=false`).
   Category click → Practice `showCustomInput=false` (KHÔNG có ô input).
7. **Icon micro** dùng `MaterialTheme.colorScheme.onPrimary/onError` — không hard-code
   `Color.White` (mất tích trong dark mode vì primary màu nhạt).
8. **Scoring (2026-08-23 lần 2)**:
   - KHÔNG có acoustic term trên Android (trọng số 0.6 phones / 0.4 words). Trước đây
     `acousticDistance=0` mặc định → 30 điểm miễn phí, im lặng vẫn được ~60đ.
   - Silence guard: `MainViewModel.isSilentOrTooShort()` chặn clip <0.4s hoặc peak RMS
     <0.005 (≈ -46 dBFS) → trả `PronunciationResult.empty(noSpeech=true)`, UI hiện
     NoSpeechCard thay vì điểm.
   - Chấm theo TỪNG từ: `G2p.textToWordPhones` → `TargetWord(text, phones, startIndex)`
     → align global trong `PronunciationScorer`, quy cost về từng từ (sub near-phone =
     0.5, del = 1.0, ins = 0.5 gán cho từ gần nhất). Word pass ≥60%.
   - Result UI (`PracticeScreen.ResultPanel`): verdict + ScoreRing, FlowRow chip màu
     theo từ (xanh ≥80 / vàng ≥60 / đỏ <60), tap chip để nghe lại từ đó
     (`viewModel.speakWord`), panel "Sounds to improve" chỉ hiện các từ yếu.
9. **FAB** giờ là hình vuông bo góc (`mainFabShape` 20dp / `smallFabShape` 14dp).
10. **PracticeScreen layout**: cột ngoài KHÔNG scroll; chỉ cột trên (target + input +
    result) có `verticalScroll` + `weight(1f)`; mic + hint + nút Listen/Next nằm NGOÀI
    vùng scroll, cố định đáy. Đừng đưa `weight` vào trong Column đang `verticalScroll`
    (height vô hạn → weight vô nghĩa, nút trôi khỏi màn hình khi result dài).
11. **Result UI theo ELSA (2026-08-23 lần 3)**: `ScoreBanner` (emoji + verdict +
    "You sound X% like a native speaker!" + vòng %), câu inline `ColoredWord` (tô màu
    xanh/vàng/đỏ + underline, tap nghe lại từ), `WordDetailCard` = bảng "Sound | You
    said" kèm mẹo phát âm từ `PhoneTips.kt`, hàng "not heard" cho âm bị mất. Bottom bar:
    [Listen] MIC [Next] dạng nút tròn flanking, cố định. ScoreRing đã bỏ.
12. **Per-phone coloring (2026-08-23 lần 4)**: `WordError.phoneCorrect` (List<Boolean>
    cùng số phần tử với token trong expectedIpa) được điền từ ops align (sub/del = sai).
    Header từ + dòng IPA dùng `coloredIpa()` tô từng token xanh/đỏ khi có result. Mic
    thu nhỏ: canvas 150dp / button 68dp / icon 28dp, bottom area min 165dp.
13. **Làm gọn (2026-08-23 lần 5)**: nút Next (SkipNext) ẨN khi `isCustomMode` (chế độ
    tự nhập — Next chỉ có ý nghĩa với word list). Vòng % trong ScoreBanner vẽ arc theo
    đúng percent (`360f * percent / 100f`) thay vì luôn full. Khi câu có result:
    header hiển thị CHỈ câu màu (FlowRow ColoredWord big) — không còn câu trắng trùng
    phía trên; ResultPanel chỉ còn banner + bảng chi tiết. Bottom bar: mic center
    tuyệt đối qua BoxScope.align, Listen CenterStart, Next CenterEnd.
14. **Palette ngữ nghĩa đậm (2026-08-23 lần 6)**: KHÔNG dùng dynamic color cho feedback —
    `ScoreGood 0xFF1FA05A / ScoreMid 0xFFF39C12 / ScoreBad 0xFFE5484D` cố định trong
    PracticeScreen (scoreColor, coloredIpa, banner tint nền alpha 0.12 + border 0.45,
    "you said" đỏ). ScoreBanner có margin-top 18dp; NoSpeechCard tương tự. Từ màu
    được Bold + underline cho dễ đọc.
15. **Bảng âm đầy đủ + bottom bar gọn (2026-08-23 lần 7)**: `WordError.phoneHeard`
    lưu heard phone theo vị trí; `WordDetailCard` liệt kê MỌI âm của từ khi accuracy
    <100% (✓ said well xanh / you said /x/ đỏ / not heard), tips chỉ dưới âm sai.
    Bottom bar cuối cùng (lần 8): hàng ngang [Listen trái] ...spacer... [Next][Mic phải],
    cả 3 nút 48dp giống hệt nhau; mic theo màu Material (primary/onPrimary, error khi
    ghi), pulse scale nhẹ khi ghi.
16. **Frosted bottom bar (lần 9)**: root là Box; cột nội dung scroll FULL height (có
    Spacer 110dp cuối) để card trượt DƯỚI bar; overlay đáy = gradient
    `Brush.verticalGradient(surface 0f→1f, cao 30dp)` + Surface(tonalElevation 2) chứa
    hàng nút + hint text. Hết vết cắt cứng khi nội dung chạm đáy.

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
