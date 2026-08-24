package com.openpronounce.android.ui

/**
 * Short articulation hints for the phones learners most often get wrong, shown under
 * each row of the "Sound / You said" breakdown (ELSA-style coaching copy).
 */
object PhoneTips {

    private val tips = mapOf(
        "θ" to "Place your tongue between your teeth and blow air out.",
        "ð" to "Same as /θ/, but add voice — the tongue buzzes lightly.",
        "ɹ" to "Curl the tip of your tongue back without touching the roof.",
        "r" to "Curl the tip of your tongue back without touching the roof.",
        "l" to "Touch the ridge behind your upper teeth with your tongue tip.",
        "v" to "Rest your upper teeth on your lower lip and buzz.",
        "w" to "Round your lips tightly, then glide open — like “oo”.",
        "ŋ" to "Press the back of your tongue against the soft palate and hum.",
        "æ" to "Open wide and spread your lips, like an exaggerated “a”.",
        "ɪ" to "Relaxed short vowel — halfway between /iː/ and /ə/.",
        "iː" to "Stretch your lips into a smile and hold the sound long.",
        "z" to "Like /s/, but voiced — feel the buzz in your throat.",
        "ʒ" to "Like /ʃ/ (“sh”), but voiced — same buzz.",
        "dʒ" to "Start like /d/, release into /ʒ/: “j” as in jump.",
        "tʃ" to "Start like /t/, release into /ʃ/: “ch” as in church.",
        "h" to "Breathe out warmly, like fogging a mirror.",
        "f" to "Upper teeth on lower lip; blow air without voice.",
        "p" to "Close both lips fully, build pressure, pop them open."
    )

    /** Tip for a phone token, ignoring length marks (“ɑː” finds the “ɑ” advice). */
    fun tipFor(phone: String): String? =
        tips[phone] ?: tips[phone.trimEnd('ː', 'ˑ', '̆')]

    // ---- Vietnamese versions (same keys) ------------------------------------

    private val tipsVi = mapOf(
        "θ" to "Đưa đầu lưỡi ra giữa hàm răng rồi thổi khí, không rung thanh.",
        "ð" to "Lưỡi giữa răng như /θ/ nhưng thêm thanh — lưỡi sẽ nhấp nháy.",
        "ɹ" to "Cuộn đầu lưỡi hơi ra sau nhưng không chạm vòm miệng.",
        "r" to "Cuộn đầu lưỡi hơi ra sau nhưng không chạm vòm miệng.",
        "l" to "Đầu lưỡi chạm nướu trên; cho khí thoát quanh hai bên lưỡi.",
        "v" to "Răng trên đặt lên môi dưới và rung thanh.",
        "w" to "Chúm môi chặt như “u”, rồi trượt nhanh mở ra.",
        "ŋ" to "Phần sau lưỡi áp vòm mềm, phát âm qua mũi.",
        "æ" to "Mở hàm rộng, kéo môi sang hai bên như “a” dài.",
        "ɪ" to "Nguyên âm ngắn thư giãn — ở giữa /iː/ và /ə/.",
        "iː" to "Kéo môi thành nụ cười, giữ âm dài.",
        "z" to "Giống /s/ nhưng rung thanh — cảm nhận rung trong họng.",
        "ʒ" to "Giống /ʃ/ (“sh”) nhưng rung thanh.",
        "dʒ" to "Bắt đầu như /d/, thoát ra thành /ʒ/: “j” trong jump.",
        "tʃ" to "Bắt đầu như /t/, thoát ra thành /ʃ/: “ch” trong church.",
        "h" to "Thở ra nhẹ như làm mờ gương bằng hơi.",
        "f" to "Răng trên đặt lên môi dưới, thổi khí không rung thanh.",
        "p" to "Khép chặt hai môi, tạo áp suất rồi bật mở."
    )

    fun tipFor(phone: String, lang: String): String? {
        val key = phone.trimEnd('ː', 'ˑ', '̆')
        return if (lang == "vi") tipsVi[key] ?: tipsVi[phone] ?: tipFor(phone)
        else tipFor(phone)
    }
}
