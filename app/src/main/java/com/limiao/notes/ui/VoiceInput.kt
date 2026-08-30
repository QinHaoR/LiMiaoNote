package com.limiao.notes.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * 系统自带语音识别（SpeechRecognizer）封装，0 成本跑通「语音 → 文字」。
 * 注意：必须从主线程调用 create / start / cancel。
 */
class VoiceRecorder(
    private val context: Context,
    private val onText: (String) -> Unit,    // 识别到文字（主线程回调）
    private val onError: (String) -> Unit,   // 出错提示，中文可直接 Toast（主线程回调）
) {
    private val main = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var started = false

    val isListening: Boolean get() = started

    fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start() {
        stopInternal()
        if (!available()) {
            onError("系统没有可用的语音识别服务")
            return
        }
        started = true
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        if (sr == null) {
            started = false
            onError("无法启动语音识别服务")
            return
        }
        recognizer = sr
        sr.setRecognitionListener(listener)
        sr.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
        )
    }

    fun cancel() {
        recognizer?.cancel()
        stopInternal()
    }

    private fun stopInternal() {
        started = false
        recognizer?.destroy()
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
            main.post {
                if (started) stopInternal()
                if (text != null) onText(text) else onError("没听清，请再说一次")
            }
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没听清，请再说一次"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "缺少录音权限"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络不可用，语音识别失败"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别服务忙，请稍后再试"
                SpeechRecognizer.ERROR_CLIENT -> "语音服务不可用"
                else -> "识别失败（错误码 $error）"
            }
            main.post {
                if (started) stopInternal()
                onError(msg)
            }
        }
    }
}
