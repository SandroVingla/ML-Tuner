package com.metronomelist.tuner.app.engine

import kotlin.math.sqrt

/**
 * YIN pitch detection algorithm
 * de Cheveigné & Kawahara (2002)
 * Portado do JavaScript do ML-T1 web tuner
 */
class YinPitchDetector(
    private val sampleRate: Int = 44100,
    private val threshold: Float = 0.10f,
    private val minHz: Float = 50f,
    private val maxHz: Float = 1400f
) {
    private val bufferSize = 4096
    private val half = bufferSize / 2
    private val d   = FloatArray(half)
    private val cm  = FloatArray(half)

    /**
     * Retorna a frequência detectada em Hz, ou null se sinal fraco/incerto.
     */
    fun detect(buf: FloatArray): Float? {
        if (rms(buf) < 0.008f) return null

        // Passo 1 — diferença
        d.fill(0f)
        for (tau in 1 until half) {
            for (i in 0 until half) {
                val delta = buf[i] - buf[i + tau]
                d[tau] += delta * delta
            }
        }

        // Passo 2 — diferença normalizada cumulativa
        cm[0] = 1f
        var sum = 0f
        for (tau in 1 until half) {
            sum += d[tau]
            cm[tau] = if (sum == 0f) 0f else d[tau] * tau / sum
        }

        val tauMin = (sampleRate / maxHz).toInt()
        val tauMax = (sampleRate / minHz).toInt()

        // Passo 3 — primeiro mínimo abaixo do threshold
        var tau = tauMin
        while (tau < tauMax) {
            if (cm[tau] < threshold) {
                while (tau + 1 < tauMax && cm[tau + 1] < cm[tau]) tau++
                // Interpolação parabólica sub-sample
                val prev = if (tau > 0) cm[tau - 1] else cm[tau]
                val cur  = cm[tau]
                val next = if (tau + 1 < half) cm[tau + 1] else cm[tau]
                val denom = 2f * (2f * cur - prev - next)
                val refinedTau = if (denom != 0f) tau + (next - prev) / denom else tau.toFloat()
                return if (refinedTau > 0) sampleRate / refinedTau else null
            }
            tau++
        }

        // Fallback — mínimo absoluto na faixa
        var minVal = Float.MAX_VALUE
        var bestTau = tauMin
        for (t in tauMin until tauMax) {
            if (cm[t] < minVal) { minVal = cm[t]; bestTau = t }
        }
        return if (minVal > 0.35f) null else sampleRate.toFloat() / bestTau
    }

    private fun rms(buf: FloatArray): Float {
        var sum = 0f
        for (v in buf) sum += v * v
        return sqrt(sum / buf.size)
    }
}