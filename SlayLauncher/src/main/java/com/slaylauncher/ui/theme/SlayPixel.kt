package com.slaylauncher.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class SlayCutShape(
    private val cut: Dp = 10.dp,
    private val topOnly: Boolean = false
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val step = with(density) { cut.toPx() }.coerceAtMost(minOf(size.width, size.height) / 4f)
        val sideEnd = if (topOnly) size.height else size.height - step
        val path = Path().apply {
            moveTo(step, 0f); lineTo(size.width - step, 0f); lineTo(size.width, step)
            lineTo(size.width, sideEnd)
            if (!topOnly) lineTo(size.width - step, size.height)
            lineTo(step, size.height); lineTo(0f, sideEnd); lineTo(0f, step); close()
        }
        return Outline.Generic(path)
    }
}

object SlayPalette {
    val accent = Color(0xFF5EC64D)
    val accentSoft = Color(0x215EC64D)
    val onAccent = Color(0xFF06200E)
    val bg = Color(0xFF161718)
    val surface = Color(0xFF1E1F20)
    val surface2 = Color(0xFF26272A)
    val raised = Color(0xFF2A2B2E)
    val inset = Color(0xFF131314)
    val fg = Color(0xFFEDEFEE)
    val fgMuted = Color(0xCCEDEFEE)
    val outline = Color(0x24FFFFFF)
    val dockOutline = Color(0x66000000)
    val bandTop = Color(0x4DFFFFFF)
    val bandBottom = Color(0x52000000)
    val bandBottomSoft = Color(0x38000000)
    val sole = Color(0x73000000)
    val dot = Color(0x99FFFFFF)
    val dotSpots = arrayOf(
        0.12f to 0.24f, 0.30f to 0.18f, 0.50f to 0.26f, 0.70f to 0.18f,
        0.88f to 0.24f, 0.20f to 0.76f, 0.50f to 0.80f, 0.80f to 0.76f
    )
}

fun Modifier.slayPixelSurface(
    fill: Color,
    cut: Dp = 10.dp,
    topOnly: Boolean = false,
    outline: Color = Color.Transparent,
    topBand: Color = Color.Transparent,
    bottomBand: Color = Color.Transparent,
    dots: Boolean = false
): Modifier = this
    .clip(SlayCutShape(cut, topOnly))
    .drawBehind {
        drawRect(color = fill, topLeft = Offset(0f, 0f), size = size)
        val t = 2.dp.toPx()
        val b = 4.dp.toPx()
        if (outline != Color.Transparent) {
            drawRect(color = outline, topLeft = Offset(0f, 0f), size = Size(size.width, t))
            drawRect(color = outline, topLeft = Offset(0f, size.height - t), size = Size(size.width, t))
            drawRect(color = outline, topLeft = Offset(0f, 0f), size = Size(t, size.height))
            drawRect(color = outline, topLeft = Offset(size.width - t, 0f), size = Size(t, size.height))
        }
        if (topBand != Color.Transparent) {
            drawRect(color = topBand, topLeft = Offset(0f, 0f), size = Size(size.width, 3.dp.toPx()))
        }
        if (bottomBand != Color.Transparent) {
            drawRect(color = bottomBand, topLeft = Offset(0f, size.height - b), size = Size(size.width, b))
        }
        if (dots) {
            val s = 2.dp.toPx()
            SlayPalette.dotSpots.forEach { (x, y) ->
                drawRect(
                    color = SlayPalette.dot,
                    topLeft = Offset(size.width * x, size.height * y),
                    size = Size(s, s)
                )
            }
        }
    }
