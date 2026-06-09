package com.v2j.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = LeafLight,
    onPrimary = Color.White,
    primaryContainer = LeafContainerLight,
    onPrimaryContainer = OnLeafContainerLight,
    secondary = LeafLight,
    onSecondary = Color.White,
    secondaryContainer = LeafContainerLight,
    onSecondaryContainer = OnLeafContainerLight,
    tertiary = ClayLight,
    onTertiary = Color.White,
    tertiaryContainer = ClayContainerLight,
    onTertiaryContainer = OnClayContainerLight,
    background = PaperLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = MutedLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    surfaceTint = LeafLight,
    inverseSurface = InkLight,
    inverseOnSurface = PaperLight,
)

private val DarkColors = darkColorScheme(
    primary = LeafDark,
    onPrimary = Color(0xFF0E2A19),
    primaryContainer = LeafContainerDark,
    onPrimaryContainer = OnLeafContainerDark,
    secondary = LeafDark,
    onSecondary = Color(0xFF0E2A19),
    secondaryContainer = LeafContainerDark,
    onSecondaryContainer = OnLeafContainerDark,
    tertiary = ClayDark,
    onTertiary = Color(0xFF3A1E0C),
    tertiaryContainer = ClayContainerDark,
    onTertiaryContainer = OnClayContainerDark,
    background = PaperDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorDark,
    onError = Color(0xFF44150C),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    surfaceTint = LeafDark,
    inverseSurface = InkDark,
    inverseOnSurface = PaperDark,
)

private val VoiceleafShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val VoiceleafTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontSize = 16.sp, lineHeight = 26.sp),
        bodyMedium = bodyMedium.copy(fontSize = 15.sp, lineHeight = 24.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.2.sp),
    )
}

@Composable
fun VoiceleafTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = VoiceleafTypography,
        shapes = VoiceleafShapes,
        content = content,
    )
}
