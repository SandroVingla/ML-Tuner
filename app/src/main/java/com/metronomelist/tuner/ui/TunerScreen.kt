package com.metronomelist.tuner.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset

private val PanelBg  = Color(0xFFCEC9B8)
private val StripBg  = Color(0xFFBDB8AB)
private val LedOff   = Color(0xFF3A0A00)
private val LedOn    = Color(0xFFCC1100)
private val SharpOff = Color(0xFF2A1C06)
private val BattOff  = Color(0xFF2A1C06)
private val BattOn   = Color(0xFF30CC10)
private val BtnBlue  = Color(0xFF2596BE)
private val BtnDark  = Color(0xFF0A3A52)
private val TextDark = Color(0xFF1A1206)
private val TextMid  = Color(0xFF4A3A18)

private val NOTE_NAMES = listOf("C","D","E","F","G","A","B")

@Composable
fun TunerScreen(vm: TunerViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A))
            .padding(8.dp)
    ) {
        // Chassis principal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
                .border(2.dp, Color(0xFF0E0E0E), RoundedCornerShape(8.dp))
        ) {
            // Painel esquerdo — Display
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(362f / 304f)  // ← proporção exata do SVG web
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(2.dp, Color(0xFF0E0E0E), RoundedCornerShape(6.dp))
            ) {
                TunerDisplay(cents = state.cents, modifier = Modifier.fillMaxSize())
            }

            // Painel direito
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.55f)
                    .background(PanelBg)
            ) {
                TopStrip(state, vm)
                HorizontalDivider(color = Color(0xFF9A9588), thickness = 1.dp)
                NoteStrip(state)
                HorizontalDivider(color = Color(0xFF9A9080), thickness = 1.dp)
                MainArea(state, vm, modifier = Modifier.weight(1f))
            }
        }

        // Status
        Text(
            text      = state.statusText,
            color     = Color(0xFF888888),
            fontSize  = 11.sp,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center
        )

        // Menu Hz
        if (state.showHzMenu) {
            HzMenu(state, vm)
        }
    }
}

@Composable
private fun TopStrip(state: TunerUiState, vm: TunerViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StripBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))

        Text("BATT.", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(Modifier.width(5.dp))
        val battColor by animateColorAsState(
            if (state.isPowered) BattOn else BattOff, tween(300), label = "batt"
        )
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(battColor)
                .border(1.dp, Color(0xFF5A4020), CircleShape)
        )

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFFA89868))
                .border(1.dp, Color(0xFF7A6840), RoundedCornerShape(3.dp))
                .clickable { vm.toggleHzMenu() }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("REF", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("${state.pitchRef}", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A0E04))
            }
        }
    }
}

@Composable
private fun NoteStrip(state: TunerUiState) {
    val activeNote = state.note?.replace("#", "")
    val isSharp    = state.note?.contains("#") == true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment     = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Column(
            modifier = Modifier.width(44.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(20.dp))
            Text("CHROMATIC", fontSize = 6.sp, color = TextMid)
        }

        NOTE_NAMES.forEach { n ->
            NoteWithLed(note = n, active = n == activeNote)
        }

        // Sharp
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val sharpColor by animateColorAsState(
                if (isSharp) LedOn else SharpOff, tween(80), label = "sharp"
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(sharpColor)
                    .border(1.dp, Color(0xFF5A4020), CircleShape)
            )
            Spacer(Modifier.height(4.dp))
            Text("♯", fontSize = 18.sp, color = TextDark)
        }
    }
}

@Composable
private fun NoteWithLed(note: String, active: Boolean) {
    val ledColor by animateColorAsState(
        if (active) LedOn else LedOff, tween(80), label = "led_$note"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(26.dp)
                .height(15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ledColor)
                .border(0.8.dp, Color(0xFF5A2A10), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(3.dp))
        Text(note, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

@Composable
private fun MainArea(state: TunerUiState, vm: TunerViewModel, modifier: Modifier) {
    Row(
        modifier          = modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier            = Modifier.fillMaxHeight()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                TunerButton("PITCH", onClick = vm::cyclePitch)
                TunerButton("DOWN",  onClick = vm::pitchDown)
                TunerButton("UP",    onClick = vm::pitchUp)
            }
            Spacer(Modifier.height(8.dp))
            PowerSlider(powered = state.isPowered, onChanged = vm::setPowered)
        }

        Spacer(Modifier.weight(1f))

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
            modifier            = Modifier.fillMaxHeight().padding(bottom = 4.dp)
        ) {
            Text("CHROMATIC TUNER", fontSize = 7.sp, color = TextMid, letterSpacing = 1.sp)
            Text("ML-T1", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextDark, letterSpacing = (-0.5).sp)
            Text("METRONOME LIST", fontSize = 7.sp, fontWeight = FontWeight.Bold, color = TextDark, letterSpacing = 2.sp)
            Text("Digital Processing", fontSize = 6.sp, color = TextMid)
        }
    }
}


@Composable
private fun TunerButton(label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = TextDark, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color(0xFF70D0EF), Color(0xFF2596BE), Color(0xFF0F5A7A))))
                .border(2.dp, BtnDark, CircleShape)
                .clickable(onClick = onClick)
        )
    }
}


@Composable
private fun PowerSlider(powered: Boolean, onChanged: (Boolean) -> Unit) {
    Column(horizontalAlignment = Alignment.Start) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("OFF", fontSize = 7.sp, color = TextMid)
            Canvas(modifier = Modifier.size(width = 28.dp, height = 10.dp)) {
                val y   = size.height / 2f
                val yUp = size.height * 0.15f
                drawLine(Color(0xFF3A2C10), Offset(0f,  y),   Offset(5f,  y),   1.2f)
                drawLine(Color(0xFF3A2C10), Offset(5f,  y),   Offset(5f,  yUp), 1.2f)
                drawLine(Color(0xFF3A2C10), Offset(5f,  yUp), Offset(12f, yUp), 1.2f)
                drawLine(Color(0xFF3A2C10), Offset(12f, yUp), Offset(12f, y),   1.2f)
                drawLine(Color(0xFF3A2C10), Offset(12f, y),   Offset(18f, y),   1.2f)
                drawLine(Color(0xFF3A2C10), Offset(18f, y),   Offset(18f, yUp), 1.2f)
                drawLine(Color(0xFF3A2C10), Offset(18f, yUp), Offset(28f, yUp), 1.2f)
            }
            Text("L  H  CHROMATIC", fontSize = 7.sp, color = TextMid)
        }

        Spacer(Modifier.height(3.dp))

        // Track com 3 posições: OFF=0, L=1, H=2
        // Usamos Int para representar a posição
        val position = if (!powered) 0 else 1  // simplificado: OFF ou ON

        Box(
            modifier = Modifier
                .width(130.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFB8B3A4))
                .border(2.dp, Color(0xFF888070), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            // Clique em cada terço do slider
            Row(modifier = Modifier.fillMaxSize()) {
                // Terço OFF
                Box(modifier = Modifier.weight(1f).fillMaxHeight()
                    .clickable { onChanged(false) })
                // Terço L
                Box(modifier = Modifier.weight(1f).fillMaxHeight()
                    .clickable { onChanged(true) })
                // Terço H/CROM
                Box(modifier = Modifier.weight(1f).fillMaxHeight()
                    .clickable { onChanged(true) })
            }

            // Thumb
            val thumbOffset = when {
                !powered -> 2.dp
                else     -> 56.dp
            }
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(50.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF60C8E0), Color(0xFF2090B8), Color(0xFF0C4A68))
                        )
                    )
                    .border(1.dp, BtnDark, RoundedCornerShape(11.dp))
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCC7000))
                        .align(Alignment.Center)
                )
            }
        }

        Row(
            modifier              = Modifier.width(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("OFF", fontSize = 6.sp, color = TextMid)
            Text("L",   fontSize = 6.sp, color = TextMid)
            Text("H",   fontSize = 6.sp, color = TextMid)
        }
    }
}
@Composable
private fun HzMenu(state: TunerUiState, vm: TunerViewModel) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        color          = Color(0xFFC8C0A0),
        shape          = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                "FREQUÊNCIA DE REFERÊNCIA (A4)",
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF2A1E08),
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PITCH_PRESETS.forEach { hz ->
                    val isActive = hz == state.pitchRef
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isActive) Color(0xFF2A1E08) else Color(0xFFB8A870))
                            .border(1.5.dp, Color(0xFF8A7040), RoundedCornerShape(4.dp))
                            .clickable { vm.setHz(hz) }
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "$hz Hz",
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color      = if (isActive) Color(0xFFE8D890) else Color(0xFF1A1008)
                            )
                            Text(
                                hzLabel(hz),
                                fontSize = 7.sp,
                                color    = if (isActive) Color(0xFFA09050) else Color(0xFF5A4820)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun hzLabel(hz: Int) = when (hz) {
    415  -> "Barroco"
    430  -> "Histórico"
    432  -> "Natural"
    435  -> "Antigo"
    440  -> "Padrão ISO"
    444  -> "Moderno"
    else -> ""
}