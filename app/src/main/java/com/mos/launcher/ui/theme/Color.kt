package com.mos.launcher.ui.theme

import androidx.compose.ui.graphics.Color

// mOS palette — restrained neutrals with a single cool accent ("Glacier").

// Dark
internal val Ink950 = Color(0xFF0E1116)
internal val Ink900 = Color(0xFF151920)
internal val Ink850 = Color(0xFF1B2029)
internal val Ink800 = Color(0xFF232935)
internal val Ink600 = Color(0xFF3A4252)
internal val Mist300 = Color(0xFF9AA3B2)
internal val Mist100 = Color(0xFFE6E9EF)
internal val GlacierDark = Color(0xFF8FB8DE)
internal val GlacierDarkContainer = Color(0xFF26415C)

// Light
internal val Paper50 = Color(0xFFF6F7F9)
internal val Paper0 = Color(0xFFFFFFFF)
internal val Paper100 = Color(0xFFECEEF2)
internal val Paper200 = Color(0xFFDDE1E8)
internal val Slate500 = Color(0xFF5B6475)
internal val Slate900 = Color(0xFF14181F)
internal val GlacierLight = Color(0xFF2F5D85)
internal val GlacierLightContainer = Color(0xFFD3E4F5)

// Error
internal val ErrorDark = Color(0xFFF2B8B5)
internal val ErrorLight = Color(0xFFB3261E)

/** Content drawn directly on the wallpaper: always light, regardless of theme. */
object OnWallpaper {
    val Primary = Color(0xFFFFFFFF)
    val Secondary = Color(0xD9FFFFFF)   // 85%
    val Tertiary = Color(0x99FFFFFF)    // 60%
    val Glass = Color(0x24FFFFFF)       // 14% — dock / chips
    val GlassStroke = Color(0x1FFFFFFF) // 12%
    val Shadow = Color(0x59000000)      // 35%
}
