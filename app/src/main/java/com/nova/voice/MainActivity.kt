package com.nova.voice

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : Activity(), RecognitionListener {
    private lateinit var nova: NovaVoiceVisualizerView
    private var recognizer: SpeechRecognizer? = null
    private val micRequest = 7

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setStatusBarColor(android.graphics.Color.rgb(3, 5, 12))
        window.setNavigationBarColor(android.graphics.Color.rgb(3, 5, 12))
        nova = NovaVoiceVisualizerView(this) { onPrimaryTap() }
        setContentView(nova)
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { it.setRecognitionListener(this) }
        }
    }

    private fun onPrimaryTap() {
        when (nova.state) {
            NovaState.IDLE -> nova.transitionTo(NovaState.READY)
            NovaState.READY, NovaState.RECOGNIZED, NovaState.ERROR -> startListening()
            else -> if (nova.state == NovaState.LISTENING) recognizer?.stopListening()
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), micRequest); return
        }
        val r = recognizer ?: run { nova.transitionTo(NovaState.ERROR); return }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "te-IN")
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayOf("te-IN", "en-IN", Locale.getDefault().toLanguageTag()))
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        nova.transitionTo(NovaState.LISTENING)
        r.startListening(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == micRequest && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) startListening()
        else nova.transitionTo(NovaState.ERROR)
    }
    override fun onReadyForSpeech(params: Bundle?) { nova.setRms(0f) }
    override fun onBeginningOfSpeech() { nova.setRms(1f) }
    override fun onRmsChanged(rmsdB: Float) { nova.setRms(rmsdB) }
    override fun onEndOfSpeech() { nova.transitionTo(NovaState.PROCESSING) }
    override fun onResults(results: Bundle?) {
        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
        if (text.isNullOrBlank()) nova.transitionTo(NovaState.ERROR) else { nova.recognizedText = text; nova.transitionTo(NovaState.RECOGNIZED) }
    }
    override fun onError(error: Int) { nova.transitionTo(NovaState.ERROR) }
    override fun onPartialResults(partialResults: Bundle?) { partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { nova.recognizedText = it } }
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEvent(eventType: Int, params: Bundle?) = Unit
    override fun onDestroy() { recognizer?.destroy(); super.onDestroy() }
}
