package com.pronouncecoach.android

import android.app.Application
import com.pronouncecoach.android.ml.G2p
import com.pronouncecoach.android.ml.PronunciationPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PronounceCoachApp : Application() {

    lateinit var pipeline: PronunciationPipeline
        private set

    override fun onCreate() {
        super.onCreate()
        pipeline = PronunciationPipeline(this)
        CoroutineScope(Dispatchers.IO).launch {
            G2p.loadIfNeeded(this@PronounceCoachApp)
        }
    }
}
