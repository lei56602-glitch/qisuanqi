package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.CalculatorScreen
import com.example.ui.theme.LocalCalculatorPalette
import com.example.ui.theme.PrecisionCalcTheme
import com.example.viewmodel.CalculatorViewModel

class MainActivity : ComponentActivity() {
  private val calculatorViewModel: CalculatorViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
    )
    setContent {
      val state by calculatorViewModel.uiState.collectAsStateWithLifecycle()
      PrecisionCalcTheme(themeMode = state.themeMode) {
        val isDark = LocalCalculatorPalette.current.isDark
        val view = LocalView.current
        if (!view.isInEditMode) {
          SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
          }
        }
        CalculatorScreen(viewModel = calculatorViewModel)
      }
    }
  }
}
