package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class VoiceSpeaker(context: Context, private val onSpeakingStateChanged: (Boolean) -> Unit) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale("hi", "IN")
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.05f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        onSpeakingStateChanged(true)
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }

                    override fun onError(utteranceId: String?) {
                        onSpeakingStateChanged(false)
                    }
                })
            }
        }
    }

    fun speak(text: String) {
        if (!isInitialized || tts == null) return
        stop()
        val utteranceId = "jarvis_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        tts?.stop()
        onSpeakingStateChanged(false)
    }

    fun setPitch(pitch: Float) {
        tts?.setPitch(pitch)
    }

    fun setRate(rate: Float) {
        tts?.setSpeechRate(rate)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
