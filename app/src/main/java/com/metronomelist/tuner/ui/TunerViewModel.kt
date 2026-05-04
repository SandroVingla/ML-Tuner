package com.metronomelist.tuner.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metronomelist.tuner.app.engine.NoteResult
import com.metronomelist.tuner.app.engine.TunerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PowerMode { OFF, L, H }

data class TunerUiState(
    val isRunning:  Boolean   = false,
    val isPowered:  Boolean   = false,
    val powerMode:  PowerMode = PowerMode.OFF,
    val note:       String?   = null,
    val cents:      Float     = -20f,
    val frequency:  Float     = 0f,
    val pitchRef:   Int       = 440,
    val statusText: String    = "Deslize para iniciar",
    val showHzMenu: Boolean   = false
)

val PITCH_PRESETS = listOf(415, 430, 432, 435, 440, 444)

// Notas válidas no modo L (guitarra/violão)
private val GUITAR_NOTES = setOf("E", "A", "D", "G", "B")

class TunerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TunerUiState())
    val uiState: StateFlow<TunerUiState> = _uiState.asStateFlow()

    // Engine criada uma única vez — callback na main thread
    private val engine = TunerEngine { result -> onEngineResult(result) }

    init {
        // Garante que o engine já inicia com a referência correta
        engine.pitchRef = _uiState.value.pitchRef
    }

    // ── Power / Modo ─────────────────────────────────────────────────────

    fun setPowerMode(mode: PowerMode) {
        when (mode) {
            PowerMode.OFF -> {
                update {
                    copy(
                        powerMode  = PowerMode.OFF,
                        isPowered  = false,
                        note       = null,
                        cents      = -20f,
                        statusText = "Desligado"
                    )
                }
                stopEngine()
            }
            PowerMode.L -> {
                update {
                    copy(
                        powerMode  = PowerMode.L,
                        isPowered  = true,
                        statusText = "Modo L — E  A  D  G  B  E"
                    )
                }
                startEngine()
            }
            PowerMode.H -> {
                update {
                    copy(
                        powerMode  = PowerMode.H,
                        isPowered  = true,
                        statusText = "Modo H — Cromático"
                    )
                }
                startEngine()
            }
        }
    }

    // ── Pitch ────────────────────────────────────────────────────────────

    fun setHz(hz: Int) {
        engine.pitchRef = hz                       // atualiza engine imediatamente
        update { copy(pitchRef = hz, statusText = "Referência: $hz Hz") }
    }

    fun cyclePitch() {
        val idx  = PITCH_PRESETS.indexOf(_uiState.value.pitchRef)
        val next = PITCH_PRESETS[(idx + 1) % PITCH_PRESETS.size]
        setHz(next)
    }

    fun pitchUp() {
        val idx = PITCH_PRESETS.indexOf(_uiState.value.pitchRef)
        if (idx < PITCH_PRESETS.size - 1) setHz(PITCH_PRESETS[idx + 1])
    }

    fun pitchDown() {
        val idx = PITCH_PRESETS.indexOf(_uiState.value.pitchRef)
        if (idx > 0) setHz(PITCH_PRESETS[idx - 1])
    }

    fun toggleHzMenu() {
        update { copy(showHzMenu = !showHzMenu) }
    }

    // ── Engine ───────────────────────────────────────────────────────────

    private fun startEngine() {
        engine.pitchRef = _uiState.value.pitchRef
        engine.start()
        update { copy(isRunning = true, statusText = "Aguardando sinal...") }
    }

    private fun stopEngine() {
        engine.stop()
        update { copy(isRunning = false, cents = -20f, note = null) }
    }

    private fun onEngineResult(result: NoteResult?) {
        if (result == null) {
            update { copy(cents = -20f, note = null, statusText = "Aguardando sinal...") }
            return
        }

        // Modo L — ignora notas que não são cordas de guitarra/violão
        val mode = _uiState.value.powerMode
        if (mode == PowerMode.L && result.name !in GUITAR_NOTES) return

        update {
            copy(
                note       = result.name,
                cents      = result.cents,
                frequency  = result.frequency,
                statusText = buildStatusText(result)
            )
        }
    }

    // ── Lifecycle ────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        engine.stop()
    }

    // ── Utils ────────────────────────────────────────────────────────────

    /** Atualiza o estado de forma segura na viewModelScope */
    private fun update(block: TunerUiState.() -> TunerUiState) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.block()
        }
    }

    private fun buildStatusText(r: NoteResult): String {
        val sign = if (r.cents >= 0) "+" else ""
        return "${r.name} — ${"%.1f".format(r.frequency)} Hz  $sign${"%.0f".format(r.cents)} cents"
    }
}