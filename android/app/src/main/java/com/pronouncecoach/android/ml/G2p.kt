package com.pronouncecoach.android.ml

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Grapheme-to-phoneme lookup backed by assets/models/dict_ipa.txt (~10k common English
 * words, generated with the same espeak en-us phonemizer the Python backend uses, so the
 * expected and the heard phones always live in the same IPA convention).
 *
 * Format: one entry per line, `word<TAB>ipa phones space separated`.
 */
class G2p(context: Context) {

    companion object {
        private const val TAG = "G2p"
        private const val ASSET = "models/dict_ipa.txt"

        /** Fallback for out-of-dictionary words (names...): letter-name phones. */
        private val LETTER_PHONES = mapOf(
            'a' to "eɪ", 'b' to "b iː", 'c' to "s iː", 'd' to "d iː", 'e' to "iː",
            'f' to "ɛ f", 'g' to "dʒ iː", 'h' to "eɪ tʃ", 'i' to "aɪ", 'j' to "dʒ eɪ",
            'k' to "k eɪ", 'l' to "ɛ l", 'm' to "ɛ m", 'n' to "ɛ n", 'o' to "oʊ",
            'p' to "p iː", 'q' to "k j uː", 'r' to "ɑː ɹ", 's' to "ɛ s", 't' to "t iː",
            'u' to "j uː", 'v' to "v iː", 'w' to "d ʌ b əl j uː", 'x' to "ɛ k s",
            'y' to "w aɪ", 'z' to "z iː"
        )

        private val dict = ConcurrentHashMap<String, String>()
        @Volatile
        private var loaded = false
        private val lock = Any()

        fun loadIfNeeded(context: Context) {
            if (loaded) return
            synchronized(lock) {
                if (loaded) return
                runCatching {
                    context.assets.open(ASSET).bufferedReader().useLines { lines ->
                        for (line in lines) {
                            val idx = line.indexOf('\t')
                            if (idx > 0) dict[line.substring(0, idx)] = line.substring(idx + 1)
                        }
                    }
                    loaded = true
                }
            }
        }
    }

    init {
        loadIfNeeded(context.applicationContext)
    }

    val isReady: Boolean get() = loaded

    /** IPA phone string of a single word, or null when unknown even as letters. */
    fun lookup(word: String): String? {
        val key = word.lowercase()
        return dict[key] ?: lettersToIpa(key)
    }

    /**
     * Phones of every word in [text], mirroring the Python pipeline's
     * get_phonemes_with_word_mapping: punctuation ignored, unknown words spelled out.
     *
     * With [fuzzy] (HEARD side only!), near-miss ASR transcriptions are corrected to
     * the closest dictionary word before conversion — "warter" -> "water" — instead of
     * being spelled out letter by letter.
     */
    fun textToPhones(text: String, fuzzy: Boolean = false): List<String> =
        Regex("[A-Za-z']+").findAll(text)
            .map { it.value }
            .flatMap { word -> phonesOf(word, fuzzy) }
            .toList()

    /** Same as [textToPhones] but keeps the word boundaries: (word, phones) in order. */
    fun textToWordPhones(text: String, fuzzy: Boolean = false): List<Pair<String, List<String>>> =
        Regex("[A-Za-z']+").findAll(text)
            .map { m -> m.value to phonesOf(m.value, fuzzy) }
            .filter { it.second.isNotEmpty() }
            .toList()

    private fun phonesOf(word: String, fuzzy: Boolean): List<String> {
        val key = word.lowercase()
        val ipa = dict[key]
            ?: (if (fuzzy) fuzzyLookup(key)?.also { android.util.Log.i(TAG, "fuzzy: $word -> $it") }
                ?.let { dict[it] } else null)
            ?: lettersToIpa(key)
        return ipa?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: emptyList()
    }

    /**
     * Closest dictionary word within a small edit-distance budget. The dictionary is
     * frequency-ranked, so the first entry at the lowest distance wins (ties favor
     * common words).
     */
    private fun fuzzyLookup(word: String): String? {
        val budget = when {
            word.length <= 4 -> 1
            word.length <= 8 -> 2
            else -> 3
        }
        var best: String? = null
        var bestDistance = Int.MAX_VALUE
        for ((candidate, _) in dict) {
            if (kotlin.math.abs(candidate.length - word.length) > budget) continue
            val d = levenshtein(candidate, word, budget)
            if (d in 1 until bestDistance) {
                bestDistance = d
                best = candidate
            }
        }
        return if (bestDistance <= budget) best else null
    }

    /** Bounded Levenshtein distance; bails out early once over [cap]. */
    private fun levenshtein(a: String, b: String, cap: Int): Int {
        if (a == b) return 0
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            var rowMin = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost)
                if (curr[j] < rowMin) rowMin = curr[j]
            }
            if (rowMin > cap) return cap + 1
            System.arraycopy(curr, 0, prev, 0, curr.size)
        }
        return prev[b.length]
    }

    /**
     * Lexicon words whose phones contain any of [sequences] (contiguous match), in
     * frequency order. Tokens are compared after stripping length marks, so a search
     * for "ɔː" also hits espeak's bare "ɔ".
     */
    fun findWords(sequences: List<List<String>>, limit: Int = 250): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        outer@ for ((word, ipa) in dict) {
            if (out.size >= limit) break
            val phones = ipa.split(" ").filter { it.isNotEmpty() }.map { normToken(it) }
            for (seq in sequences) {
                val target = seq.map { normToken(it) }
                if (target.size > phones.size) continue
                var i = 0
                while (i <= phones.size - target.size) {
                    var ok = true
                    for (j in target.indices) {
                        if (phones[i + j] != target[j]) { ok = false; break }
                    }
                    if (ok) { out.add(word to ipa); continue@outer }
                    i++
                }
            }
        }
        return out
    }

    private fun normToken(t: String): String =
        t.trimEnd('ː', 'ˑ').replace("ɝ", "ɜ")

    /** Unknown words are spelled out letter by letter, like espeak would guess them. */
    private fun lettersToIpa(word: String): String? {
        if (word.isEmpty() || !word.all { it in LETTER_PHONES }) return null
        return word.mapNotNull { LETTER_PHONES[it] }.joinToString(" ").ifEmpty { null }
    }
}
