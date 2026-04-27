package com.metronomelist.tuner.engine

import kotlin.math.log2
import kotlin.math.round

data class NoteResult(
    val name: String,      // ex: "A", "C#"
    val cents: Float,      // -50 a +50
    val frequency: Float   // Hz detectado
)

private val NOTE_NAMES = arrayOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")

/**
 * Converte frequência detectada → nota + desvio em cents,
 * usando pitchRef como referência para A4.
 */
fun freqToNote(freq: Float, pitchRef: Int = 440): NoteResult {
    // MIDI number relativo ao pitchRef (A4 = 69)
    val midi     = 12.0 * log2(freq.toDouble() / pitchRef) + 69.0
    val rounded  = round(midi).toInt()
    val cents    = ((midi - rounded) * 100).toFloat()
    val name     = NOTE_NAMES[((rounded % 12) + 12) % 12]
    return NoteResult(name = name, cents = cents, frequency = freq)
}
