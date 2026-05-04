package com.metronomelist.tuner.app.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metronomelist.tuner.app.TunerActivity
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                SplashScreen {
                    // Garante navegação mesmo se Activity estiver sendo destruída
                    if (!isFinishing && !isDestroyed) {
                        startActivity(Intent(this, TunerActivity::class.java))
                        finish()
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val needleAngle = remember { Animatable(-28f) }
    val textAlpha   = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        delay(250)
        needleAngle.animateTo(
            targetValue   = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness    = Spring.StiffnessLow
            )
        )
        textAlpha.animateTo(
            targetValue   = 1f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
        )
        delay(900)
        onFinished()
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .windowInsetsPadding(WindowInsets.systemBars),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Display âmbar animado
            Canvas(modifier = Modifier.size(width = 224.dp, height = 164.dp)) {
                val w    = size.width
                val h    = size.height
                val px   = w * 0.5f
                val py   = h * 0.90f
                val maxR = py - h * 0.04f

                // Moldura
                drawRoundRect(
                    color        = Color(0xFF0A0500),
                    size         = size,
                    cornerRadius = CornerRadius(6f)
                )

                // Fundo âmbar
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFC85800), Color(0xFF8B3A00), Color(0xFF4A1800)),
                        center = Offset(px, h * 0.68f),
                        radius = h * 0.85f
                    ),
                    topLeft      = Offset(2f, 2f),
                    size         = Size(w - 4f, h - 4f),
                    cornerRadius = CornerRadius(4f)
                )

                // Vinheta
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x00000000), Color(0x88000000)),
                        center = Offset(px, h * 0.5f),
                        radius = h * 0.7f
                    ),
                    topLeft      = Offset(2f, 2f),
                    size         = Size(w - 4f, h - 4f),
                    cornerRadius = CornerRadius(4f)
                )

                // Badge CENT
                drawRect(
                    color   = Color(0xFF1A6A1A),
                    topLeft = Offset(w * 0.735f, h * 0.055f),
                    size    = Size(w * 0.185f, h * 0.100f)
                )

                // Arco tuning guide
                val arcPath = Path().apply {
                    moveTo(px - maxR * 0.78f, py - maxR * 0.25f)
                    quadraticTo(px, py - maxR * 0.88f, px + maxR * 0.78f, py - maxR * 0.25f)
                }
                drawPath(arcPath, color = Color(0xFF2A1000), style = Stroke(width = maxR * 0.008f))

                // Ticks
                listOf(-40f, -25f, -10f, 0f, 10f, 25f, 40f).forEach { cent ->
                    val theta  = Math.toRadians(-90.0 + (cent / 50.0) * 82.0)
                    val outerR = maxR * 0.88f
                    val innerR = if (cent == 0f) maxR * 0.72f else maxR * 0.78f
                    drawLine(
                        color       = if (cent == 0f) Color(0xFF4A2000) else Color(0xFF2A1000),
                        start       = Offset(px + outerR * cos(theta).toFloat(), py + outerR * sin(theta).toFloat()),
                        end         = Offset(px + innerR * cos(theta).toFloat(), py + innerR * sin(theta).toFloat()),
                        strokeWidth = if (cent == 0f) maxR * 0.018f else maxR * 0.011f,
                        cap         = StrokeCap.Round
                    )
                }

                // Pivot
                drawCircle(Color(0xFF0F0800), radius = maxR * 0.042f, center = Offset(px, py))
                drawCircle(Color(0xFF1A0A00), radius = maxR * 0.026f, center = Offset(px, py))
                drawCircle(Color(0xFFBB5500), radius = maxR * 0.013f, center = Offset(px, py))

                // Agulha animada
                val angle   = needleAngle.value
                val rad     = Math.toRadians(90.0 + (angle / 50.0) * 82.0)
                val cosA    = cos(rad).toFloat()
                val sinA    = sin(rad).toFloat()
                val baseR   = maxR * 0.06f
                val tipR    = maxR * 0.88f
                val base    = Offset(px + baseR * cosA, py + baseR * sinA)
                val tip     = Offset(px - tipR  * cosA, py - tipR  * sinA)
                val tw      = maxR * 0.032f
                val perpCos = cos(rad + Math.PI / 2).toFloat()
                val perpSin = sin(rad + Math.PI / 2).toFloat()
                val tipFar  = Offset(px - (tipR + maxR * 0.065f) * cosA, py - (tipR + maxR * 0.065f) * sinA)

                drawLine(Color(0xFFF0E8D0), base, tip, maxR * 0.010f, cap = StrokeCap.Round)
                drawPath(
                    Path().apply {
                        moveTo(tipFar.x, tipFar.y)
                        lineTo(tip.x + tw * perpCos, tip.y + tw * perpSin)
                        lineTo(tip.x - tw * perpCos, tip.y - tw * perpSin)
                        close()
                    },
                    color = Color(0xFFF0E8D0)
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text          = "ML-T1",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Bold,
                color         = Color(0xFF5A4A2A).copy(alpha = textAlpha.value),
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text          = "CHROMATIC TUNER",
                fontSize      = 20.sp,
                fontWeight    = FontWeight.Black,
                color         = Color(0xFFD4A85A).copy(alpha = textAlpha.value),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(130.dp)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color(0xFF5A3A10), Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text          = "BY METRONOME LIST STUDIO",
                fontSize      = 7.sp,
                color         = Color(0xFF3A2A12).copy(alpha = textAlpha.value),
                letterSpacing = 3.sp
            )
        }
    }
}