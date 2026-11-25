package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.domain.AngleUnit
import com.example.domain.CalcErrorType
import com.example.domain.CalculatorEngine
import com.example.domain.EvalResult
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalculationRecord(
  val id: Long,
  val expression: String,
  val formattedExpression: String,
  val resultRaw: String,
  val resultDisplay: String,
)

data class CalculatorUiState(
  val expression: String = "",
  val formattedExpression: String = "",
  val livePreview: String = "",
  val errorType: CalcErrorType? = null,
  val justEvaluated: Boolean = false,
  val isScientific: Boolean = false,
  val isInverse: Boolean = false,
  val angleUnit: AngleUnit = AngleUnit.DEG,
  val themeMode: ThemeMode = ThemeMode.DARK,
  val history: List<CalculationRecord> = emptyList(),
)

class CalculatorViewModel : ViewModel() {
  private val _uiState = MutableStateFlow(CalculatorUiState())
  val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

  private var nextHistoryId = 1L

  private val functionTokens =
    listOf(
      "sin⁻¹(",
      "cos⁻¹(",
      "tan⁻¹(",
      "sin(",
      "cos(",
      "tan(",
      "exp(",
      "abs(",
      "log(",
      "ln(",
      "√(",
    )

  private val binaryOps = setOf('+', '−', '×', '÷', '^')

  fun onDigit(digit: Char) {
    if (digit !in '0'..'9') return
    _uiState.update { state ->
      val base = if (state.justEvaluated) "" else state.expression
      val updated = appendDigit(base, digit)
      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  private fun appendDigit(expr: String, digit: Char): String {
    if (expr.isEmpty()) return digit.toString()
    // Prevent leading zeros like 05; replace single 0 after empty or operator/paren with digit
    val lastNumberStart = findTrailingNumberStart(expr)
    val trailingNum = expr.substring(lastNumberStart)
    if (trailingNum == "0") {
      return expr.substring(0, lastNumberStart) + digit
    }
    return expr + digit
  }

  fun onDecimal() {
    _uiState.update { state ->
      val base = if (state.justEvaluated) "" else state.expression
      val updated =
        if (base.isEmpty()) {
          "0."
        } else {
          val lastChar = base.last()
          when {
            lastChar in binaryOps || lastChar == '(' -> base + "0."
            lastChar == ')' || lastChar == 'π' || lastChar == 'e' || lastChar == '%' || lastChar == '!' ->
              base + "×0."
            else -> {
              val start = findTrailingNumberStart(base)
              val trailing = base.substring(start)
              if (trailing.contains('.') || trailing.contains('E')) base else "$base."
            }
          }
        }
      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  fun onOperator(op: Char) {
    if (op !in binaryOps) return
    _uiState.update { state ->
      val expr = state.expression
      if (expr.isEmpty()) {
        if (op == '−') {
          return@update recomputePreview(
            state.copy(expression = "−", justEvaluated = false, errorType = null)
          )
        }
        return@update state
      }

      val last = expr.last()
      val updated =
        when {
          last == '(' -> {
            if (op == '−') "$expr−" else expr
          }
          last in binaryOps -> {
            // Allow negative exponent or negative multiplier like 5×−3 or 2^−3
            if (op == '−' && (last == '×' || last == '÷' || last == '^')) {
              "$expr−"
            } else if (expr.length >= 2 && last == '−' && expr[expr.length - 2] in setOf('×', '÷', '^', '(')) {
              if (op == '−') expr else expr.dropLast(2) + op
            } else if (expr == "−") {
              if (op == '−') expr else ""
            } else {
              expr.dropLast(1) + op
            }
          }
          last == '.' -> expr.dropLast(1) + op
          else -> expr + op
        }

      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  fun onParenthesis() {
    _uiState.update { state ->
      val base = if (state.justEvaluated) "" else state.expression
      val openCount = base.count { it == '(' } - base.count { it == ')' }
      val last = base.lastOrNull()
      val canClose =
        openCount > 0 &&
          last != null &&
          (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!')
      val updated = if (canClose) "$base)" else "$base("
      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  fun onConstant(constant: String) {
    _uiState.update { state ->
      val base = if (state.justEvaluated) "" else state.expression
      val updated = base + constant
      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  fun onFunction(funcPrefix: String) {
    _uiState.update { state ->
      val updated =
        if (state.justEvaluated && state.expression.isNotEmpty()) {
          "$funcPrefix${state.expression})"
        } else {
          state.expression + funcPrefix
        }
      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  fun onPostfix(symbol: Char) {
    if (symbol != '%' && symbol != '!') return
    _uiState.update { state ->
      val expr = state.expression
      val last = expr.lastOrNull() ?: return@update state
      if (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!') {
        recomputePreview(
          state.copy(expression = expr + symbol, justEvaluated = false, errorType = null)
        )
      } else {
        state
      }
    }
  }

  fun onSquare() {
    _uiState.update { state ->
      val expr = state.expression
      val last = expr.lastOrNull() ?: return@update state
      if (last.isDigit() || last == ')' || last == 'π' || last == 'e' || last == '%' || last == '!') {
        recomputePreview(
          state.copy(expression = "$expr^2", justEvaluated = false, errorType = null)
        )
      } else {
        state
      }
    }
  }

  fun onReciprocal() {
    _uiState.update { state ->
      val expr = state.expression
      if (expr.isEmpty()) return@update state
      val updated =
        if (state.justEvaluated) {
          "1÷($expr)"
        } else {
          val last = expr.last()
          if (last.isDigit() || last == ')' || last == 'π' || last == 'e') {
            val start = findTrailingNumberStart(expr)
            if (start == 0) "1÷($expr)" else expr.substring(0, start) + "(1÷" + expr.substring(start) + ")"
          } else {
            expr
          }
        }
      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  fun onToggleSign() {
    _uiState.update { state ->
      val expr = state.expression
      if (expr.isEmpty()) {
        return@update recomputePreview(
          state.copy(expression = "−", justEvaluated = false, errorType = null)
        )
      }
      if (expr == "−") {
        return@update recomputePreview(
          state.copy(expression = "", justEvaluated = false, errorType = null)
        )
      }

      if (state.justEvaluated) {
        val updated = if (expr.startsWith("−")) expr.drop(1) else if (expr == "0") "0" else "−$expr"
        return@update recomputePreview(
          state.copy(expression = updated, justEvaluated = false, errorType = null)
        )
      }

      val last = expr.last()
      if (!last.isDigit() && last != '.' && last != 'π' && last != 'e') {
        val updated =
          if (expr.endsWith("(−")) expr.dropLast(2)
          else if (last in binaryOps || last == '(') "$expr(−"
          else expr
        return@update recomputePreview(
          state.copy(expression = updated, justEvaluated = false, errorType = null)
        )
      }

      val start = findTrailingNumberStart(expr)
      val num = expr.substring(start)
      val updated =
        when {
          start >= 2 && expr.substring(start - 2, start) == "(−" -> {
            expr.substring(0, start - 2) + num
          }
          start == 1 && expr[0] == '−' -> num
          start == 0 -> "−$num"
          else -> expr.substring(0, start) + "(−$num"
        }
      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  fun onBackspace() {
    _uiState.update { state ->
      val expr = state.expression
      if (expr.isEmpty()) {
        return@update state.copy(errorType = null, livePreview = "")
      }
      if (state.justEvaluated) {
        return@update recomputePreview(
          state.copy(expression = "", justEvaluated = false, errorType = null)
        )
      }
      val matchedFunc = functionTokens.firstOrNull { expr.endsWith(it) }
      val updated =
        if (matchedFunc != null) {
          expr.dropLast(matchedFunc.length)
        } else {
          expr.dropLast(1)
        }
      recomputePreview(state.copy(expression = updated, justEvaluated = false, errorType = null))
    }
  }

  fun onClearAll() {
    _uiState.update { state ->
      state.copy(
        expression = "",
        formattedExpression = "",
        livePreview = "",
        errorType = null,
        justEvaluated = false,
      )
    }
  }

  fun onEquals() {
    _uiState.update { state ->
      val expr = state.expression
      if (expr.isBlank()) return@update state

      when (val result = CalculatorEngine.evaluate(expr, state.angleUnit, isPreview = false)) {
        is EvalResult.Empty -> state
        is EvalResult.Error -> state.copy(errorType = result.type, livePreview = "")
        is EvalResult.Success -> {
          val formattedExpr = formatExpressionForDisplay(expr)
          val newRecord =
            CalculationRecord(
              id = nextHistoryId++,
              expression = expr,
              formattedExpression = formattedExpr,
              resultRaw = result.rawText,
              resultDisplay = result.displayText,
            )
          val updatedHistory =
            if (state.history.firstOrNull()?.let {
                it.expression == newRecord.expression && it.resultRaw == newRecord.resultRaw
              } == true
            ) {
              state.history
            } else {
              listOf(newRecord) + state.history.take(99)
            }

          // Convert raw '-' to display '−' for expression continuity
          val editableResult = result.rawText.replace('-', '−')
          state.copy(
            expression = editableResult,
            formattedExpression = formatExpressionForDisplay(editableResult),
            livePreview = "",
            errorType = null,
            justEvaluated = true,
            history = updatedHistory,
          )
        }
      }
    }
  }

  fun onToggleScientific() {
    _uiState.update { it.copy(isScientific = !it.isScientific) }
  }

  fun onToggleInverse() {
    _uiState.update { it.copy(isInverse = !it.isInverse) }
  }

  fun onToggleAngleUnit() {
    _uiState.update { state ->
      val nextUnit = if (state.angleUnit == AngleUnit.DEG) AngleUnit.RAD else AngleUnit.DEG
      recomputePreview(state.copy(angleUnit = nextUnit))
    }
  }

  fun onCycleThemeMode() {
    _uiState.update { state ->
      val nextTheme =
        when (state.themeMode) {
          ThemeMode.DARK -> ThemeMode.LIGHT
          ThemeMode.LIGHT -> ThemeMode.DARK
          ThemeMode.SYSTEM -> ThemeMode.DARK
        }
      state.copy(themeMode = nextTheme)
    }
  }

  fun onSelectHistoryItem(record: CalculationRecord, useResultOnly: Boolean) {
    _uiState.update { state ->
      val valueToInsert =
        if (useResultOnly) record.resultRaw.replace('-', '−') else record.expression
      val updated =
        if (state.justEvaluated || state.expression.isEmpty()) {
          valueToInsert
        } else {
          val last = state.expression.last()
          if (last in binaryOps || last == '(') {
            state.expression + valueToInsert
          } else {
            valueToInsert
          }
        }
      recomputePreview(
        state.copy(
          expression = updated,
          justEvaluated = false,
          errorType = null,
        )
      )
    }
  }

  fun onClearHistory() {
    _uiState.update { it.copy(history = emptyList()) }
  }

  private fun recomputePreview(state: CalculatorUiState): CalculatorUiState {
    val formattedExpr = formatExpressionForDisplay(state.expression)
    val preview =
      when (val eval = CalculatorEngine.evaluate(state.expression, state.angleUnit, isPreview = true)) {
        is EvalResult.Success -> eval.displayText
        is EvalResult.Error -> ""
        EvalResult.Empty -> ""
      }
    return state.copy(formattedExpression = formattedExpr, livePreview = preview)
  }

  private fun findTrailingNumberStart(expr: String): Int {
    var i = expr.length - 1
    while (i >= 0) {
      val ch = expr[i]
      if (ch.isDigit() || ch == '.' || ch == 'E') {
        i--
      } else if ((ch == '-' || ch == '+' || ch == '−') && i > 0 && expr[i - 1] == 'E') {
        i--
      } else {
        break
      }
    }
    return i + 1
  }

  companion object {
    fun formatExpressionForDisplay(expr: String): String {
      if (expr.isEmpty()) return ""
      val sb = StringBuilder(expr.length + 8)
      var i = 0
      while (i < expr.length) {
        val ch = expr[i]
        if (ch.isDigit()) {
          val start = i
          while (i < expr.length && expr[i].isDigit()) {
            i++
          }
          val intDigits = expr.substring(start, i)
          // Check if preceded by '.' or 'E' (fractional or exponent part -> don't group)
          val prev = expr.getOrNull(start - 1)
          val inFractionOrExp =
            prev == '.' ||
              prev == 'E' ||
              ((prev == '-' || prev == '+' || prev == '−') && expr.getOrNull(start - 2) == 'E')
          if (inFractionOrExp || intDigits.length <= 3) {
            sb.append(intDigits)
          } else {
            sb.append(groupDigits(intDigits))
          }
        } else {
          sb.append(ch)
          i++
        }
      }
      return sb.toString()
    }

    private fun groupDigits(digits: String): String {
      val out = StringBuilder(digits.length + digits.length / 3)
      var count = 0
      for (idx in digits.length - 1 downTo 0) {
        out.append(digits[idx])
        count++
        if (count % 3 == 0 && idx > 0) out.append(',')
      }
      return out.reverse().toString()
    }
  }
}
