package com.metronomelist.tuner.app.engine

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gerencia AudioRecord + YIN em background.
 * Chama onResult() na thread principal via callback.
 */
class TunerEngine(
    private val onResult: (NoteResult?) -> Unit
) {
    companion object {
        const val SAMPLE_RATE = 44100
        const val BUFFER_SIZE = 4096
        const val SILENCE_MS  = 400L
    }

    private val yin = YinPitchDetector(sampleRate = SAMPLE_RATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) return

        val bufSize = maxOf(minBuf, BUFFER_SIZE * 4)

        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT,
                bufSize
            )
        } catch (e: Exception) {
            return
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return
        }

        audioRecord = record
        record.startRecording()

        val floatBuf = FloatArray(BUFFER_SIZE)
        var lastDetectedMs = System.currentTimeMillis()

        job = scope.launch {
            while (isActive) {
                val read = audioRecord?.read(
                    floatBuf, 0, BUFFER_SIZE, AudioRecord.READ_BLOCKING
                ) ?: break

                if (read <= 0) continue

                val freq = yin.detect(floatBuf)
                val now  = System.currentTimeMillis()

                if (freq != null && freq > 50f && freq < 1500f) {
                    lastDetectedMs = now
                    val result = freqToNote(freq, pitchRef)
                    withContext(Dispatchers.Main) { onResult(result) }
                } else {
                    if (now - lastDetectedMs > SILENCE_MS) {
                        withContext(Dispatchers.Main) { onResult(null) }
                    }
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        try {
            audioRecord?.stop()
        } catch (_: Exception) {}
        audioRecord?.release()
        audioRecord = null
    }

    val isRunning get() = job?.isActive == true
}