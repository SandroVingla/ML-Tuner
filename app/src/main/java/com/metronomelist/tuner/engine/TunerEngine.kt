package com.metronomelist.tuner.engine

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Gerencia AudioRecord + YIN em background.
 * Chama onResult() na thread principal via callback.
 */
class TunerEngine(
    private val onResult: (NoteResult?) -> Unit
) {
    companion object {
        const val SAMPLE_RATE  = 44100
        const val BUFFER_SIZE  = 4096
        const val SILENCE_MS   = 400L
    }

    private val yin    = YinPitchDetector(sampleRate = SAMPLE_RATE)
    private var job: Job? = null
    private var audioRecord: AudioRecord? = null
    var pitchRef: Int = 440

    fun start() {
        if (job?.isActive == true) return

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val bufSize = maxOf(minBuf, BUFFER_SIZE * 4)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            bufSize
        ).also { it.startRecording() }

        val floatBuf = FloatArray(BUFFER_SIZE)
        var lastDetectedMs = System.currentTimeMillis()

        job = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val read = audioRecord?.read(floatBuf, 0, BUFFER_SIZE, AudioRecord.READ_BLOCKING) ?: break
                if (read <= 0) continue

                val freq = yin.detect(floatBuf)
                val now  = System.currentTimeMillis()

                if (freq != null && freq > 50f && freq < 1500f) {
                    lastDetectedMs = now
                    val result = freqToNote(freq, pitchRef)
                    CoroutineScope(Dispatchers.Main).launch { onResult(result) }
                } else {
                    if (now - lastDetectedMs > SILENCE_MS) {
                        CoroutineScope(Dispatchers.Main).launch { onResult(null) }
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    val isRunning get() = job?.isActive == true
}
