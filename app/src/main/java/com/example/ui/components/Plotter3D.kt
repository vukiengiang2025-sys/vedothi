package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.math.MathParser
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun Plotter3D(
    formula: String,
    scale: Float,
    theta: Float, // Rotational azimuth angle
    phi: Float,   // Elevation tilt angle
    rangeMin: Float,
    rangeMax: Float,
    onTransform: (dScale: Float, dTheta: Float, dPhi: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val mathParser = remember(formula) {
        try {
            MathParser(formula)
        } catch (e: Exception) {
            null
        }
    }

    val themeAxes = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    val themeText = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(formula, scale, theta, phi, rangeMin, rangeMax) {
                detectTransformGestures { _, pan, zoom, _ ->
                    // Zoom
                    val newScale = max(10f, min(500f, scale * zoom))
                    // Rotation ratios: pan X rotates theta, pan Y tilts phi
                    val dTheta = pan.x * 0.007f
                    val dPhi = -pan.y * 0.007f
                    onTransform(newScale, theta + dTheta, phi + dPhi)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0 || height <= 0) return@Canvas

            val cx = width / 2
            val cy = height / 2

            // Function to project 3D point (X, Y, Z) to 2D screen coordinate (Offset)
            fun project(mx: Double, my: Double, mz: Double): Offset {
                // 1. Rotate around Z-axis by theta (Azimuth)
                val xRot = mx * cos(theta.toDouble()) - my * sin(theta.toDouble())
                val yRot = mx * sin(theta.toDouble()) + my * cos(theta.toDouble())

                // 2. Rotate around X-axis by phi (Elevation tilt)
                // We tilt up and down (elevate the camera)
                val zRot = mz * cos(phi.toDouble()) - yRot * sin(phi.toDouble())

                // Screen mapping
                val sx = cx + (xRot * scale).toFloat()
                val sy = cy - (zRot * scale).toFloat() // Y is inverted in screens

                return Offset(sx, sy)
            }

            // Draw 3D coordinate axes: X (reddish), Y (greenish), Z (blueish)
            val axesLen = max(rangeMax, -rangeMin) * 1.2
            val origin2D = project(0.0, 0.0, 0.0)

            val xAxisEnd2D = project(axesLen, 0.0, 0.0)
            val yAxisEnd2D = project(0.0, axesLen, 0.0)
            val zAxisEnd2D = project(0.0, 0.0, axesLen)

            // Draw absolute Axes lines
            drawLine(Color(0xFFE57373), origin2D, xAxisEnd2D, strokeWidth = 4f) // X Axis
            drawLine(Color(0xFF81C784), origin2D, yAxisEnd2D, strokeWidth = 4f) // Y Axis
            drawLine(Color(0xFF64B5F6), origin2D, zAxisEnd2D, strokeWidth = 4f) // Z Axis

            // Draw Axis Labels "x", "y", "z"
            drawContext.canvas.nativeCanvas.drawText("X", xAxisEnd2D.x + 8f, xAxisEnd2D.y - 4f, android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#E57373")
                textSize = 34f
                isFakeBoldText = true
            })
            drawContext.canvas.nativeCanvas.drawText("Y", yAxisEnd2D.x + 8f, yAxisEnd2D.y - 4f, android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#81C784")
                textSize = 34f
                isFakeBoldText = true
            })
            drawContext.canvas.nativeCanvas.drawText("Z", zAxisEnd2D.x + 8f, zAxisEnd2D.y - 4f, android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#64B5F6")
                textSize = 34f
                isFakeBoldText = true
            })

            // Plot grid mesh of 3D function
            if (mathParser != null) {
                // Sampling resolution
                val steps = 30
                val dx = (rangeMax - rangeMin) / steps
                val dy = (rangeMax - rangeMin) / steps

                // First, compute and cache vertices to find height min/max for beautiful color mapping
                val zGrid = Array(steps + 1) { DoubleArray(steps + 1) }
                var maxZ = -Double.MAX_VALUE
                var minZ = Double.MAX_VALUE
                var hasValidPoints = false

                for (i in 0..steps) {
                    val mx = rangeMin + i * dx
                    for (j in 0..steps) {
                        val my = rangeMin + j * dy
                        try {
                            val mz = mathParser.parse(mx.toDouble(), my.toDouble())
                            if (!mz.isNaN() && !mz.isInfinite() && abs(mz) < 1000.0) {
                                zGrid[i][j] = mz
                                if (mz > maxZ) maxZ = mz
                                if (mz < minZ) minZ = mz
                                hasValidPoints = true
                            } else {
                                zGrid[i][j] = Double.NaN
                            }
                        } catch (e: Exception) {
                            zGrid[i][j] = Double.NaN
                        }
                    }
                }

                if (hasValidPoints) {
                    val zAmplitude = if (maxZ > minZ) (maxZ - minZ) else 10.0

                    // Color mapping helper based on elevation fraction
                    fun getColorForZ(z: Double): Color {
                        if (z.isNaN()) return Color.Gray
                        val fraction = ((z - minZ) / zAmplitude).coerceIn(0.0, 1.0)
                        
                        // Radiant spectrum: Dark Blue (low) -> Teal -> Green -> Yellow -> Orange -> Crimson Red (high)
                        return when {
                            fraction < 0.25 -> {
                                val f = fraction / 0.25
                                Color(
                                    red = (0x0F + f * (0x19 - 0x0F)).toInt(),
                                    green = (0x80 + f * (0xDC - 0x80)).toInt(),
                                    blue = (0xEB + f * (0xD4 - 0xEB)).toInt()
                                )
                            }
                            fraction < 0.5 -> {
                                val f = (fraction - 0.25) / 0.25
                                Color(
                                    red = (0x19 + f * (0x4C - 0x19)).toInt(),
                                    green = (0xDC + f * (0xBB - 0xDC)).toInt(),
                                    blue = (0xD4 + f * (0x5A - 0xD4)).toInt()
                                )
                            }
                            fraction < 0.75 -> {
                                val f = (fraction - 0.5) / 0.25
                                Color(
                                    red = (0x4C + f * (0xF5 - 0x4C)).toInt(),
                                    green = (0xBB + f * (0x7D - 0xBB)).toInt(),
                                    blue = (0x5A + f * (0x22 - 0x5A)).toInt()
                                )
                            }
                            else -> {
                                val f = (fraction - 0.75) / 0.25
                                Color(
                                    red = (0xF5 + f * (0xD3 - 0xF5)).toInt(),
                                    green = (0x7D + f * (0x2F - 0x7D)).toInt(),
                                    blue = (0x22 + f * (0x2F - 0x22)).toInt()
                                )
                            }
                        }
                    }

                    // Render grid line networks
                    for (i in 0..steps) {
                        val mx = rangeMin + i * dx
                        for (j in 0..steps) {
                            val my = rangeMin + j * dy
                            val mz = zGrid[i][j]

                            if (!mz.isNaN()) {
                                val pCurrent = project(mx.toDouble(), my.toDouble(), mz)

                                // 1. Draw segment along Y direction (vary j to j+1)
                                if (j < steps) {
                                    val mzY = zGrid[i][j + 1]
                                    if (!mzY.isNaN()) {
                                        val pY = project(mx.toDouble(), (rangeMin + (j + 1) * dy).toDouble(), mzY)
                                        // Draw only if on screen
                                        if (pCurrent.x in -100f..(width + 100f) && pCurrent.y in -100f..(height + 100f)) {
                                            drawLine(
                                                color = getColorForZ((mz + mzY) / 2.0).copy(alpha = 0.85f),
                                                start = pCurrent,
                                                end = pY,
                                                strokeWidth = 2f
                                            )
                                        }
                                    }
                                }

                                // 2. Draw segment along X direction (vary i to i+1)
                                if (i < steps) {
                                    val mzX = zGrid[i + 1][j]
                                    if (!mzX.isNaN()) {
                                        val pX = project((rangeMin + (i + 1) * dx).toDouble(), my.toDouble(), mzX)
                                        if (pCurrent.x in -100f..(width + 100f) && pCurrent.y in -100f..(height + 100f)) {
                                            drawLine(
                                                color = getColorForZ((mz + mzX) / 2.0).copy(alpha = 0.85f),
                                                start = pCurrent,
                                                end = pX,
                                                strokeWidth = 2f
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
