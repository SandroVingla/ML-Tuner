package com.metronomelist.tuner.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metronomelist.tuner.engine.NoteResult
import com.metronomelist.tuner.engine.TunerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


enum class PowerMode { OFF, L, H }

data class TunerUiState(
    val isRunning:   Boolean    = false,
    val isPowered:   Boolean    = false,  // ← começa desligado
    val powerMode:   PowerMode  = PowerMode.OFF,  // ← começa em OFF
    val note:        String?    = null,
    val cents:       Float      = -20f,
    val frequency:   Float      = 0f,
    val pitchRef:    Int        = 440,
    val statusText:  String     = "Desligue e ligue para iniciar",
    val showHzMenu:  Boolean    = false
)

val PITCH_PRESETS = listOf(415, 430, 432, 435, 440, 444)

class TunerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    private val engine = TunerEngine { result -> onEngineResult(result) }

    init {
        // ← não inicia automaticamente — espera o usuário ligar o slider
        setHz(440)
    }

    fun setPowerMode(mode: PowerMode) {
        _uiState.value = _uiState.value.copy(powerMode = mode)
        when (mode) {
            PowerMode.OFF -> {
                _uiState.value = _uiState.value.copy(
                    isPowered  = false,
                    note       = null,
                    cents      = -20f,
                    statusText = "Desligado"
                )
                stopTuner()
            }
            PowerMode.L -> {
                _uiState.value = _uiState.value.copy(
                    isPowered  = true,
                    statusText = "Modo L — cordas E A D G B E"
                )
                startTuner()
            }
            PowerMode.H -> {
                _uiState.value = _uiState.value.copy(
                    isPowered  = true,
                    statusText = "Modo H — cromático"
                )
                startTuner()
            }
        }
    }

    fun setPowered(on: Boolean) {
        _uiState.value = _uiState.value.copy(
            isPowered  = on,
            cents      = if (!on) -20f else _uiState.value.cents,
            note       = if (!on) null else _uiState.value.note,
            statusText = if (on) "Aguardando sinal..." else "Desligado"
        )
        if (on) startTuner() else stopTuner()
    }

    fun toggleMic() {
        if (engine.isRunning) stopTuner() else startTuner()
    }

    private fun startTuner() {
        engine.pitchRef = _uiState.value.pitchRef
        engine.start()
        _uiState.value = _uiState.value.copy(
            isRunning  = true,
            isPowered  = true,
            statusText = "Aguardando sinal..."
        )
    }

    private fun stopTuner() {
        engine.stop()
        _uiState.value = _uiState.value.copy(
            isRunning  = false,
            cents      = -20f,
            note       = null,
            statusText = "Microfone desativado"
        )
    }

    private fun onEngineResult(result: NoteResult?) {
        val mode = _uiState.value.powerMode
        if (result == null) {
            _uiState.value = _uiState.value.copy(
                cents      = -20f,
                note       = null,
                statusText = "Aguardando sinal..."
            )
            return
        }
        // Modo L — só mostra notas de corda de guitarra/baixo
        val guitarNotes = setOf("E", "A", "D", "G", "B")
        if (mode == PowerMode.L && result.name !in guitarNotes) return

        _uiState.value = _uiState.value.copy(
            note       = result.name,
            cents      = result.cents,
            frequency  = result.frequency,
            statusText = "${result.name} — ${"%.1f".format(result.frequency)} Hz  " +
                    "${if (result.cents >= 0) "+" else ""}${"%.0f".format(result.cents)} cents"
        )
    }
    var pitchRef: Int = 440
    fun cyclePitch() {
        val idx = PITCH_PRESETS.indexOf(pitchRef)
        val next = PITCH_PRESETS[(idx + 1) % PITCH_PRESETS.size]
        setHz(next)
    }

    fun pitchUp() {
        val idx = PITCH_PRESETS.indexOf(pitchRef)
        if (idx < PITCH_PRESETS.size - 1) {
            setHz(PITCH_PRESETS[idx + 1])
        }
    }

    fun pitchDown() {
        val idx = PITCH_PRESETS.indexOf(pitchRef)
        if (idx > 0) {
            setHz(PITCH_PRESETS[idx - 1])
        }
    }

    fun setHz(hz: Int) {
        pitchRef = hz
        engine.pitchRef = hz
        _uiState.value = _uiState.value.copy(
            pitchRef   = hz,
            statusText = "Referência: $hz Hz"
        )
    }

    fun toggleHzMenu() {
        _uiState.value = _uiState.value.copy(showHzMenu = !_uiState.value.showHzMenu)
    }

    override fun onCleared() {
        super.onCleared()
        engine.stop()
    }
}