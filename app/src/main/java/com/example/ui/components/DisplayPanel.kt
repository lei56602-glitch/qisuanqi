package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.AngleUnit
import com.example.domain.CalcErrorType
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCalculatorPalette
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.viewmodel.CalculatorUiState

@Composable
fun DisplayPanel(
  state: CalculatorUiState,
  onToggleScientific: () -> Unit,
  onToggleAngleUnit: () -> Unit,
  onOpenHistory: () -> Unit,
  onToggleTheme: () -> Unit,
  onCopyResult: () -> Unit,
  showHistoryButton: Boolean = true,
  expandToFill: Boolean = false,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCalculatorPalette.current
  val scrollState = rememberScrollState()

  LaunchedEffect(state.formattedExpression) {
    scrollState.animateScrollTo(scrollState.maxValue)
  }

  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .pointerInput(Unit) {
          detectVerticalDragGestures { _, dragAmount ->
            if (dragAmount > 28f && showHistoryButton) {
              onOpenHistory()
            }
          }
        },
    shape = RoundedCornerShape(28.dp),
    color = palette.displaySurface,
    shadowElevation = if (palette.isDark) 0.dp else 3.dp,
    border = androidx.compose.foundation.BorderStroke(0.85.dp, palette.divider),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .then(if (expandToFill) Modifier.fillMaxHeight() else Modifier)
          .drawBehind {
            val glowRadius = size.maxDimension * 0.72f
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors = listOf(palette.displayGlow.copy(alpha = 0.85f), Color.Transparent),
                  center = Offset(size.width * 0.88f, size.height * 0.78f),
                  radius = glowRadius,
                ),
              radius = glowRadius,
              center = Offset(size.width * 0.88f, size.height * 0.78f),
            )
          }
    ) {
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .then(if (expandToFill) Modifier.fillMaxHeight() else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        // Top Utility Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ModeSegmentedPill(
              isScientific = state.isScientific,
              onToggleScientific = onToggleScientific,
            )

            if (state.isScientific) {
              Surface(
                onClick = onToggleAngleUnit,
                shape = RoundedCornerShape(12.dp),
                color = palette.utilityKey,
                modifier = Modifier.minimumInteractiveComponentSize().testTag("angle_unit_button"),
              ) {
                Text(
                  text =
                    if (state.angleUnit == AngleUnit.DEG) {
                      stringResource(R.string.angle_deg)
                    } else {
                      stringResource(R.string.angle_rad)
                    },
                  style = MaterialTheme.typography.labelMedium,
                  color = palette.operatorKeyText,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
              }
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            if (showHistoryButton) {
              Box(contentAlignment = Alignment.TopEnd) {
                IconButton(
                  onClick = onOpenHistory,
                  modifier = Modifier.testTag("history_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = stringResource(R.string.cd_toggle_history),
                    tint = palette.secondaryText,
                  )
                }
                if (state.history.isNotEmpty()) {
                  Box(
                    modifier =
                      Modifier.padding(top = 10.dp, end = 10.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(palette.equalsKey)
                  )
                }
              }
            }

            IconButton(
              onClick = onToggleTheme,
              modifier = Modifier.testTag("theme_toggle_button"),
            ) {
              Icon(
                imageVector =
                  if (palette.isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = stringResource(R.string.cd_toggle_theme),
                tint = palette.secondaryText,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Main Expression / Result Readout
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.End,
        ) {
          val displayText = state.formattedExpression.ifEmpty { "0" }
          val textLength = displayText.length
          val fontSize =
            when {
              textLength <= 9 -> 52.sp
              textLength <= 13 -> 42.sp
              textLength <= 18 -> 34.sp
              else -> 28.sp
            }

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (state.formattedExpression.isEmpty()) {
                  AnnotatedString("0")
                } else {
                  highlightExpression(
                    text = displayText,
                    operatorColor = palette.operatorKeyText,
                    symbolColor = palette.secondaryText,
                  )
                },
              fontFamily = SpaceGroteskFontFamily,
              fontWeight = FontWeight.SemiBold,
              fontSize = fontSize,
              lineHeight = fontSize * 1.15f,
              color =
                if (state.formattedExpression.isEmpty()) {
                  palette.secondaryText.copy(alpha = 0.55f)
                } else {
                  MaterialTheme.colorScheme.onSurface
                },
              maxLines = 1,
              softWrap = false,
              textAlign = TextAlign.End,
              style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum"),
              modifier = Modifier.testTag("display_expression"),
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Bottom Status / Live Preview / Copy Pill
          Box(
            modifier = Modifier.fillMaxWidth().height(34.dp),
            contentAlignment = Alignment.CenterEnd,
          ) {
            when {
              state.errorType != null -> {
                val errorRes =
                  when (state.errorType) {
                    CalcErrorType.DIVIDE_BY_ZERO -> R.string.error_divide_by_zero
                    CalcErrorType.DOMAIN -> R.string.error_domain
                    CalcErrorType.OVERFLOW -> R.string.error_overflow
                    CalcErrorType.SYNTAX -> R.string.error_syntax
                  }
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = palette.clearKey,
                  modifier = Modifier.testTag("display_error"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.ErrorOutline,
                      contentDescription = null,
                      tint = palette.clearKeyText,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = stringResource(errorRes),
                      style = MaterialTheme.typography.labelLarge,
                      color = palette.clearKeyText,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }
              state.livePreview.isNotEmpty() -> {
                Text(
                  text = "= ${state.livePreview}",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Medium,
                  fontSize = 20.sp,
                  color = palette.operatorKeyText,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("display_preview"),
                )
              }
              state.justEvaluated && state.expression.isNotEmpty() -> {
                Surface(
                  onClick = onCopyResult,
                  shape = RoundedCornerShape(12.dp),
                  color = palette.utilityKey.copy(alpha = 0.7f),
                  modifier = Modifier.testTag("copy_result_button"),
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.ContentCopy,
                      contentDescription = stringResource(R.string.cd_copy_result),
                      tint = palette.secondaryText,
                      modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                      text = stringResource(R.string.cd_copy_result),
                      style = MaterialTheme.typography.labelMedium,
                      color = palette.secondaryText,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ModeSegmentedPill(
  isScientific: Boolean,
  onToggleScientific: () -> Unit,
) {
  val palette = LocalCalculatorPalette.current
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = palette.background.copy(alpha = 0.7f),
  ) {
    Row(
      modifier = Modifier.padding(3.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      ModeTab(
        label = stringResource(R.string.mode_standard),
        selected = !isScientific,
        onClick = { if (isScientific) onToggleScientific() },
        tag = "mode_standard_tab",
      )
      ModeTab(
        label = stringResource(R.string.mode_scientific),
        selected = isScientific,
        onClick = { if (!isScientific) onToggleScientific() },
        tag = "mode_scientific_tab",
      )
    }
  }
}

@Composable
private fun ModeTab(
  label: String,
  selected: Boolean,
  onClick: () -> Unit,
  tag: String,
) {
  val palette = LocalCalculatorPalette.current
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(11.dp))
        .background(if (selected) palette.equalsKey else Color.Transparent)
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 6.dp)
        .testTag(tag),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelLarge,
      color = if (selected) palette.equalsKeyText else palette.secondaryText,
    )
  }
}

private fun highlightExpression(
  text: String,
  operatorColor: Color,
  symbolColor: Color,
): AnnotatedString {
  val operators = setOf('+', '−', '×', '÷', '^', '%', '!')
  val brackets = setOf('(', ')')
  return buildAnnotatedString {
    append(text)
    text.forEachIndexed { index, ch ->
      when {
        ch in operators -> {
          addStyle(SpanStyle(color = operatorColor), index, index + 1)
        }
        ch in brackets -> {
          addStyle(SpanStyle(color = symbolColor), index, index + 1)
        }
      }
    }
  }
}
