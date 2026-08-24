package com.pronouncecoach.android.data

/**
 * Catalog of practiceable sounds. [tokens] are espeak en-us phone sequences used to
 * search the lexicon (see G2p.findWords): length marks stripped, US equivalents
 * substituted (əʊ→oʊ, r→ɹ, g→ɡ, ɒ→ɑ).
 * [tip] / [tipVi] are the articulation guides shown before practicing.
 */
data class PhonemeEntry(
    val symbol: String,
    val type: String,            // "Vowel" | "Diphthong" | "Consonant" | "Cluster"
    val tokens: List<String>,
    val tip: String,
    val tipVi: String
)

object PhonemeCatalog {

    private fun e(
        symbol: String, type: String, tipEn: String, tipVn: String, vararg tokens: String
    ) = PhonemeEntry(symbol, type, tokens.toList(), tipEn, tipVn)

    val vowels = listOf(
        e("iː", "Vowel", "Stretch your lips into a wide smile, tongue high and front. Hold it long: s-ea.",
            "Kéo môi thành nụ cười rộng, lưỡi cao và đưa về trước. Giữ âm dài: s-ê."),
        e("ɪ", "Vowel", "Relaxed short vowel — tongue slightly lower than /iː/. Think “ship”, not “sheep”.",
            "Nguyên âm ngắn, thư giãn — lưỡi thấp hơn /iː/ một chút. Nghĩ “ship” chứ không phải “sheep”."),
        e("e", "Vowel", "Mid-front vowel: jaw slightly dropped, lips unrounded, as in “bed”.",
            "Nguyên âm giữa-trước: hàm hơi mở, môi không tròn, như trong “bed”."),
        e("æ", "Vowel", "Drop your jaw wide and spread your lips; tongue low and front, as in “cat”.",
            "Mở hàm rộng, kéo môi sang hai bên; lưỡi thấp về trước, như trong “cat”."),
        e("ʌ", "Vowel", "Short central vowel — mouth relaxed and half-open, as in “cup”.",
            "Nguyên âm ngắn ở giữa — miệng thả lỏng, hé nửa miệng, như trong “cup”."),
        e("ɑː", "Vowel", "Open back vowel: drop the jaw, tongue low and back, like “f-ah-ther”.",
            "Nguyên âm sau mở: hạ hàm dưới, lưỡi thấp về sau, như “f-a-ther”."),
        e("ɒ", "Vowel", "Rounded lips with tongue low-back (UK “hot”). In American English it merges into /ɑː/.",
            "Môi hơi tròn, lưỡi thấp về sau (tiếng Anh-Anh “hot”). Tiếng Anh-Mỹ thường nhập vào /ɑː/."),
        e("ɔː", "Vowel", "Round your lips, tongue mid-low back; hold it long, as in “law”.",
            "Môi tròn, lưỡi giữa-thấp-về sau; giữ âm dài, như trong “law”."),
        e("ʊ", "Vowel", "Short rounded vowel — lips loosely rounded, tongue high-back, as in “book”.",
            "Nguyên âm ngắn có làm tròn môi — môi hơi tròn, lưỡi cao về sau, như “book”."),
        e("uː", "Vowel", "Tongue high-back with lips tightly rounded; hold long, like “f-oo-d”.",
            "Lưỡi cao về sau, môi chúm chặt; giữ âm dài, như “f-u-d”."),
        e("ə", "Vowel", "The schwa: completely relaxed, very short. It is the sound of every unstressed syllable in English.",
            "Âm schwa: thả lỏng hoàn toàn, rất ngắn. Đây là âm của mọi âm tiết không nhấn trong tiếng Anh.")
    )

    val diphthongs = listOf(
        e("eɪ", "Diphthong", "Glide smoothly from /e/ to /ɪ/ in one motion: “d-ay”.",
            "Trượt mượt từ /e/ sang /ɪ/ trong một động tác: “d-ây”."),
        e("aɪ", "Diphthong", "Start with open /a/, glide up to /ɪ/: “t-i-me”.",
            "Bắt đầu từ /a/ mở, trượt lên /ɪ/: “t-ai-m”."),
        e("ɔɪ", "Diphthong", "Blend /ɔ/ into /ɪ/ — lips start rounded then relax: “b-oy”.",
            "Hòa /ɔ/ vào /ɪ/ — môi bắt đầu tròn rồi thả lỏng: “b-ơi”."),
        e("aʊ", "Diphthong", "Start open /a/, glide into rounded /ʊ/: “n-ow”.",
            "Bắt đầu /a/ mở, trượt vào /ʊ/ tròn môi: “n-au”."),
        e("əʊ", "Diphthong", "Start mid-central then round your lips into /ʊ/: “g-o”.",
            "Bắt đầu ở giữa miệng rồi chúm môi vào /ʊ/: “g-ô”.")
    )

    val consonants = listOf(
        e("p", "Consonant", "Close both lips fully, build pressure, pop them open — no voice.",
            "Khép chặt hai môi, tạo áp suất rồi bật mở — không rung dây thanh."),
        e("b", "Consonant", "Same as /p/ but voiced — feel the buzz in your lips.",
            "Giống /p/ nhưng có thanh — cảm nhận sự rung ở môi."),
        e("t", "Consonant", "Tap the ridge behind your upper teeth with the tongue tip; sharp release.",
            "Đầu lưỡi chạm nướu trên rồi tách nhanh, khí thoát dứt khoát."),
        e("d", "Consonant", "Same position as /t/ but voiced — the buzz starts in the hold.",
            "Cùng vị trí /t/ nhưng có thanh — độ rung bắt đầu ngay khi giữ âm."),
        e("k", "Consonant", "Press the back of your tongue against the soft palate, then release.",
            "Phần sau lưỡi áp lên vòm mềm rồi bật ra."),
        e("g", "Consonant", "Same position as /k/ but voiced — throat buzzes before release.",
            "Giống /k/ nhưng có thanh — cổ họng rung trước khi bật."),
        e("tʃ", "Consonant", "Start like /t/, release into “sh”: “ch” as in church.",
            "Bắt đầu như /t/, thoát ra thành “sh”: “ch” trong church."),
        e("dʒ", "Consonant", "Start like /d/, release into /ʒ/: “j” as in jump.",
            "Bắt đầu như /d/, thoát ra thành /ʒ/: “j” trong jump."),
        e("f", "Consonant", "Rest upper teeth on lower lip; blow air without voice.",
            "Răng trên đặt lên môi dưới; thổi khí mà không rung thanh."),
        e("v", "Consonant", "Same as /f/ but voiced — the lip tickles with vibration.",
            "Giống /f/ nhưng có thanh — môi nhấp nháy vì rung."),
        e("θ", "Consonant", "Place your tongue between your teeth and blow air out, voiceless.",
            "Đưa đầu lưỡi ra giữa hàm răng rồi thổi khí, không rung thanh."),
        e("ð", "Consonant", "Tongue between the teeth like /θ/, but add voice — it buzzes.",
            "Lưỡi để giữa răng như /θ/ nhưng thêm thanh — sẽ bị rung."),
        e("s", "Consonant", "Tongue close to the ridge behind the teeth; push a thin stream of air.",
            "Lưỡi sát nướu sau răng; đẩy luồng khí mảnh và mạnh."),
        e("z", "Consonant", "Like /s/ but voiced — feel the buzz in your throat.",
            "Giống /s/ nhưng có thanh — cảm nhận rung trong cổ họng."),
        e("ʃ", "Consonant", "Pull the tongue slightly back from /s/, round your lips: “sh”.",
            "Kéo lưỡi lùi hơn /s/ một chút, môi tròn: “sh”."),
        e("ʒ", "Consonant", "Like /ʃ/ but voiced — the “si” in vision.",
            "Giống /ʃ/ nhưng có thanh — như “si” trong vision."),
        e("h", "Consonant", "Just breathe out warmly, like fogging a mirror.",
            "Chỉ cần thở ra nhẹ nhàng, như làm mờ gương bằng hơi."),
        e("m", "Consonant", "Close your lips and hum through your nose.",
            "Khép hai môi và phát âm xuyên qua mũi."),
        e("n", "Consonant", "Tongue on the ridge behind the teeth; hum through your nose.",
            "Lưỡi chạm nướu sau răng; phát âm xuyên qua mũi."),
        e("ŋ", "Consonant", "Back of tongue against the soft palate; nasal hum, as the ending of “sing” — never an audible /g/.",
            "Phần sau lưỡi áp vòm mềm, âm đi qua mũi — như cuối “sing”, không nghe rõ /g/."),
        e("l", "Consonant", "Touch the ridge behind your upper teeth with the tongue tip; let air pass around the sides.",
            "Đầu lưỡi chạm nướu trên; cho khí thoát quanh hai bên lưỡi."),
        e("r", "Consonant", "Curl the tip of your tongue back without touching the roof; lips slightly rounded.",
            "Cuộn đầu lưỡi hơi ra sau nhưng không chạm vòm; môi hơi tròn."),
        e("w", "Consonant", "Round your lips tightly as for “oo”, then glide quickly into the vowel.",
            "Chúm môi chặt như “u”, rồi trượt nhanh vào nguyên âm sau đó."),
        e("j", "Consonant", "The English “y”: tongue high-front like /iː/, glide instantly into the vowel.",
            "Âm “y” tiếng Anh: lưỡi cao về trước như /iː/, trượt ngay vào nguyên âm."),
        e("kw", "Cluster", "Two sounds glued together: release /k/ straight into rounded /w/ — “qu” in quick.",
            "Hai âm dính nhau: bật /k/ rồi chuyển ngay sang /w/ tròn môi — “qu” trong quick."),
        e("ŋk", "Cluster", "Nasal /ŋ/ flowing straight into /k/: think “si-nk”, keep the back of the tongue planted.",
            "/ŋ/ mũi nối thẳng vào /k/: nghĩ “si-nk”, giữ nguyên phần sau lưỡi.")
    )

    val all: List<PhonemeEntry> get() = vowels + diphthongs + consonants

    fun bySymbol(symbol: String): PhonemeEntry? = all.firstOrNull { it.symbol == symbol }
}

/**
 * Orders drill words from easy to hard:
 *  1. fewer syllables first,
 *  2. within a level, the target sound moves from word-initial to medial to final.
 */
object DrillSorter {

    // Syllable nuclei (length marks stripped); diphthong tokens count once each.
    private val nuclei = setOf(
        "i", "ɪ", "e", "ɛ", "æ", "a", "ɑ", "ɒ", "ɔ", "o", "ʊ", "u",
        "ə", "ɚ", "ɜ", "ʌ", "ᵻ", "ɐ",
        "eɪ", "aɪ", "ɔɪ", "aʊ", "oʊ"
    )

    private fun isNucleus(token: String): Boolean =
        token.removeSuffix("ː") in nuclei

    fun syllableCount(phones: List<String>): Int = phones.count { isNucleus(it) }

    /** 0 = sound starts the word, 1 = somewhere in the middle, 2 = ends the word. */
    fun positionRank(phones: List<String>, targets: List<String>, norm: (String) -> String): Int {
        val t = targets.map(norm)
        val idx = phones.indexOfFirst { p -> t.any { norm(p) == it } }
        if (idx <= 0) return 0
        return if (idx >= phones.size - 1) 2 else 1
    }
}
