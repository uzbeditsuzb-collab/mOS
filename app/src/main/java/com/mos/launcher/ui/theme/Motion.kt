package com.mos.launcher.ui.theme

import androidx.compose.animation.core.CubicBezierEasing

/** mOS motion tokens — identical to the prototype's --ease and timings. */
object MosMotion {
    val Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    const val EnterMillis = 300
    const val ExitMillis = 240
}
