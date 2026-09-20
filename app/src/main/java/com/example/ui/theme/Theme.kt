package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = DiscordBlurple,
  onPrimary = Color.White,
  primaryContainer = DiscordBlurpleDark,
  onPrimaryContainer = Color.White,
  secondary = DiscordGreen,
  onSecondary = Color.Black,
  secondaryContainer = DiscordHover,
  onSecondaryContainer = DiscordTextPrimary,
  tertiary = DiscordYellow,
  onTertiary = Color.Black,
  background = DiscordBackground,
  onBackground = DiscordTextPrimary,
  surface = DiscordSurface,
  onSurface = DiscordTextPrimary,
  surfaceVariant = DiscordElevated,
  onSurfaceVariant = DiscordTextSecondary,
  error = DiscordRed,
  onError = Color.White,
  outline = DiscordHover
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to Discord dark theme for authentic developer experience
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}

