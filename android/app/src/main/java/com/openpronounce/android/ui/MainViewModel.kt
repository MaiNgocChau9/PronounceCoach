package com.openpronounce.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openpronounce.android.OpenPronounceApp
import com.openpronounce.android.audio.AudioRecorder
import com.openpronounce.android.data.PhonemeEntry
import com.openpronounce.android.data.DrillSorter
import com.openpronounce.android.data.WordDatabase
import com.openpronounce.android.data.WordItem
import com.openpronounce.android.ml.EspeakWrapper
import com.openpronounce.android.ml.G2p
import com.openpronounce.android.scoring.PronunciationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val pipeline = (application as OpenPronounceApp).pipeline
    private val recorder = AudioRecorder(application)
    private val speaker = EspeakWrapper(application)
    private val g2p = G2p(application)

    val isRecording: StateFlow<Boolean> = recorder.isRecording
    val audioLevel: StateFlow<Float> = recorder.audioLevel

    private val _currentWord = MutableStateFlow<WordItem?>(null)
    val currentWord: StateFlow<WordItem?> = _currentWord

    /** Expected phones of the current target, one entry per phone. */
    private val _expectedPhones = MutableStateFlow<List<String>>(emptyList())

    private val _result = MutableStateFlow<PronunciationResult?>(null)
    val result: StateFlow<PronunciationResult?> = _result

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Loading)
    val modelState: StateFlow<ModelState> = _modelState

    private val _history = MutableStateFlow<List<HistoryItem>>(emptyList())
    val history: StateFlow<List<HistoryItem>> = _history

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory

    private val _customText = MutableStateFlow("")
    val customText: StateFlow<String> = _customText

    private val _isCustomMode = MutableStateFlow(false)
    val isCustomMode: StateFlow<Boolean> = _isCustomMode

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    /** Active sound-drill session (null when practicing categories/free text). */
    private val _drillPhone = MutableStateFlow<PhonemeEntry?>(null)
    val drillPhone: StateFlow<PhonemeEntry?> = _drillPhone

    private var drillQueue: List<WordItem> = emptyList()
    private var drillPos = 0

    private var wordIndex = 0

    init {
        loadModels()
        loadNextWord()
    }

    private fun loadModels() {
        viewModelScope.launch {
            _modelState.value = ModelState.Loading
            // Model loading is heavy (91 MB + session warm-up): keep it off the main thread.
            val result = withContext(Dispatchers.Default) { pipeline.loadModels() }
            _modelState.value = if (result.isSuccess) {
                ModelState.Ready
            } else {
                ModelState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun loadNextWord() {
        val category = _selectedCategory.value
        val words = if (category != null) {
            WordDatabase.getCategory(category)?.words ?: WordDatabase.getAllWords()
        } else {
            WordDatabase.getAllWords()
        }

        if (words.isNotEmpty()) {
            wordIndex = wordIndex % words.size
            setTarget(words[wordIndex])
            wordIndex++
        }
    }

    private fun setTarget(word: WordItem) {
        _currentWord.value = word
        _expectedPhones.value = word.ipa.split(Regex("\\s+")).filter { it.isNotEmpty() }
        _result.value = null
        _isCustomMode.value = false
        recorder.clearRecording()
    }

    fun setCustomText(text: String) {
        _customText.value = text
        if (text.isBlank()) return

        // Reference pronunciation for every word from the bundled espeak lexicon
        // (same convention the heard side is decoded into); OOV words are spelled out.
        val phones = g2p.textToPhones(text)

        _currentWord.value = WordItem(word = text.trim(), ipa = phones.joinToString(" "))
        _expectedPhones.value = phones
        _result.value = null
        _isCustomMode.value = true
        recorder.clearRecording()
    }

    /** Free practice: blank slate for typing any word or sentence. */
    fun startFreePractice() {
        _selectedCategory.value = null
        _currentWord.value = null
        _expectedPhones.value = emptyList()
        _customText.value = ""
        _result.value = null
        _isCustomMode.value = true
        recorder.clearRecording()
    }

    fun startRecording() {
        if (!recorder.hasPermission()) return
        _result.value = null
        recorder.clearRecording()
        recorder.startRecording()
    }

    fun stopRecording() {
        recorder.stopRecording()

        viewModelScope.launch {
            val audio = recorder.awaitRecording() ?: return@launch
            val target = _currentWord.value ?: return@launch
            val expected = _expectedPhones.value

            // Silence guard: never score empty air (it used to land ~60 points).
            val peak = peakRms(audio)
            android.util.Log.i("MainViewModel", "recorded ${audio.size / 16000f}s peakRms=$peak")
            if (audio.size < AudioRecorder.SAMPLE_RATE * 2 / 5 || peak < 0.005) {
                _result.value = PronunciationResult.empty(noSpeech = true)
                return@launch
            }

            _isAnalyzing.value = true
            try {
                // ONNX inference is CPU-heavy: never block the UI thread.
                val analysisResult = withContext(Dispatchers.Default) {
                    pipeline.analyze(audio, expected, target.word)
                }
                _result.value = analysisResult

                _history.value = listOf(
                    HistoryItem(target, analysisResult, System.currentTimeMillis())
                ) + _history.value
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /** Rejects clips shorter than 0.4 s or whose loudest 40 ms window is near-silence. */
    private fun isSilentOrTooShort(audio: FloatArray): Boolean {
        val sampleRate = AudioRecorder.SAMPLE_RATE
        if (audio.size < sampleRate * 2 / 5) return true
        return peakRms(audio) < 0.005              // ≈ -46 dBFS: room tone at best
    }

    /** Loudest 40 ms RMS window of the clip. */
    private fun peakRms(audio: FloatArray): Double {
        if (audio.isEmpty()) return 0.0
        val frameLen = AudioRecorder.SAMPLE_RATE / 25
        val hop = frameLen / 2
        var peak = 0.0
        var start = 0
        while (start + frameLen <= audio.size) {
            var sum = 0.0
            for (i in start until start + frameLen) sum += audio[i].toDouble() * audio[i].toDouble()
            val rms = sqrt(sum / frameLen)
            if (rms > peak) peak = rms
            start += hop
        }
        return peak
    }

    fun listen() {
        val word = _currentWord.value ?: return
        // Speak exactly what the user is practicing — not the example sentence.
        speaker.speak(word.word)
    }

    /** Speak a single word (tap-to-hear on the result screen). */
    fun speakWord(word: String) {
        speaker.speak(word)
    }

    fun setCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
        wordIndex = 0
        loadNextWord()
    }

    /** Random word from all categories, for the FAB's "random" shortcut. */
    fun loadRandomWord() {
        _selectedCategory.value = null
        _customText.value = ""
        _isCustomMode.value = false
        val words = WordDatabase.getAllWords()
        if (words.isNotEmpty()) setTarget(words.random())
    }

    // -----------------------------------------------------------------------
    // Sound drill: endless queue of lexicon words containing the chosen phone.
    // -----------------------------------------------------------------------

    /**
     * Builds the drill queue for [entry], ordered easy → hard:
     * 1-syllable words first (sound initial), then longer words with the sound moving
     * toward the end. Words come from the 10k-lexicon, so there is always variety.
     */
    fun startDrill(entry: PhonemeEntry) {
        val raw = g2p.findWords(listOf(entry.tokens))
            .filter { (word, _) -> word.length >= 2 }
            .shuffled()   // randomize so ties within a difficulty bucket vary

        val vi = com.openpronounce.android.data.Prefs.language(getApplication()) == "vi"
        val drillMeaning = if (vi) "Luyện /${entry.symbol}/" else "Drill /${entry.symbol}/"
        val items = raw.mapNotNull { (w, ipa) ->
            val phones = ipa.split(" ").filter { it.isNotEmpty() }
            val syllables = DrillSorter.syllableCount(phones)
            if (syllables == 0) return@mapNotNull null
            val pos = DrillSorter.positionRank(phones, entry.tokens) { t -> t.trimEnd('ː', 'ˑ') }
            Triple(w, ipa, syllables * 10 + pos)
        }
            .sortedBy { it.third }
            .take(60)
            .map { (w, ipa, _) ->
                WordItem(w, ipa, entry.symbol, drillMeaning, "drill", 1)
            }
        if (items.isEmpty()) return

        _selectedCategory.value = null
        _customText.value = ""
        _isCustomMode.value = false
        _drillPhone.value = entry
        drillQueue = items
        drillPos = 0
        setTarget(items[0])
    }

    /** Next word of the drill; wraps around keeping the easy→hard order. */
    fun nextDrillWord() {
        if (_drillPhone.value == null || drillQueue.isEmpty()) {
            loadNextWord()
            return
        }
        if (drillPos + 1 >= drillQueue.size && drillQueue.size > 1) {
            // New pass through the same progression.
            drillPos = 0
        } else if (drillPos + 1 < drillQueue.size) {
            drillPos++
        }
        setTarget(drillQueue[drillPos])
    }

    /** Leaves drill mode (e.g. when navigating elsewhere). */
    fun stopDrill() {
        _drillPhone.value = null
        drillQueue = emptyList()
        drillPos = 0
    }

    fun getCategories() = WordDatabase.getAllCategories()

    override fun onCleared() {
        super.onCleared()
        recorder.destroy()
        speaker.close()
        pipeline.close()
    }
}

sealed class ModelState {
    object Loading : ModelState()
    object Ready : ModelState()
    data class Error(val message: String) : ModelState()
}

data class HistoryItem(
    val word: WordItem,
    val result: PronunciationResult,
    val timestamp: Long
)
