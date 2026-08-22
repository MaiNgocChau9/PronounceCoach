package com.openpronounce.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openpronounce.android.OpenPronounceApp
import com.openpronounce.android.audio.AudioRecorder
import com.openpronounce.android.data.WordDatabase
import com.openpronounce.android.data.WordItem
import com.openpronounce.android.scoring.PronunciationResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val pipeline = (application as OpenPronounceApp).pipeline
    private val recorder = AudioRecorder(application)

    val isRecording: StateFlow<Boolean> = recorder.isRecording
    val audioLevel: StateFlow<Float> = recorder.audioLevel

    private val _currentWord = MutableStateFlow<WordItem?>(null)
    val currentWord: StateFlow<WordItem?> = _currentWord

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

    private val _recordingDuration = MutableStateFlow(0f)
    val recordingDuration: StateFlow<Float> = _recordingDuration

    private var wordIndex = 0

    init {
        loadModels()
        loadNextWord()
    }

    private fun loadModels() {
        viewModelScope.launch {
            _modelState.value = ModelState.Loading
            val result = pipeline.loadModels()
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
            _currentWord.value = words[wordIndex]
            _result.value = null
            _isCustomMode.value = false
            recorder.clearRecording()
            wordIndex++
        }
    }

    fun setCustomText(text: String) {
        _customText.value = text
        if (text.isNotBlank()) {
            _isCustomMode.value = true
            _currentWord.value = WordItem(
                word = text,
                ipa = "",
                meaning = "Custom text"
            )
        }
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
            val word = _currentWord.value ?: return@launch
            val audio = recorder.getRecordedAudio() ?: return@launch

            val analysisResult = pipeline.analyze(audio, word)
            _result.value = analysisResult

            val historyItem = HistoryItem(
                word = word,
                result = analysisResult,
                timestamp = System.currentTimeMillis()
            )
            _history.value = listOf(historyItem) + _history.value
        }
    }

    fun setCategory(categoryId: String?) {
        _selectedCategory.value = categoryId
        wordIndex = 0
        loadNextWord()
    }

    fun getCategories() = WordDatabase.getAllCategories()

    override fun onCleared() {
        super.onCleared()
        recorder.destroy()
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
