package com.example

import com.example.domain.AngleUnit
import com.example.domain.CalcErrorType
import com.example.domain.CalculatorEngine
import com.example.domain.EvalResult
import com.example.viewmodel.CalculatorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

  @Test
  fun `exact decimal addition avoids binary floating point drift`() {
    val result = CalculatorEngine.evaluate("0.1+0.2", AngleUnit.DEG)
    assertTrue(result is EvalResult.Success)
    assertEquals("0.3", (result as EvalResult.Success).displayText)
  }

  @Test
  fun `operator precedence and parentheses evaluate accurately`() {
    val r1 = CalculatorEngine.evaluate("2+3×4", AngleUnit.DEG) as EvalResult.Success
    assertEquals("14", r1.displayText)

    val r2 = CalculatorEngine.evaluate("(2+3)×4", AngleUnit.DEG) as EvalResult.Success
    assertEquals("20", r2.displayText)

    val r3 = CalculatorEngine.evaluate("2(3+4)", AngleUnit.DEG) as EvalResult.Success
    assertEquals("14", r3.displayText)

    val r4 = CalculatorEngine.evaluate("(2+3)(4+5)", AngleUnit.DEG) as EvalResult.Success
    assertEquals("45", r4.displayText)
  }

  @Test
  fun `powers and unary minus follow standard mathematical precedence`() {
    val r1 = CalculatorEngine.evaluate("2^3^2", AngleUnit.DEG) as EvalResult.Success
    assertEquals("512", r1.displayText)

    val r2 = CalculatorEngine.evaluate("−2^2", AngleUnit.DEG) as EvalResult.Success
    assertEquals("−4", r2.displayText)

    val r3 = CalculatorEngine.evaluate("(−2)^2", AngleUnit.DEG) as EvalResult.Success
    assertEquals("4", r3.displayText)
  }

  @Test
  fun `trigonometric functions respect DEG and RAD modes`() {
    val sinDeg = CalculatorEngine.evaluate("sin(30)", AngleUnit.DEG) as EvalResult.Success
    assertEquals("0.5", sinDeg.displayText)

    val cos90 = CalculatorEngine.evaluate("cos(90)", AngleUnit.DEG) as EvalResult.Success
    assertEquals("0", cos90.displayText)

    val tan90 = CalculatorEngine.evaluate("tan(90)", AngleUnit.DEG)
    assertEquals(EvalResult.Error(CalcErrorType.DOMAIN), tan90)

    val sinRad = CalculatorEngine.evaluate("sin(π÷2)", AngleUnit.RAD) as EvalResult.Success
    assertEquals("1", sinRad.displayText)
  }

  @Test
  fun `factorial percentage and sqrt work cleanly`() {
    val fact = CalculatorEngine.evaluate("5!", AngleUnit.DEG) as EvalResult.Success
    assertEquals("120", fact.displayText)

    val pct = CalculatorEngine.evaluate("200×15%", AngleUnit.DEG) as EvalResult.Success
    assertEquals("30", pct.displayText)

    val root = CalculatorEngine.evaluate("√(144)", AngleUnit.DEG) as EvalResult.Success
    assertEquals("12", root.displayText)
  }

  @Test
  fun `division by zero returns DIVIDE_BY_ZERO error`() {
    val res = CalculatorEngine.evaluate("42÷0", AngleUnit.DEG)
    assertEquals(EvalResult.Error(CalcErrorType.DIVIDE_BY_ZERO), res)
  }

  @Test
  fun `viewModel records history and removes function token on backspace`() {
    val vm = CalculatorViewModel()
    vm.onFunction("sin(")
    assertEquals("sin(", vm.uiState.value.expression)
    vm.onBackspace()
    assertEquals("", vm.uiState.value.expression)

    vm.onDigit('1')
    vm.onDigit('2')
    vm.onDigit('5')
    vm.onOperator('×')
    vm.onDigit('8')
    assertEquals("1,000", vm.uiState.value.livePreview)
    vm.onEquals()

    assertEquals("1000", vm.uiState.value.expression)
    assertEquals("1,000", vm.uiState.value.formattedExpression)
    assertEquals(1, vm.uiState.value.history.size)
    assertEquals("125×8", vm.uiState.value.history.first().expression)
  }
}
