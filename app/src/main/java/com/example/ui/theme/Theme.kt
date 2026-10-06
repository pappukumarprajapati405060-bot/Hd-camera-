package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HDCameraColorScheme = darkColorScheme(
  primary = AccentCyan,
  onPrimary = CameraBlack,
  primaryContainer = SurfaceCard,
  onPrimaryContainer = AccentCyan,
  secondary = AccentGold,
  onSecondary = CameraBlack,
  secondaryContainer = SurfaceCard,
  onSecondaryContainer = AccentGold,
  tertiary = AccentRed,
  onTertiary = Color.White,
  background = CameraBlack,
  onBackground = TextWhite,
  surface = SurfaceDark,
  onSurface = TextWhite,
  surfaceVariant = SurfaceCard,
  onSurfaceVariant = TextGray,
  outline = SurfaceBorder
)

@Composable
fun HDCameraTheme(
  content: @Composable () -> Unit
) {
  MaterialTheme(
    colorScheme = HDCameraColorScheme,
    typography = Typography,
    content = content
  )
}
