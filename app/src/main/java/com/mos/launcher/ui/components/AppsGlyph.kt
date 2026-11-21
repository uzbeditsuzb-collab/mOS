package com.mos.launcher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** mOS "all apps" glyph: a 3×3 dot matrix, drawn in code (no image asset). */
@Composable
fun AppsGlyph(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
) {
    Canvas(modifier.size(size)) {
        val step = this.size.minDimension / 3f
        val radius = step * 0.17f
        for (row in 0..2) {
            for (col in 0..2) {
                drawCircle(
                    color = color,
                    radius = radius,
                    center = Offset(step * (col + 0.5f), step * (row + 0.5f)),
                )
            }
        }
    }
}
