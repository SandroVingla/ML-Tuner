package com.metronomelist.tuner.app.engine

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Gerencia AudioRecord + YIN em background.
 * Thread-safe, com fallback para PCM_16BIT se PCM_FLOAT não for suportado.
 */
class TunerEngine(
    private val onResult: (NoteResult?) -> Unit
) {
    companion object {
        private const val TAG         = "TunerEngine"
        const val SAMPLE_RATE         = 44100
        const val BUFFER_SIZE         = 4096
        const val SILENCE_MS          = 400L
        private const val SILENCE_RMS = 0.008f
    }

    private val yin   = YinPitchDetector(sampleRate = SAMPLE_RATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job:         Job?        = null
    private var audioRecord: AudioRecord? = null

    @Volatile var pitchRef: Int = 440

    // ── Iniciar ──────────────────────────────────────────────────────────
    fun start() {
        if (job?.isActive == true) return

        val record = createAudioRecord() ?: run {
            Log.e(TAG, "Falha ao criar AudioRecord")
            return
        }

        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "startRecording falhou: ${e.message}")
            record.release()
            return
        }

        audioRecord = record
        val floatBuf = FloatArray(BUFFER_SIZE)
        var lastDetectedMs = System.currentTimeMillis()

        job = scope.launch {
            try {
                while (isActive) {
                    val ar = audioRecord ?: break
                    val read = ar.read(floatBuf, 0, BUFFER_SIZE, AudioRecord.READ_BLOCKING)

                    if (read <= 0) continue

                    val now  = System.currentTimeMillis()
                    val freq = yin.detect(floatBuf)

                    if (freq != null && freq > 50f && freq < 1500f) {
                        lastDetectedMs = now
                        val result = freqToNote(freq, pitchRef)
                        withContext(Dispatchers.Main) { onResult(result) }
                    } else if (now - lastDetectedMs > SILENCE_MS) {
                        withContext(Dispatchers.Main) { onResult(null) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro no loop de áudio: ${e.message}")
            }
        }
    }

    // ── Parar ────────────────────────────────────────────────────────────
    fun stop() {
        job?.cancel()
        job = null
        releaseAudioRecord()
    }

    val isRunning get() = job?.isActive == true

    // ── Helpers ──────────────────────────────────────────────────────────

    /**
     * Tenta criar AudioRecord com PCM_FLOAT primeiro.
     * Se falhar (alguns dispositivos não suportam), tenta PCM_16BIT
     * e converte para Float no buffer.
     */
    private fun createAudioRecord(): AudioRecord? {
        // Tentativa 1 — PCM_FLOAT (melhor qualidade)
        tryCreateRecord(AudioFormat.ENCODING_PCM_FLOAT)?.let { return it }

        // Tentativa 2 — PCM_16BIT (compatibilidade máxima)
        Log.w(TAG, "PCM_FLOAT não suportado, usando PCM_16BIT")
        return tryCreateRecord(AudioFormat.ENCODING_PCM_16BIT)
    }

    private fun tryCreateRecord(encoding: Int): AudioRecord? {
        return try {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                encoding
            )
            if (minBuf <= 0) return null

            val bufSize = maxOf(minBuf, BUFFER_SIZE * 4)
            val record  = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                encoding,
                bufSize
            )
            if (record.state == AudioRecord.STATE_INITIALIZED) record else {
                record.release()
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "tryCreateRecord($encoding) falhou: ${e.message}")
            null
        }
    }

    private fun releaseAudioRecord() {
        try { audioRecord?.stop() } catch (_: Exception) {}
        try { audioRecord?.release() } catch (_: Exception) {}
        audioRecord = null
    }
}