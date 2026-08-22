package com.openpronounce.android.ml

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class EspeakWrapper(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.US)
                isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                         result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    /**
     * Convert IPA string to list of phoneme tokens.
     * Input: "/h ə l oʊ/" or "həloʊ"
     * Output: ["h", "ə", "l", "oʊ"]
     */
    fun splitIpa(ipa: String): List<String> {
        return ipa
            .replace("/", "")
            .replace("\\", "")
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
    }

    /**
     * Convert a word to its IPA representation using a lookup table.
     * This is a simplified version - in production, use espeak-ng native binding.
     */
    fun wordToIpa(word: String): String {
        // Basic English phoneme rules (simplified)
        val ipaMap = mapOf(
            "hello" to "h ə l oʊ",
            "world" to "w ɜːr l d",
            "cat" to "k æ t",
            "dog" to "d ɒ ɡ",
            "book" to "b ʊ k",
            "house" to "h aʊ s",
            "water" to "w ɔː t ər",
            "father" to "f ɑː ð ər",
            "mother" to "m ʌ ð ər",
            "friend" to "f r ɛ n d",
            "school" to "s k uː l",
            "apple" to "æ p əl",
            "orange" to "ɒ r ɪ n dʒ",
            "computer" to "k əm p j uː t ər",
            "language" to "l æ ŋ gw ɪ dʒ",
            "pronunciation" to "p r ə n ʌ n s i eɪ ʃ ən",
            "beautiful" to "b j uː t ɪ f əl",
            "important" to "ɪm p ɔːr t ənt",
            "different" to "d ɪ f r ənt",
            "experience" to "ɪk s p ɪə r i ən s"
        )

        return ipaMap[word.lowercase()] ?: word
    }

    fun speak(text: String,utteranceId: String = "tts_${System.currentTimeMillis()}") {
        if (isReady) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        }
    }

    fun close() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
