package com.openpronounce.android

import android.app.Application
import com.openpronounce.android.ml.PronunciationPipeline

class OpenPronounceApp : Application() {

    lateinit var pipeline: PronunciationPipeline
        private set

    override fun onCreate() {
        super.onCreate()
        pipeline = PronunciationPipeline(this)
    }
}
