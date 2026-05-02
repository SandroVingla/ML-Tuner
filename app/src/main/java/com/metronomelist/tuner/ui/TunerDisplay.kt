package com.metronomelist.tuner.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private const val MAX_ANGLE = 82f
private const val SVG_W = 362f
private const val SVG_H = 304f
private const val SVG_PX = 181f
private const val SVG_PY = 298f

private val AMBER_DARK   = Color(0xFF6A2C00)
private val AMBER_MID    = Color(0xFFB84E00)
private val AMBER_LIGHT  = Color(0xFFD86200)
private val TICK_COLOR   = Color(0xFF3A1800)
private val TICK_CENTER  = Color(0xFF5A2800)
private val LABEL_COLOR  = Color(0xFF5A2800)
private val NEEDLE_COLOR = Color(0xFFF5F0E2)
private val CENT_GREEN   = Color(0xFF1A7A1A)
private val CENT_TEXT    = Color(0xFF90EE60)
private val HZ_COLOR     = Color(0xFF2D1000)

@Composable
fun TunerDisplay(
    cents: Float,
    modifier: Modifier = Modifier
) {
    val needleAngle = remember { Animatable(centsToAngle(-20f)) }

    LaunchedEffect(cents) {
        needleAngle.animateTo(
            targetValue = centsToAngle(cents),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                //stiffness    = Spring.StiffnessMediumLow --essa linha deixa a agulha mais rapida
                stiffness    = Spring.StiffnessLow //--essa linha deixa a agulha mais lenta
            )
        )
    }

    val tm = rememberTextMeasurer()

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val scaleX = w / SVG_W
        val scaleY = h / SVG_H
        val scale  = minOf(scaleX, scaleY)

        val px = SVG_PX * scaleX
        val py = SVG_PY * scaleY

        drawRect(AMBER_DARK)
        drawRect(ShaderBrush(RadialGradientShader(
            center     = Offset(w * 0.5f, h * 0.65f),
            radius     = h * 0.9f,
            colors     = listOf(AMBER_LIGHT, AMBER_MID, AMBER_DARK),
            colorStops = listOf(0f, 0.55f, 1f)
        )))

        drawScale(px, py, w, h, scale, tm)
        drawCentBadge(w, h, scale, tm)
        drawNeedle(px, py, scale, needleAngle.value)
    }
}

private fun centsToAngle(cents: Float): Float =
    (cents.coerceIn(-50f, 50f) / 50f) * MAX_ANGLE

private fun polar(rSvg: Float, cents: Float, px: Float, py: Float, scale: Float): Offset {
    val rad = Math.toRadians((-90.0 + (cents / 50.0) * MAX_ANGLE))
    val r   = rSvg * scale
    return Offset(px + r * cos(rad).toFloat(), py + r * sin(rad).toFloat())
}

private fun DrawScope.drawScale(
    px: Float, py: Float, w: Float, h: Float, scale: Float, tm: TextMeasurer
) {
    val rOuter     = 185f
    val rInner     = 167f
    val rMinO      = 181f
    val rMinI      = 173f
    val rLabel     = 143f
    val rGuide     = 148f
    val rGuideCtrl = 124f
    val rTuningLbl = 136f

    // Ticks principais: -30 a +30 com -10 e +10
    val mainTicks  = listOf(-30f, -20f, -10f, 0f, 10f, 20f, 30f)

    // Ticks menores: exatamente no meio entre os principais
    val minorTicks = listOf(-25f, -15f, -5f, 5f, 15f, 25f)

    // Ticks principais
    for (c in mainTicks) {
        val o = polar(rOuter, c, px, py, scale)
        val i = polar(rInner, c, px, py, scale)
        drawLine(
            color       = if (c == 0f) TICK_CENTER else TICK_COLOR,
            start       = o, end = i,
            strokeWidth = if (c == 0f) 2.8f * scale else 2f * scale,
            cap         = StrokeCap.Round
        )
    }

    // Ticks menores
    for (c in minorTicks) {
        drawLine(
            color       = TICK_COLOR.copy(alpha = 0.65f),
            start       = polar(rMinO, c, px, py, scale),
            end         = polar(rMinI, c, px, py, scale),
            strokeWidth = 1.1f * scale,
            cap         = StrokeCap.Round
        )
    }

    // Labels cents -30 a +30
    for (c in mainTicks) {
        val pos   = polar(rLabel, c, px, py, scale)
        val style = if (c == 0f)
            TextStyle(fontSize = (9f * scale).sp, color = LABEL_COLOR, fontWeight = FontWeight.Bold)
        else
            TextStyle(fontSize = (8f * scale).sp, color = LABEL_COLOR)
        val m = tm.measure(c.toInt().toString(), style)
        drawText(m, topLeft = Offset(pos.x - m.size.width / 2f, pos.y - m.size.height / 2f))
    }

    // Hz 440 topo
    val hz440Style = TextStyle(fontSize = (9f * scale).sp, color = HZ_COLOR, fontWeight = FontWeight.Bold)
    val m440 = tm.measure("440", hz440Style)
    val p440 = polar(196f, 0f, px, py, scale)
    drawText(m440, topLeft = Offset(p440.x - m440.size.width / 2f, p440.y - m440.size.height / 2f))

    // Hz 430 lateral esquerda
    val hzSide = TextStyle(fontSize = (9f * scale).sp, color = HZ_COLOR, fontWeight = FontWeight.Bold)
    val m430 = tm.measure("430", hzSide)
    val p430 = polar(178f, -42f, px, py, scale)
    drawText(m430, topLeft = Offset(
        (p430.x + 14f * scale - m430.size.width / 2f).coerceAtLeast(2f),
        p430.y - m430.size.height / 2f
    ))

    // Hz 450 lateral direita
    val m450 = tm.measure("450", hzSide)
    val p450 = polar(178f, +42f, px, py, scale)
    drawText(m450, topLeft = Offset(
        (p450.x - 14f * scale - m450.size.width / 2f).coerceAtMost(w - m450.size.width - 2f),
        p450.y - m450.size.height / 2f
    ))

    // Tuning guide arco
    val gL = polar(rGuide,     -50f, px, py, scale)
    val gM = polar(rGuideCtrl,   0f, px, py, scale)
    val gR = polar(rGuide,     +50f, px, py, scale)
    val guidePath = Path().apply {
        moveTo(gL.x, gL.y)
        quadraticTo(gM.x, gM.y, gR.x, gR.y)
    }
    drawPath(guidePath, color = TICK_COLOR, style = Stroke(width = 1.2f * scale))

    // Texto TUNING GUIDE
    val tlStyle = TextStyle(
        fontSize      = (4.5f * scale).sp,
        color         = TICK_COLOR,
        letterSpacing = (1f * scale).sp
    )
    val tl    = tm.measure("TUNING GUIDE", tlStyle)
    val tlPos = polar(rTuningLbl, 0f, px, py, scale)
    drawText(tl, topLeft = Offset(
        tlPos.x - tl.size.width / 2f,
        tlPos.y + (42f * scale)
    ))

    // Pivot
    drawCircle(Color(0xFF1A0A00), radius = 9f   * scale, center = Offset(px, py))
    drawCircle(Color(0xFF2A1200), radius = 5f   * scale, center = Offset(px, py))
    drawCircle(Color(0xFFCC6000), radius = 2.5f * scale, center = Offset(px, py))
}

private fun DrawScope.drawNeedle(px: Float, py: Float, scale: Float, angleDeg: Float) {
    val baseY     = py - (298f - 288f) * scale
    val tipY      = py - (298f - 91f)  * scale
    val polyY     = py - (298f - 76f)  * scale
    val polyHalfW = 6.3f * scale

    rotate(degrees = angleDeg, pivot = Offset(px, py)) {
        drawLine(
            color       = Color(0x550A0500),
            start       = Offset(px + 2f * scale, baseY),
            end         = Offset(px + 2f * scale, tipY),
            strokeWidth = 2.2f * scale,
            cap         = StrokeCap.Round
        )
        drawLine(
            color       = NEEDLE_COLOR,
            start       = Offset(px, baseY),
            end         = Offset(px, tipY),
            strokeWidth = 1.8f * scale,
            cap         = StrokeCap.Round
        )
        val path = Path().apply {
            moveTo(px, polyY)
            lineTo(px - polyHalfW, tipY)
            lineTo(px + polyHalfW, tipY)
            close()
        }
        drawPath(path, color = NEEDLE_COLOR)
    }
}

private fun DrawScope.drawCentBadge(w: Float, h: Float, scale: Float, tm: TextMeasurer) {
    val scaleX = w / SVG_W
    val scaleY = h / SVG_H
    val bx = 220f * scaleX
    val by = 62f  * scaleY
    val bw = 46f  * scaleX
    val bh = 18f  * scaleY
    drawRect(CENT_GREEN, topLeft = Offset(bx, by), size = Size(bw, bh))
    val t = tm.measure(
        "CENT",
        TextStyle(fontSize = (4f * scale).sp, color = CENT_TEXT, fontWeight = FontWeight.Bold)
    )
    drawText(t, topLeft = Offset(
        bx + bw / 2f - t.size.width / 2f,
        by + bh / 2f - t.size.height / 2f
    ))
}