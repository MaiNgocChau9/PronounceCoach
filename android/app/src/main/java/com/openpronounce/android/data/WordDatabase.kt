package com.openpronounce.android.data

import android.content.Context

object WordDatabase {

    private val categories = listOf(
        WordCategory(
            id = "basics",
            name = "Basic Words",
            words = listOf(
                WordItem("hello", "h ə l oʊ", "greeting", "Hello, how are you?", "basics", 1),
                WordItem("world", "w ɜːr l d", "noun", "The world is beautiful.", "basics", 1),
                WordItem("thank", "θ æ ŋ k", "verb", "Thank you very much.", "basics", 1),
                WordItem("please", "p l iː z", "adverb", "Please sit down.", "basics", 1),
                WordItem("sorry", "s ɒ r i", "adjective", "I am sorry.", "basics", 1),
                WordItem("yes", "j ɛ s", "adverb", "Yes, I understand.", "basics", 1),
                WordItem("no", "n oʊ", "adverb", "No, thank you.", "basics", 1),
                WordItem("good", "ɡ ʊ d", "adjective", "Good morning!", "basics", 1),
                WordItem("bad", "b æ d", "adjective", "That's bad.", "basics", 1),
                WordItem("big", "b ɪ ɡ", "adjective", "A big house.", "basics", 1)
            )
        ),
        WordCategory(
            id = "food",
            name = "Food & Drinks",
            words = listOf(
                WordItem("water", "w ɔː t ər", "noun", "Can I have water?", "food", 1),
                WordItem("apple", "æ p əl", "noun", "An apple a day.", "food", 1),
                WordItem("bread", "b r ɛ d", "noun", "I like bread.", "food", 1),
                WordItem("coffee", "k ɒ f i", "noun", "Black coffee please.", "food", 1),
                WordItem("sugar", "ʃ ʊ ɡ ər", "noun", "No sugar for me.", "food", 1),
                WordItem("chicken", "tʃ ɪ k ɪ n", "noun", "Grilled chicken.", "food", 2),
                WordItem("vegetable", "v ɛ dʒ t ə b əl", "noun", "Eat your vegetables.", "food", 2),
                WordItem("restaurant", "r ɛ s t r ɒ n t", "noun", "A nice restaurant.", "food", 2),
                WordItem("delicious", "d ɪ l ɪʃ əs", "adjective", "Delicious food!", "food", 2),
                WordItem("breakfast", "b r ɛ k f əst", "noun", "Breakfast is ready.", "food", 2)
            )
        ),
        WordCategory(
            id = "travel",
            name = "Travel",
            words = listOf(
                WordItem("airport", "ɛər p ɔːr t", "noun", "Go to the airport.", "travel", 2),
                WordItem("hotel", "h oʊ t ɛ l", "noun", "Book a hotel.", "travel", 1),
                WordItem("ticket", "t ɪ k ɪ t", "noun", "One ticket please.", "travel", 1),
                WordItem("passport", "p æ s p ɔːr t", "noun", "Where is my passport?", "travel", 2),
                WordItem("luggage", "l ʌ ɡ ɪ dʒ", "noun", "My luggage is heavy.", "travel", 2),
                WordItem("direction", "d ər ɛ k ʃ ən", "noun", "Which direction?", "travel", 2),
                WordItem("station", "s t eɪ ʃ ən", "noun", "Train station.", "travel", 2),
                WordItem("bathroom", "b æ θ r uː m", "noun", "Where is the bathroom?", "travel", 2),
                WordItem("excuse", "ɪk s k j uː z", "verb", "Excuse me.", "travel", 1),
                WordItem("language", "l æ ŋ gw ɪ dʒ", "noun", "What language?", "travel", 2)
            )
        ),
        WordCategory(
            id = "work",
            name = "Work & Business",
            words = listOf(
                WordItem("meeting", "m iː t ɪ ŋ", "noun", "I have a meeting.", "work", 2),
                WordItem("project", "p r ɒ dʒ ɛ k t", "noun", "Start a project.", "work", 2),
                WordItem("computer", "k əm p j uː t ər", "noun", "Use the computer.", "work", 2),
                WordItem("important", "ɪm p ɔːr t ənt", "adjective", "Very important.", "work", 2),
                WordItem("experience", "ɪk s p ɪər i ən s", "noun", "Work experience.", "work", 3),
                WordItem("manager", "m æ n ɪ dʒ ər", "noun", "Talk to the manager.", "work", 2),
                WordItem("schedule", "sk ɛ dʒ uː l", "noun", "Check the schedule.", "work", 2),
                WordItem("salary", "s æ l ər i", "noun", "Monthly salary.", "work", 2),
                WordItem("interview", "ɪn t ər v j uː", "noun", "Job interview.", "work", 2),
                WordItem("colleague", "k ɒ l iː ɡ", "noun", "My colleague.", "work", 2)
            )
        ),
        WordCategory(
            id = "nature",
            name = "Nature & Weather",
            words = listOf(
                WordItem("beautiful", "b j uː t ɪ f əl", "adjective", "Beautiful weather.", "nature", 2),
                WordItem("mountain", "m aʊ n t ɪ n", "noun", "Climb the mountain.", "nature", 2),
                WordItem("ocean", "oʊ ʃ ən", "noun", "The blue ocean.", "nature", 2),
                WordItem("forest", "f ɒ r ɪ s t", "noun", "Walk in the forest.", "nature", 2),
                WordItem("weather", "w ɛ ð ər", "noun", "Nice weather today.", "nature", 1),
                WordItem("rainbow", "r eɪ n b oʊ", "noun", "Look at the rainbow.", "nature", 2),
                WordItem("sunshine", "s ʌ n ʃ aɪ n", "noun", "Morning sunshine.", "nature", 2),
                WordItem("flower", "f l aʊ ər", "noun", "A red flower.", "nature", 1),
                WordItem("garden", "ɡ ɑːr d ən", "noun", "In the garden.", "nature", 1),
                WordItem("river", "r ɪ v ər", "noun", "Swim in the river.", "nature", 1)
            )
        )
    )

    fun getAllCategories(): List<WordCategory> = categories

    fun getCategory(id: String): WordCategory? = categories.find { it.id == id }

    fun getAllWords(): List<WordItem> = categories.flatMap { it.words }

    fun getWordsByDifficulty(difficulty: Int): List<WordItem> {
        return getAllWords().filter { it.difficulty == difficulty }
    }

    fun getRandomWord(difficulty: Int = 1): WordItem {
        val words = getWordsByDifficulty(difficulty)
        return words.random()
    }

    fun searchWords(query: String): List<WordItem> {
        val lowerQuery = query.lowercase()
        return getAllWords().filter {
            it.word.contains(lowerQuery) ||
            it.meaning.contains(lowerQuery) ||
            it.ipa.contains(lowerQuery)
        }
    }
}
