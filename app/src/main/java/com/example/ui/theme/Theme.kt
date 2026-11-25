package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CalculatorPalette(
  val background: Color,
  val displaySurface: Color,
  val displayGlow: Color,
  val keypadDeck: Color,
  val numberKey: Color,
  val numberKeyBottom: Color,
  val numberKeyBorder: Color,
  val numberKeyBorderBottom: Color,
  val numberKeyText: Color,
  val utilityKey: Color,
  val utilityKeyBottom: Color,
  val utilityKeyBorder: Color,
  val utilityKeyText: Color,
  val scientificKey: Color,
  val scientificKeyBorder: Color,
  val scientificKeyText: Color,
  val operatorKey: Color,
  val operatorKeyBottom: Color,
  val operatorKeyBorder: Color,
  val operatorKeyText: Color,
  val clearKey: Color,
  val clearKeyBottom: Color,
  val clearKeyBorder: Color,
  val clearKeyText: Color,
  val equalsKey: Color,
  val equalsKeyBottom: Color,
  val equalsKeyText: Color,
  val secondaryText: Color,
  val divider: Color,
  val isDark: Boolean,
)

private val DarkCalculatorPalette =
  CalculatorPalette(
    background = CarbonBackground,
    displaySurface = CarbonSurface,
    displayGlow = CarbonDisplayGlow,
    keypadDeck = CarbonKeypadDeck,
    numberKey = CarbonNumTop,
    numberKeyBottom = CarbonNumBottom,
    numberKeyBorder = CarbonNumBorderTop,
    numberKeyBorderBottom = CarbonNumBorderBottom,
    numberKeyText = CarbonTextPrimary,
    utilityKey = CarbonUtilTop,
    utilityKeyBottom = CarbonUtilBottom,
    utilityKeyBorder = CarbonUtilBorder,
    utilityKeyText = CarbonTextPrimary,
    scientificKey = CarbonSciBg,
    scientificKeyBorder = CarbonSciBorder,
    scientificKeyText = CarbonTextSecondary,
    operatorKey = CarbonOpTop,
    operatorKeyBottom = CarbonOpBottom,
    operatorKeyBorder = CarbonOpBorder,
    operatorKeyText = CarbonOpText,
    clearKey = CarbonClearTop,
    clearKeyBottom = CarbonClearBottom,
    clearKeyBorder = CarbonClearBorder,
    clearKeyText = CarbonClearText,
    equalsKey = CarbonPrimaryTop,
    equalsKeyBottom = CarbonPrimaryBottom,
    equalsKeyText = CarbonOnPrimary,
    secondaryText = CarbonTextSecondary,
    divider = CarbonDivider,
    isDark = true,
  )

private val LightCalculatorPalette =
  CalculatorPalette(
    background = PorcelainBackground,
    displaySurface = PorcelainSurface,
    displayGlow = PorcelainDisplayGlow,
    keypadDeck = PorcelainKeypadDeck,
    numberKey = PorcelainNumTop,
    numberKeyBottom = PorcelainNumBottom,
    numberKeyBorder = PorcelainNumBorderTop,
    numberKeyBorderBottom = PorcelainNumBorderBottom,
    numberKeyText = PorcelainTextPrimary,
    utilityKey = PorcelainUtilTop,
    utilityKeyBottom = PorcelainUtilBottom,
    utilityKeyBorder = PorcelainUtilBorder,
    utilityKeyText = PorcelainTextPrimary,
    scientificKey = PorcelainSciBg,
    scientificKeyBorder = PorcelainSciBorder,
    scientificKeyText = PorcelainTextSecondary,
    operatorKey = PorcelainOpTop,
    operatorKeyBottom = PorcelainOpBottom,
    operatorKeyBorder = PorcelainOpBorder,
    operatorKeyText = PorcelainOpText,
    clearKey = PorcelainClearTop,
    clearKeyBottom = PorcelainClearBottom,
    clearKeyBorder = PorcelainClearBorder,
    clearKeyText = PorcelainClearText,
    equalsKey = PorcelainPrimaryTop,
    equalsKeyBottom = PorcelainPrimaryBottom,
    equalsKeyText = PorcelainOnPrimary,
    secondaryText = PorcelainTextSecondary,
    divider = PorcelainDivider,
    isDark = false,
  )

val LocalCalculatorPalette = staticCompositionLocalOf { DarkCalculatorPalette }

private val DarkColorScheme =
  darkColorScheme(
    primary = CarbonPrimaryTop,
    onPrimary = CarbonOnPrimary,
    secondary = CarbonOpText,
    background = CarbonBackground,
    onBackground = CarbonTextPrimary,
    surface = CarbonSurface,
    onSurface = CarbonTextPrimary,
    surfaceVariant = CarbonUtilTop,
    onSurfaceVariant = CarbonTextSecondary,
    outline = CarbonDivider,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PorcelainPrimaryTop,
    onPrimary = PorcelainOnPrimary,
    secondary = PorcelainOpText,
    background = PorcelainBackground,
    onBackground = PorcelainTextPrimary,
    surface = PorcelainSurface,
    onSurface = PorcelainTextPrimary,
    surfaceVariant = PorcelainUtilTop,
    onSurfaceVariant = PorcelainTextSecondary,
    outline = PorcelainDivider,
  )

enum class ThemeMode {
  SYSTEM,
  DARK,
  LIGHT,
}

@Composable
fun PrecisionCalcTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
    }

  val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
  val palette = if (isDark) DarkCalculatorPalette else LightCalculatorPalette

  CompositionLocalProvider(LocalCalculatorPalette provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      content = content,
    )
  }
}
