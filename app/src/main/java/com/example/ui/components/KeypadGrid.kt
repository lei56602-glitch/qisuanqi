package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCalculatorPalette
import com.example.ui.theme.SpaceGroteskFontFamily

enum class KeyStyle {
  NUMBER,
  UTILITY,
  OPERATOR,
  CLEAR,
  EQUALS,
  SCIENTIFIC,
  SCIENTIFIC_ACTIVE,
}

data class KeySpec(
  val label: String,
  val testTag: String,
  val style: KeyStyle,
  val icon: ImageVector? = null,
  val contentDesc: String? = null,
  val onClick: () -> Unit,
)

@Composable
fun ScientificKeypadSection(
  isVisible: Boolean,
  isInverse: Boolean,
  onToggleInverse: () -> Unit,
  onFunction: (String) -> Unit,
  onConstant: (String) -> Unit,
  onOperator: (Char) -> Unit,
  onPostfix: (Char) -> Unit,
  onSquare: () -> Unit,
  onReciprocal: () -> Unit,
  onToggleSign: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCalculatorPalette.current

  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn() + expandVertically(),
    exit = fadeOut() + shrinkVertically(),
    modifier = modifier,
  ) {
    val rows =
      listOf(
        listOf(
          KeySpec(
            label = "2nd",
            testTag = "key_inv",
            style = if (isInverse) KeyStyle.SCIENTIFIC_ACTIVE else KeyStyle.SCIENTIFIC,
            onClick = onToggleInverse,
          ),
          KeySpec(
            label = if (isInverse) "sin⁻¹" else "sin",
            testTag = "key_sin",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onFunction(if (isInverse) "sin⁻¹(" else "sin(") },
          ),
          KeySpec(
            label = if (isInverse) "cos⁻¹" else "cos",
            testTag = "key_cos",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onFunction(if (isInverse) "cos⁻¹(" else "cos(") },
          ),
          KeySpec(
            label = if (isInverse) "tan⁻¹" else "tan",
            testTag = "key_tan",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onFunction(if (isInverse) "tan⁻¹(" else "tan(") },
          ),
          KeySpec(
            label = "±",
            testTag = "key_sign",
            style = KeyStyle.SCIENTIFIC,
            onClick = onToggleSign,
          ),
        ),
        listOf(
          KeySpec(
            label = if (isInverse) "eˣ" else "ln",
            testTag = "key_ln",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onFunction(if (isInverse) "exp(" else "ln(") },
          ),
          KeySpec(
            label = if (isInverse) "10ˣ" else "log",
            testTag = "key_log",
            style = KeyStyle.SCIENTIFIC,
            onClick = {
              if (isInverse) onFunction("10^(") else onFunction("log(")
            },
          ),
          KeySpec(
            label = if (isInverse) "x²" else "√",
            testTag = "key_sqrt",
            style = KeyStyle.SCIENTIFIC,
            onClick = { if (isInverse) onSquare() else onFunction("√(") },
          ),
          KeySpec(
            label = "xʸ",
            testTag = "key_pow",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onOperator('^') },
          ),
          KeySpec(
            label = "1/x",
            testTag = "key_reciprocal",
            style = KeyStyle.SCIENTIFIC,
            onClick = onReciprocal,
          ),
        ),
        listOf(
          KeySpec(
            label = "π",
            testTag = "key_pi",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onConstant("π") },
          ),
          KeySpec(
            label = "e",
            testTag = "key_e",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onConstant("e") },
          ),
          KeySpec(
            label = "n!",
            testTag = "key_factorial",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onPostfix('!') },
          ),
          KeySpec(
            label = "|x|",
            testTag = "key_abs",
            style = KeyStyle.SCIENTIFIC,
            onClick = { onFunction("abs(") },
          ),
          KeySpec(
            label = "x²",
            testTag = "key_square",
            style = KeyStyle.SCIENTIFIC,
            onClick = onSquare,
          ),
        ),
      )

    Surface(
      shape = RoundedCornerShape(22.dp),
      color = palette.keypadDeck,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        rows.forEach { row ->
          Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
          ) {
            row.forEach { key ->
              CalcKeyButton(
                spec = key,
                shape = RoundedCornerShape(14.dp),
                fontSize = 14.sp,
                useMonoFont = true,
                elevation = 0.dp,
                modifier = Modifier.weight(1f).fillMaxHeight(),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun StandardKeypadSection(
  onDigit: (Char) -> Unit,
  onDecimal: () -> Unit,
  onOperator: (Char) -> Unit,
  onParenthesis: () -> Unit,
  onPercent: () -> Unit,
  onClearAll: () -> Unit,
  onBackspace: () -> Unit,
  onEquals: () -> Unit,
  fillAvailableHeight: Boolean = false,
  maxGridHeight: Dp = Dp.Unspecified,
  modifier: Modifier = Modifier,
) {
  val backspaceDesc = stringResource(R.string.cd_backspace)
  val rows =
    listOf(
      listOf(
        KeySpec("AC", "key_clear", KeyStyle.CLEAR, onClick = onClearAll),
        KeySpec("( )", "key_paren", KeyStyle.UTILITY, onClick = onParenthesis),
        KeySpec("%", "key_percent", KeyStyle.UTILITY, onClick = onPercent),
        KeySpec("÷", "key_divide", KeyStyle.OPERATOR, onClick = { onOperator('÷') }),
      ),
      listOf(
        KeySpec("7", "key_7", KeyStyle.NUMBER, onClick = { onDigit('7') }),
        KeySpec("8", "key_8", KeyStyle.NUMBER, onClick = { onDigit('8') }),
        KeySpec("9", "key_9", KeyStyle.NUMBER, onClick = { onDigit('9') }),
        KeySpec("×", "key_multiply", KeyStyle.OPERATOR, onClick = { onOperator('×') }),
      ),
      listOf(
        KeySpec("4", "key_4", KeyStyle.NUMBER, onClick = { onDigit('4') }),
        KeySpec("5", "key_5", KeyStyle.NUMBER, onClick = { onDigit('5') }),
        KeySpec("6", "key_6", KeyStyle.NUMBER, onClick = { onDigit('6') }),
        KeySpec("−", "key_minus", KeyStyle.OPERATOR, onClick = { onOperator('−') }),
      ),
      listOf(
        KeySpec("1", "key_1", KeyStyle.NUMBER, onClick = { onDigit('1') }),
        KeySpec("2", "key_2", KeyStyle.NUMBER, onClick = { onDigit('2') }),
        KeySpec("3", "key_3", KeyStyle.NUMBER, onClick = { onDigit('3') }),
        KeySpec("+", "key_plus", KeyStyle.OPERATOR, onClick = { onOperator('+') }),
      ),
      listOf(
        KeySpec("0", "key_0", KeyStyle.NUMBER, onClick = { onDigit('0') }),
        KeySpec(".", "key_decimal", KeyStyle.NUMBER, onClick = onDecimal),
        KeySpec(
          label = "",
          testTag = "key_backspace",
          style = KeyStyle.UTILITY,
          icon = Icons.AutoMirrored.Rounded.Backspace,
          contentDesc = backspaceDesc,
          onClick = onBackspace,
        ),
        KeySpec("=", "key_equals", KeyStyle.EQUALS, onClick = onEquals),
      ),
    )

  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    val gap = 11.dp
    val colWidth = (maxWidth - gap * 3) / 4f
    val idealRowHeight = (colWidth * 0.86f).coerceIn(54.dp, 78.dp)
    val rowHeight =
      when {
        fillAvailableHeight && maxHeight != Dp.Infinity ->
          ((maxHeight - gap * 4) / 5f).coerceAtMost(colWidth * 0.94f).coerceAtLeast(48.dp)
        maxGridHeight != Dp.Unspecified ->
          ((maxGridHeight - gap * 4) / 5f).coerceAtMost(idealRowHeight).coerceAtLeast(48.dp)
        else -> idealRowHeight
      }

    // Pill/circle hybrid shape that stays sculptural at any phone width
    val keyShape = RoundedCornerShape(percent = 50)
    val digitFontSize = if (rowHeight >= 68.dp) 26.sp else 23.sp
    val opFontSize = if (rowHeight >= 68.dp) 29.sp else 26.sp

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(gap),
    ) {
      rows.forEach { row ->
        Row(
          modifier = Modifier.fillMaxWidth().height(rowHeight).heightIn(min = 48.dp),
          horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
          row.forEach { key ->
            val fontSize =
              when (key.style) {
                KeyStyle.OPERATOR,
                KeyStyle.EQUALS -> opFontSize
                KeyStyle.CLEAR -> (digitFontSize.value - 4f).sp
                KeyStyle.UTILITY -> (digitFontSize.value - 3f).sp
                else -> digitFontSize
              }
            CalcKeyButton(
              spec = key,
              shape = keyShape,
              fontSize = fontSize,
              useMonoFont = false,
              elevation = if (key.style == KeyStyle.EQUALS) 6.dp else 2.dp,
              modifier = Modifier.weight(1f).fillMaxHeight(),
            )
          }
        }
      }
    }
  }
}

@Composable
fun CalcKeyButton(
  spec: KeySpec,
  shape: Shape,
  fontSize: TextUnit,
  useMonoFont: Boolean,
  elevation: Dp = 2.dp,
  modifier: Modifier = Modifier,
) {
  val palette = LocalCalculatorPalette.current
  val haptic = LocalHapticFeedback.current
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by
    animateFloatAsState(
      targetValue = if (isPressed) 0.92f else 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessHigh,
        ),
      label = "keyScale",
    )

  val topColor: Color
  val bottomColor: Color
  val borderTop: Color
  val borderBottom: Color
  val contentColor: Color
  val shadowColor: Color

  when (spec.style) {
    KeyStyle.NUMBER -> {
      topColor = palette.numberKey
      bottomColor = palette.numberKeyBottom
      borderTop = palette.numberKeyBorder
      borderBottom = palette.numberKeyBorderBottom
      contentColor = palette.numberKeyText
      shadowColor = if (palette.isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.14f)
    }
    KeyStyle.UTILITY -> {
      topColor = palette.utilityKey
      bottomColor = palette.utilityKeyBottom
      borderTop = palette.utilityKeyBorder
      borderBottom = palette.utilityKeyBottom
      contentColor = palette.utilityKeyText
      shadowColor = if (palette.isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.10f)
    }
    KeyStyle.OPERATOR -> {
      topColor = palette.operatorKey
      bottomColor = palette.operatorKeyBottom
      borderTop = palette.operatorKeyBorder
      borderBottom = palette.operatorKeyBottom
      contentColor = palette.operatorKeyText
      shadowColor = if (palette.isDark) Color.Black.copy(alpha = 0.35f) else palette.equalsKey.copy(alpha = 0.18f)
    }
    KeyStyle.CLEAR -> {
      topColor = palette.clearKey
      bottomColor = palette.clearKeyBottom
      borderTop = palette.clearKeyBorder
      borderBottom = palette.clearKeyBottom
      contentColor = palette.clearKeyText
      shadowColor = if (palette.isDark) Color.Black.copy(alpha = 0.35f) else palette.clearKeyText.copy(alpha = 0.14f)
    }
    KeyStyle.EQUALS -> {
      topColor = palette.equalsKey
      bottomColor = palette.equalsKeyBottom
      borderTop = Color.White.copy(alpha = 0.32f)
      borderBottom = palette.equalsKeyBottom
      contentColor = palette.equalsKeyText
      shadowColor = palette.equalsKey.copy(alpha = if (palette.isDark) 0.55f else 0.40f)
    }
    KeyStyle.SCIENTIFIC -> {
      topColor = palette.scientificKey
      bottomColor = palette.scientificKey
      borderTop = palette.scientificKeyBorder
      borderBottom = palette.scientificKeyBorder
      contentColor = palette.scientificKeyText
      shadowColor = Color.Transparent
    }
    KeyStyle.SCIENTIFIC_ACTIVE -> {
      topColor = palette.operatorKey
      bottomColor = palette.operatorKeyBottom
      borderTop = palette.operatorKeyBorder
      borderBottom = palette.operatorKeyBorder
      contentColor = palette.operatorKeyText
      shadowColor = Color.Transparent
    }
  }

  val backgroundBrush = Brush.verticalGradient(colors = listOf(topColor, bottomColor))
  val borderBrush = Brush.verticalGradient(colors = listOf(borderTop, borderBottom))

  Box(
    modifier =
      modifier
        .scale(scale)
        .then(
          if (elevation > 0.dp) {
            Modifier.shadow(
              elevation = elevation,
              shape = shape,
              ambientColor = shadowColor,
              spotColor = shadowColor,
            )
          } else {
            Modifier
          }
        )
        .clip(shape)
        .background(brush = backgroundBrush)
        .border(width = 0.85.dp, brush = borderBrush, shape = shape)
        .clickable(
          interactionSource = interactionSource,
          indication = ripple(color = contentColor.copy(alpha = 0.22f)),
          role = Role.Button,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            spec.onClick()
          },
        )
        .testTag(spec.testTag)
        .semantics {
          contentDescription = spec.contentDesc ?: spec.label
        },
    contentAlignment = Alignment.Center,
  ) {
    if (spec.icon != null) {
      Icon(
        imageVector = spec.icon,
        contentDescription = spec.contentDesc,
        tint = contentColor,
        modifier = Modifier.size(23.dp),
      )
    } else {
      Text(
        text = spec.label,
        color = contentColor,
        fontFamily = if (useMonoFont) JetBrainsMonoFontFamily else SpaceGroteskFontFamily,
        fontWeight =
          when (spec.style) {
            KeyStyle.EQUALS,
            KeyStyle.OPERATOR,
            KeyStyle.CLEAR -> FontWeight.SemiBold
            KeyStyle.SCIENTIFIC,
            KeyStyle.SCIENTIFIC_ACTIVE -> FontWeight.Medium
            else -> FontWeight.Medium
          },
        fontSize = fontSize,
        maxLines = 1,
        style = MaterialTheme.typography.headlineSmall.copy(fontFeatureSettings = "tnum"),
      )
    }
  }
}
