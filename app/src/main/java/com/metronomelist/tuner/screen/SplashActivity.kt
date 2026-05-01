package com.metronomelist.tuner

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SplashScreen {
                    startActivity(Intent(this, TunerActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {

    // Agulha: parte de -28° e vai para 0° com bounce
    val needleAngle = remember { Animatable(-28f) }

    // Fade-in do texto: começa invisível
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(250)

        // Agulha sweepando até o centro
        needleAngle.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness    = Spring.StiffnessLow
            )
        )

        // Texto aparece após a agulha chegar
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )

        delay(900)
        onFinished()
    }

    // Fundo preto quase puro — textura de borracha implícita
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── DISPLAY ÂMBAR ──────────────────────────────────────────
            Box(contentAlignment = Alignment.Center) {

                // Parafusos nos cantos
                val screwSize = 7.dp
                val screwOffset = 14.dp
                listOf(
                    Alignment.TopStart, Alignment.TopEnd,
                    Alignment.BottomStart, Alignment.BottomEnd
                ).forEach { align ->
                    Box(
                        modifier = Modifier
                            .align(align)
                            .offset(
                                x = if (align == Alignment.TopStart || align == Alignment.BottomStart) (-screwOffset) else screwOffset,
                                y = if (align == Alignment.TopStart || align == Alignment.TopEnd) (-screwOffset) else screwOffset
                            )
                            .size(screwSize)
                            .background(Color(0xFF1A1A1A), shape = androidx.compose.foundation.shape.CircleShape)
                    )
                }

                // Display principal
                Canvas(modifier = Modifier.size(width = 224.dp, height = 164.dp)) {
                    val w  = size.width
                    val h  = size.height
                    val px = w * 0.5f
                    val py = h * 0.90f          // ponto de pivô da agulha
                    val maxR = py - h * 0.04f   // raio máximo dos arcos

                    // --- Borda externa do display (moldura) ---
                    drawRoundRect(
                        color       = Color(0xFF0A0500),
                        size        = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                    )

                    // --- Fundo âmbar com gradiente radial ---
                    drawRoundRect(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFC85800),
                                Color(0xFF8B3A00),
                                Color(0xFF4A1800)
                            ),
                            center = Offset(px, h * 0.68f),
                            radius = h * 0.85f
                        ),
                        topLeft = Offset(2f, 2f),
                        size    = androidx.compose.ui.geometry.Size(w - 4f, h - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                    )

                    // --- Vinheta escura nas bordas ---
                    drawRoundRect(
                        brush = androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(
                                Color(0x00000000),
                                Color(0x88000000)
                            ),
                            center = Offset(px, h * 0.5f),
                            radius = h * 0.7f
                        ),
                        topLeft = Offset(2f, 2f),
                        size    = androidx.compose.ui.geometry.Size(w - 4f, h - 4f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f)
                    )

                    // --- Label INPUT (canto esquerdo) ---
                    // desenhado via drawContext (sem Text em Canvas)
                    // usamos uma linha decorativa no lugar

                    // --- Badge CENT (canto direito) ---
                    drawRect(
                        color   = Color(0xFF1A6A1A),
                        topLeft = Offset(w * 0.735f, h * 0.055f),
                        size    = androidx.compose.ui.geometry.Size(w * 0.185f, h * 0.100f)
                    )

                    // --- Arco guia da escala ---
                    val arcPath = Path().apply {
                        val startX = px - maxR * 0.78f
                        val startY = py - maxR * 0.25f
                        val endX   = px + maxR * 0.78f
                        val endY   = startY
                        val ctrlX  = px
                        val ctrlY  = py - maxR * 0.88f
                        moveTo(startX, startY)
                        quadraticTo(ctrlX, ctrlY, endX, endY)
                    }
                    drawPath(
                        arcPath,
                        color = Color(0xFF2A1000),
                        style = Stroke(width = maxR * 0.008f)
                    )

                    // --- Ticks da escala ---
                    val tickAngles = listOf(-40f, -25f, -10f, 0f, 10f, 25f, 40f)
                    tickAngles.forEach { cent ->
                        val theta = Math.toRadians((-90.0 + (cent / 50.0) * 82.0))
                        val outerR = maxR * 0.88f
                        val innerR = if (cent == 0f) maxR * 0.72f else maxR * 0.78f
                        val outer  = Offset(
                            px + outerR * cos(theta).toFloat(),
                            py + outerR * sin(theta).toFloat()
                        )
                        val inner  = Offset(
                            px + innerR * cos(theta).toFloat(),
                            py + innerR * sin(theta).toFloat()
                        )
                        drawLine(
                            color       = if (cent == 0f) Color(0xFF4A2000) else Color(0xFF2A1000),
                            start       = outer,
                            end         = inner,
                            strokeWidth = if (cent == 0f) maxR * 0.018f else maxR * 0.011f,
                            cap         = StrokeCap.Round
                        )
                    }

                    // --- Pivot ---
                    drawCircle(Color(0xFF0F0800), radius = maxR * 0.042f, center = Offset(px, py))
                    drawCircle(Color(0xFF1A0A00), radius = maxR * 0.026f, center = Offset(px, py))
                    drawCircle(Color(0xFFBB5500), radius = maxR * 0.013f, center = Offset(px, py))

                    // --- Agulha animada ---
                    val angle  = needleAngle.value
                    val rad    = Math.toRadians((90.0 + (angle / 50.0) * 82.0))
                    val cosA   = cos(rad).toFloat()
                    val sinA   = sin(rad).toFloat()

                    // base da agulha levemente abaixo do pivot
                    val baseR  = maxR * 0.06f
                    val tipR   = maxR * 0.88f
                    val base   = Offset(px + baseR * cosA, py + baseR * sinA)
                    val tip    = Offset(px - tipR * cosA, py - tipR * sinA)

                    // haste
                    drawLine(
                        color       = Color(0xFFF0E8D0),
                        start       = base,
                        end         = tip,
                        strokeWidth = maxR * 0.010f,
                        cap         = StrokeCap.Round
                    )

                    // triângulo na ponta
                    val tw       = maxR * 0.032f
                    val perpCos  = cos(rad + Math.PI / 2).toFloat()
                    val perpSin  = sin(rad + Math.PI / 2).toFloat()
                    val tipFar   = Offset(px - (tipR + maxR * 0.065f) * cosA, py - (tipR + maxR * 0.065f) * sinA)
                    val triPath  = Path().apply {
                        moveTo(tipFar.x, tipFar.y)
                        lineTo(tip.x + tw * perpCos, tip.y + tw * perpSin)
                        lineTo(tip.x - tw * perpCos, tip.y - tw * perpSin)
                        close()
                    }
                    drawPath(triPath, color = Color(0xFFF0E8D0))
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── TEXTO: nova hierarquia ─────────────────────────────────

            // Modelo — pequeno, discreto, acima
            Text(
                text          = "ML-T1",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF5A4A2A).copy(alpha = textAlpha.value),
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(4.dp))

            // CHROMATIC TUNER — DESTAQUE PRINCIPAL
            Text(
                text          = "CHROMATIC TUNER",
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Black,
                color         = Color(0xFFD4A85A).copy(alpha = textAlpha.value),
                letterSpacing = 1.5.sp
            )

            // Linha decorativa
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(1.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF5A3A10),
                                Color.Transparent
                            )
                        )
                    )
            )
            Spacer(Modifier.height(8.dp))

            // by Metronome List — mínimo, marca secundária
            Text(
                text          = "BY METRONOME LIST STUDIO",
                fontSize      = 7.sp,
                fontWeight    = FontWeight.Normal,
                color         = Color(0xFF3A2A12).copy(alpha = textAlpha.value),
                letterSpacing = 3.sp
            )
        }
    }
}

/** Converte cents/graus em coordenada polar centrada em (px, py) */
private fun polar(r: Float, cents: Float, px: Float, py: Float): Offset {
    val rad = Math.toRadians((-90.0 + (cents / 50.0) * 82.0))
    return Offset(px + r * cos(rad).toFloat(), py + r * sin(rad).toFloat())
}