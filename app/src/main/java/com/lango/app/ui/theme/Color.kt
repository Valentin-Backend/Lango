package com.lango.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val PureBlack      = Color(0xFF09090F)
val DarkBg         = Color(0xFF0D0D14)
val DarkSurface    = Color(0xFF14141F)
val DarkCard       = Color(0xFF1A1A2A)
val DarkCardAlt    = Color(0xFF1E1230)

val MediumPurple   = Color(0xFF7B2CBF)
val LightPurple    = Color(0xFF9D4EDD)
val BrightPurple   = Color(0xFFA855F7)
val VibrantPurple  = Color(0xFFC77DFF)
val SoftPurple     = Color(0xFFE0AAFF)

val Secondary      = Color(0xFF4ECDC4)
val SecondaryDark  = Color(0xFF36B0A8)

val ColorSuccess   = Color(0xFF10B981)
val ColorWarning   = Color(0xFFF59E0B)
val ColorError     = Color(0xFFEF4444)
val ColorNew       = Color(0xFF64B5F6)

val TextPrimary    = Color(0xFFFFFFFF)
val TextSecondary  = Color(0xFF8888AA)
val TextHint       = Color(0xFF555570)

val Primary        = BrightPurple
val PrimaryLight   = VibrantPurple
val PrimaryDark    = MediumPurple

val PrimaryGradient = Brush.horizontalGradient(
    colors = listOf(MediumPurple, BrightPurple)
)
val PrimaryGradientVertical = Brush.verticalGradient(
    colors = listOf(MediumPurple, LightPurple)
)
val GradientBackground = Brush.verticalGradient(
    colors = listOf(DarkBg, DarkBg)
)
val CardGradient = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))
)
val GlassGradient = Brush.linearGradient(
    colors = listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f))
)
val AuthGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF09090F), Color(0xFF150B28), Color(0xFF09090F))
)

val DeckColorStops: List<List<Color>> = listOf(
    listOf(Color(0xFF2A2A36), Color(0xFF1F1F2B)),
    listOf(Color(0xFF7B2CBF), Color(0xFFA855F7)),
    listOf(Color(0xFF0369A1), Color(0xFF0EA5E9)),
    listOf(Color(0xFF065F46), Color(0xFF10B981)),
    listOf(Color(0xFF92400E), Color(0xFFF59E0B)),
    listOf(Color(0xFF991B1B), Color(0xFFEF4444)),
    listOf(Color(0xFF1E1B4B), Color(0xFF6366F1)),
    listOf(Color(0xFF831843), Color(0xFFEC4899)),
    listOf(Color(0xFF134E4A), Color(0xFF14B8A6))
)

val DeckColors: List<Brush> = DeckColorStops.map { colors ->
    Brush.horizontalGradient(colors)
}

val LangoColorScheme = darkColorScheme(
    primary = Primary, onPrimary = Color.White,
    primaryContainer = MediumPurple, onPrimaryContainer = SoftPurple,
    secondary = Secondary, onSecondary = Color.White,
    secondaryContainer = SecondaryDark,
    background = DarkBg, onBackground = TextPrimary,
    surface = DarkSurface, onSurface = TextPrimary,
    surfaceVariant = DarkCard, onSurfaceVariant = TextSecondary,
    error = ColorError, outline = Color.White.copy(alpha = 0.10f)
)
