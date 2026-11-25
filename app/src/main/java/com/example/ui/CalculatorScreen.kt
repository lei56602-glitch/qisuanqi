package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.DisplayPanel
import com.example.ui.components.HistoryBottomSheet
import com.example.ui.components.HistoryListContent
import com.example.ui.components.ScientificKeypadSection
import com.example.ui.components.StandardKeypadSection
import com.example.ui.theme.LocalCalculatorPalette
import com.example.viewmodel.CalculatorUiState
import com.example.viewmodel.CalculatorViewModel
import kotlinx.coroutines.launch

@Composable
fun CalculatorScreen(
  viewModel: CalculatorViewModel,
  modifier: Modifier = Modifier,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val palette = LocalCalculatorPalette.current
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var showHistorySheet by rememberSaveable { mutableStateOf(false) }

  val onCopyResult: () -> Unit = {
    val textToCopy = state.formattedExpression.ifEmpty { state.expression }
    if (textToCopy.isNotEmpty()) {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      clipboard?.setPrimaryClip(ClipData.newPlainText("Calculator Result", textToCopy))
      scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(
          message = context.getString(R.string.copied_to_clipboard, textToCopy)
        )
      }
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = palette.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
  ) { innerPadding ->
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.Center,
    ) {
      val isWideLayout = maxWidth >= 600.dp

      if (isWideLayout) {
        WideCalculatorLayout(
          state = state,
          viewModel = viewModel,
          onCopyResult = onCopyResult,
          modifier = Modifier.fillMaxSize().widthIn(max = 1080.dp).padding(16.dp),
        )
      } else {
        CompactCalculatorLayout(
          state = state,
          viewModel = viewModel,
          onOpenHistory = { showHistorySheet = true },
          onCopyResult = onCopyResult,
          modifier = Modifier.fillMaxSize().widthIn(max = 520.dp).padding(horizontal = 16.dp, vertical = 10.dp),
        )
      }
    }

    if (showHistorySheet) {
      HistoryBottomSheet(
        history = state.history,
        onSelectRecord = { record, useResultOnly ->
          viewModel.onSelectHistoryItem(record, useResultOnly)
        },
        onClearHistory = viewModel::onClearHistory,
        onDismiss = { showHistorySheet = false },
      )
    }
  }
}

@Composable
private fun CompactCalculatorLayout(
  state: CalculatorUiState,
  viewModel: CalculatorViewModel,
  onOpenHistory: () -> Unit,
  onCopyResult: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BoxWithConstraints(modifier = modifier) {
    val minDisplayHeight = 178.dp
    val sciTrayHeight = if (state.isScientific) 186.dp else 0.dp
    val standardBudget = (maxHeight - minDisplayHeight - sciTrayHeight - 24.dp).coerceAtLeast(284.dp)

    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      DisplayPanel(
        state = state,
        onToggleScientific = viewModel::onToggleScientific,
        onToggleAngleUnit = viewModel::onToggleAngleUnit,
        onOpenHistory = onOpenHistory,
        onToggleTheme = viewModel::onCycleThemeMode,
        onCopyResult = onCopyResult,
        showHistoryButton = true,
        expandToFill = true,
        modifier = Modifier.weight(1f),
      )

      ScientificKeypadSection(
        isVisible = state.isScientific,
        isInverse = state.isInverse,
        onToggleInverse = viewModel::onToggleInverse,
        onFunction = viewModel::onFunction,
        onConstant = viewModel::onConstant,
        onOperator = viewModel::onOperator,
        onPostfix = viewModel::onPostfix,
        onSquare = viewModel::onSquare,
        onReciprocal = viewModel::onReciprocal,
        onToggleSign = viewModel::onToggleSign,
      )

      StandardKeypadSection(
        onDigit = viewModel::onDigit,
        onDecimal = viewModel::onDecimal,
        onOperator = viewModel::onOperator,
        onParenthesis = viewModel::onParenthesis,
        onPercent = { viewModel.onPostfix('%') },
        onClearAll = viewModel::onClearAll,
        onBackspace = viewModel::onBackspace,
        onEquals = viewModel::onEquals,
        fillAvailableHeight = false,
        maxGridHeight = standardBudget,
      )
    }
  }
}

@Composable
private fun WideCalculatorLayout(
  state: CalculatorUiState,
  viewModel: CalculatorViewModel,
  onCopyResult: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCalculatorPalette.current

  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // Left Pane: Display + Inline History Tape
    Column(
      modifier = Modifier.weight(0.45f).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      DisplayPanel(
        state = state,
        onToggleScientific = viewModel::onToggleScientific,
        onToggleAngleUnit = viewModel::onToggleAngleUnit,
        onOpenHistory = {},
        onToggleTheme = viewModel::onCycleThemeMode,
        onCopyResult = onCopyResult,
        showHistoryButton = false,
      )

      Surface(
        shape = RoundedCornerShape(24.dp),
        color = palette.displaySurface,
        modifier = Modifier.weight(1f).fillMaxWidth(),
      ) {
        Box(modifier = Modifier.fillMaxSize().padding(top = 14.dp)) {
          HistoryListContent(
            history = state.history,
            onSelectRecord = { record, useResultOnly ->
              viewModel.onSelectHistoryItem(record, useResultOnly)
            },
            onClearHistory = viewModel::onClearHistory,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }

    // Right Pane: Scientific + Standard Keypad
    Column(
      modifier = Modifier.weight(0.55f).fillMaxHeight(),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      ScientificKeypadSection(
        isVisible = state.isScientific,
        isInverse = state.isInverse,
        onToggleInverse = viewModel::onToggleInverse,
        onFunction = viewModel::onFunction,
        onConstant = viewModel::onConstant,
        onOperator = viewModel::onOperator,
        onPostfix = viewModel::onPostfix,
        onSquare = viewModel::onSquare,
        onReciprocal = viewModel::onReciprocal,
        onToggleSign = viewModel::onToggleSign,
      )

      Spacer(modifier = Modifier.height(0.dp))

      StandardKeypadSection(
        onDigit = viewModel::onDigit,
        onDecimal = viewModel::onDecimal,
        onOperator = viewModel::onOperator,
        onParenthesis = viewModel::onParenthesis,
        onPercent = { viewModel.onPostfix('%') },
        onClearAll = viewModel::onClearAll,
        onBackspace = viewModel::onBackspace,
        onEquals = viewModel::onEquals,
        fillAvailableHeight = true,
        modifier = Modifier.weight(1f),
      )
    }
  }
}
