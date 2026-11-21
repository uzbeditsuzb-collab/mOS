package com.mos.launcher.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.mos.launcher.data.icons.IconLoader
import com.mos.launcher.domain.model.AppInfo

val LocalIconLoader = staticCompositionLocalOf<IconLoader> {
    error("IconLoader not provided")
}

/**
 * Draws an app icon. The bitmap is loaded off the main thread only when this
 * composable enters composition (i.e. when it becomes visible in a lazy list),
 * and comes straight from cache on subsequent appearances.
 */
@Composable
fun AppIcon(
    app: AppInfo,
    size: Dp,
    modifier: Modifier = Modifier,
    placeholderColor: Color = Color.White.copy(alpha = 0.12f),
) {
    val loader = LocalIconLoader.current
    val sizePx = with(LocalDensity.current) { size.roundToPx() }

    val bitmap: Bitmap? by produceState(
        initialValue = loader.cached(app, sizePx),
        app.key, sizePx,
    ) {
        if (value == null) value = loader.load(app, sizePx)
    }

    val current = bitmap
    if (current != null) {
        val image = remember(current) { current.asImageBitmap() }
        Image(
            bitmap = image,
            contentDescription = null, // label is announced by the parent tile
            modifier = modifier.size(size),
            filterQuality = FilterQuality.Medium,
        )
    } else {
        Box(
            modifier
                .size(size)
                .background(placeholderColor, CircleShape)
        )
    }
}
