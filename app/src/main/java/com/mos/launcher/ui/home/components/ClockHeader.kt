package com.mos.launcher.ui.home.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mos.launcher.R
import com.mos.launcher.ui.theme.OnWallpaper
import com.mos.launcher.ui.theme.WallpaperTextShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Emits the current time, then again exactly at each minute boundary.
 * Collected with lifecycle awareness, so it stops while mOS is in the background.
 */
private fun minuteTicker(): Flow<LocalDateTime> = flow {
    while (true) {
        val now = LocalDateTime.now()
        emit(now)
        val untilNextMinute = 60_000L - (now.second * 1_000L + now.nano / 1_000_000L)
        delay(untilNextMinute.coerceAtLeast(250L))
    }
}

@Composable
fun ClockHeader(
    showDefaultBanner: Boolean,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val ticker = remember { minuteTicker() }
    val now by ticker.collectAsStateWithLifecycle(initialValue = LocalDateTime.now())

    val is24h = DateFormat.is24HourFormat(context)
    val timeText = remember(now, is24h, locale) {
        now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm", locale))
    }
    val dateText = remember(now, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
        now.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    Column(modifier) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(shadow = WallpaperTextShadow),
            color = OnWallpaper.Primary,
            modifier = Modifier.noRippleClickable(onClockClick),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodyLarge.copy(shadow = WallpaperTextShadow),
            color = OnWallpaper.Secondary,
            modifier = Modifier.noRippleClickable(onDateClick),
        )
        if (showDefaultBanner) {
            Spacer(Modifier.height(14.dp))
            DefaultHomeBanner(onClick = onSetDefault)
        }
    }
}

@Composable
private fun DefaultHomeBanner(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(OnWallpaper.Glass)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.home_not_default),
            style = MaterialTheme.typography.labelMedium,
            color = OnWallpaper.Primary,
        )
        Text(
            text = stringResource(R.string.home_set_default),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
