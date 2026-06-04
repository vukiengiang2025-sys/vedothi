package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.example.math.MathParser
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun Plotter2D(
    formula: String,
    scale: Float,
    centerX: Double,
    centerY: Double,
    onTransform: (dScale: Float, dCenterX: Double, dCenterY: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val mathParser = remember(formula) {
        try {
            MathParser(formula)
        } catch (e: Exception) {
            null
        }
    }

    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val themeGrid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val themeAxes = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    val themeText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(formula, scale, centerX, centerY) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = max(5f, min(10000f, scale * zoom))
                    val dCX = -pan.x / newScale
                    val dCY = pan.y / newScale
                    onTransform(newScale, dCX.toDouble(), dCY.toDouble())
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0 || height <= 0) return@Canvas

            // Mathematical bound calculations
            val mathMinX = centerX - (width / 2) / scale
            val mathMaxX = centerX + (width / 2) / scale
            val mathMinY = centerY - (height / 2) / scale
            val mathMaxY = centerY + (height / 2) / scale

            // Draw Adaptive Grid and Labels
            // Find appropriate major tick step size (1, 2, 5, 10, 20 etc. in power of 10)
            val rangeX = mathMaxX - mathMinX
            val logRange = log10(rangeX)
            val powerOf10 = 10.0.pow(floor(logRange)).toFloat()
            val ratio = rangeX / powerOf10

            val tickStep = when {
                ratio < 2.0 -> powerOf10 / 5f
                ratio < 5.0 -> powerOf10 / 2f
                else -> powerOf10
            }

            // Draw Grid Lines helper
            val startGridX = ceil(mathMinX / tickStep) * tickStep
            val endGridX = floor(mathMaxX / tickStep) * tickStep

            var gridValX = startGridX
            while (gridValX <= endGridX) {
                val sx = (width / 2 + (gridValX - centerX) * scale).toFloat()
                drawLine(
                    color = themeGrid,
                    start = Offset(sx, 0f),
                    end = Offset(sx, height),
                    strokeWidth = 1f
                )
                // Draw coordinate tick text
                if (abs(gridValX) > 1e-6) {
                    val label = String.format("%.2f", gridValX).trimEnd('0').trimEnd('.')
                    val syAxis = (height / 2 + centerY * scale).toFloat().coerceIn(10f, height - 20f)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        sx + 4f,
                        syAxis - 4f,
                        android.graphics.Paint().apply {
                            color = themeText.toArgb()
                            textSize = 28f
                            isAntiAlias = true
                        }
                    )
                }
                gridValX += tickStep
            }

            val startGridY = ceil(mathMinY / tickStep) * tickStep
            val endGridY = floor(mathMaxY / tickStep) * tickStep

            var gridValY = startGridY
            while (gridValY <= endGridY) {
                val sy = (height / 2 - (gridValY - centerY) * scale).toFloat()
                drawLine(
                    color = themeGrid,
                    start = Offset(0f, sy),
                    end = Offset(width, sy),
                    strokeWidth = 1f
                )
                // Draw coordinate tick text
                if (abs(gridValY) > 1e-6) {
                    val label = String.format("%.2f", gridValY).trimEnd('0').trimEnd('.')
                    val sxAxis = (width / 2 - centerX * scale).toFloat().coerceIn(10f, width - 60f)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        sxAxis + 4f,
                        sy - 4f,
                        android.graphics.Paint().apply {
                            color = themeText.toArgb()
                            textSize = 28f
                            isAntiAlias = true
                        }
                    )
                }
                gridValY += tickStep
            }

            // Draw X and Y absolute axes
            val sxAxis = (width / 2 - centerX * scale).toFloat()
            if (sxAxis in 0f..width) {
                drawLine(
                    color = themeAxes,
                    start = Offset(sxAxis, 0f),
                    end = Offset(sxAxis, height),
                    strokeWidth = 3f
                )
            }

            val syAxis = (height / 2 + centerY * scale).toFloat()
            if (syAxis in 0f..height) {
                drawLine(
                    color = themeAxes,
                    start = Offset(0f, syAxis),
                    end = Offset(width, syAxis),
                    strokeWidth = 3f
                )
            }

            // Draw Title / Origin label (0, 0)
            if (sxAxis in 0f..width && syAxis in 0f..height) {
                drawContext.canvas.nativeCanvas.drawText(
                    "0",
                    sxAxis + 6f,
                    syAxis + 26f,
                    android.graphics.Paint().apply {
                        color = themeAxes.toArgb()
                        textSize = 28f
                        isAntiAlias = true
                    }
                )
            }

            // Draw the mathematical function if parser is valid
            if (mathParser != null) {
                val path = Path()
                var inSegment = false

                // Sample points at every pixel width for extreme high accuracy
                val numSamples = width.roundToInt()
                for (pixelX in 0..numSamples) {
                    val mX = ((pixelX - width / 2) / scale + centerX).toDouble()
                    try {
                        val mY = mathParser.parse(mX)
                        if (mY.isNaN() || mY.isInfinite()) {
                            inSegment = false
                        } else {
                            val pixelY = (height / 2 - (mY - centerY) * scale).toFloat()
                            val isYVisible = pixelY in -100f..(height + 100f) // Allow slight overflow for clipping

                            if (isYVisible) {
                                if (!inSegment) {
                                    path.moveTo(pixelX.toFloat(), pixelY)
                                    inSegment = true
                                } else {
                                    path.lineTo(pixelX.toFloat(), pixelY)
                                }
                            } else {
                                // High jumping vertical asymptotes
                                inSegment = false
                            }
                        }
                    } catch (e: Exception) {
                        inSegment = false
                    }
                }

                drawPath(
                    path = path,
                    color = themePrimary,
                    style = Stroke(width = 5f)
                )
            }
        }
    }
}
