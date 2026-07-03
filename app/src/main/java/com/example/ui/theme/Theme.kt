package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme =
  darkColorScheme(
    primary = GeoPrimary,
    secondary = GeoPrimaryVariant,
    tertiary = GeoPrimaryContainer,
    background = GeoBackground,
    surface = GeoSurface,
    surfaceVariant = GeoSurfaceVariant,
    onPrimary = GeoOnPrimary,
    onPrimaryContainer = GeoOnPrimaryContainer,
    onBackground = GeoText,
    onSurface = GeoText,
    outline = GeoBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  // Always enforce our custom Geometric Balance dark theme for an immersive editing experience
  val colorScheme = DarkColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
