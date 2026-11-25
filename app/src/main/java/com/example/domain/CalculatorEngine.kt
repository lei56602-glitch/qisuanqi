package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

enum class AngleUnit {
  DEG,
  RAD,
}

enum class CalcErrorType {
  DIVIDE_BY_ZERO,
  DOMAIN,
  OVERFLOW,
  SYNTAX,
}

sealed interface EvalResult {
  data object Empty : EvalResult

  data class Success(
    val value: BigDecimal,
    val rawText: String,
    val displayText: String,
  ) : EvalResult

  data class Error(val type: CalcErrorType) : EvalResult
}

private class CalcException(val errorType: CalcErrorType) : RuntimeException()

object CalculatorEngine {
  private val MC = MathContext(34, RoundingMode.HALF_UP)
  private val DISPLAY_MC = MathContext(12, RoundingMode.HALF_UP)
  private val BD_PI = BigDecimal("3.1415926535897932384626433832795029")
  private val BD_E = BigDecimal("2.7182818284590452353602874713526625")
  private val BD_100 = BigDecimal("100")
  private val BD_180 = BigDecimal("180")
  private val MAX_SCI_THRESHOLD = BigDecimal("1e12")
  private val MIN_SCI_THRESHOLD = BigDecimal("1e-9")

  private sealed interface Token {
    data class Number(val value: BigDecimal) : Token

    data class Constant(val name: String) : Token

    data class Func(val name: String) : Token

    data class Op(val symbol: Char) : Token

    data object LParen : Token

    data object RParen : Token

    data object Percent : Token

    data object Factorial : Token
  }

  fun evaluate(
    expression: String,
    angleUnit: AngleUnit,
    isPreview: Boolean = false,
  ): EvalResult {
    val trimmed = expression.trim()
    if (trimmed.isEmpty()) return EvalResult.Empty

    return try {
      val tokens = tokenize(trimmed)
      if (tokens.isEmpty()) return EvalResult.Empty

      if (isPreview) {
        val last = tokens.last()
        if (last is Token.Op || last is Token.LParen || last is Token.Func) {
          return EvalResult.Empty
        }
        // Don't show redundant preview for a single plain number or constant
        if (tokens.size == 1 && (last is Token.Number || last is Token.Constant)) {
          return EvalResult.Empty
        }
        if (tokens.size == 2 && tokens[0] == Token.Op('-') && tokens[1] is Token.Number) {
          return EvalResult.Empty
        }
      }

      val balancedTokens = autoCloseParens(tokens)
      val expandedTokens = insertImplicitMultiplication(balancedTokens)
      val parser = Parser(expandedTokens, angleUnit)
      val result = parser.parse()

      val normalized = normalizeResult(result)
      val rawText = toRawString(normalized)
      val displayText = formatForDisplay(normalized)
      EvalResult.Success(value = normalized, rawText = rawText, displayText = displayText)
    } catch (e: CalcException) {
      if (isPreview && e.errorType == CalcErrorType.SYNTAX) {
        EvalResult.Empty
      } else {
        EvalResult.Error(e.errorType)
      }
    } catch (_: ArithmeticException) {
      EvalResult.Error(CalcErrorType.OVERFLOW)
    } catch (_: Exception) {
      if (isPreview) EvalResult.Empty else EvalResult.Error(CalcErrorType.SYNTAX)
    }
  }

  private fun tokenize(input: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    while (i < input.length) {
      val ch = input[i]
      when {
        ch.isWhitespace() || ch == ',' -> i++
        ch.isDigit() || ch == '.' -> {
          val start = i
          var hasDot = false
          while (i < input.length && (input[i].isDigit() || input[i] == '.')) {
            if (input[i] == '.') {
              if (hasDot) throw CalcException(CalcErrorType.SYNTAX)
              hasDot = true
            }
            i++
          }
          // Support scientific notation inside numbers like 1.5E10 or 1.5E-4 (uppercase E only; lowercase e is Euler's constant)
          if (i < input.length && input[i] == 'E') {
            val next = input.getOrNull(i + 1)
            val afterNext = input.getOrNull(i + 2)
            val isExpNumber =
              (next != null && next.isDigit()) ||
                ((next == '+' || next == '-' || next == '−') &&
                  afterNext != null &&
                  afterNext.isDigit())
            if (isExpNumber) {
              i++
              if (input[i] == '+' || input[i] == '-' || input[i] == '−') {
                i++
              }
              while (i < input.length && input[i].isDigit()) {
                i++
              }
            }
          }
          val numStr = input.substring(start, i).replace('−', '-')
          if (numStr == ".") throw CalcException(CalcErrorType.SYNTAX)
          val bd =
            try {
              BigDecimal(numStr, MC)
            } catch (_: NumberFormatException) {
              throw CalcException(CalcErrorType.SYNTAX)
            }
          tokens.add(Token.Number(bd))
        }
        ch == 'π' -> {
          tokens.add(Token.Constant("pi"))
          i++
        }
        ch == 'e' -> {
          // Check if it's "exp"
          if (input.startsWith("exp", i)) {
            tokens.add(Token.Func("exp"))
            i += 3
          } else {
            tokens.add(Token.Constant("e"))
            i++
          }
        }
        ch == '√' -> {
          tokens.add(Token.Func("sqrt"))
          i++
        }
        ch == '(' -> {
          tokens.add(Token.LParen)
          i++
        }
        ch == ')' -> {
          tokens.add(Token.RParen)
          i++
        }
        ch == '%' -> {
          tokens.add(Token.Percent)
          i++
        }
        ch == '!' -> {
          tokens.add(Token.Factorial)
          i++
        }
        ch == '+' -> {
          tokens.add(Token.Op('+'))
          i++
        }
        ch == '-' || ch == '−' -> {
          tokens.add(Token.Op('-'))
          i++
        }
        ch == '*' || ch == '×' -> {
          tokens.add(Token.Op('*'))
          i++
        }
        ch == '/' || ch == '÷' -> {
          tokens.add(Token.Op('/'))
          i++
        }
        ch == '^' -> {
          tokens.add(Token.Op('^'))
          i++
        }
        ch.isLetter() -> {
          val start = i
          while (i < input.length && (input[i].isLetter() || input[i].isDigit() || input[i] == '⁻' || input[i] == '¹')) {
            i++
          }
          val word = input.substring(start, i).lowercase()
          when (word) {
            "pi" -> tokens.add(Token.Constant("pi"))
            "sin",
            "cos",
            "tan",
            "asin",
            "acos",
            "atan",
            "ln",
            "log",
            "sqrt",
            "abs",
            "exp" -> tokens.add(Token.Func(word))
            "sin⁻¹" -> tokens.add(Token.Func("asin"))
            "cos⁻¹" -> tokens.add(Token.Func("acos"))
            "tan⁻¹" -> tokens.add(Token.Func("atan"))
            else -> throw CalcException(CalcErrorType.SYNTAX)
          }
        }
        else -> throw CalcException(CalcErrorType.SYNTAX)
      }
    }
    return tokens
  }

  private fun autoCloseParens(tokens: List<Token>): List<Token> {
    var openCount = 0
    for (token in tokens) {
      when (token) {
        Token.LParen -> openCount++
        Token.RParen -> openCount--
        else -> {}
      }
      if (openCount < 0) throw CalcException(CalcErrorType.SYNTAX)
    }
    if (openCount == 0) return tokens
    return tokens + List(openCount) { Token.RParen }
  }

  private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
    if (tokens.size < 2) return tokens
    val result = mutableListOf<Token>()
    for (idx in tokens.indices) {
      val current = tokens[idx]
      if (idx > 0) {
        val prev = tokens[idx - 1]
        val prevEndsValue =
          prev is Token.Number ||
            prev is Token.Constant ||
            prev is Token.RParen ||
            prev is Token.Percent ||
            prev is Token.Factorial
        val currStartsValue =
          current is Token.Number ||
            current is Token.Constant ||
            current is Token.LParen ||
            current is Token.Func
        if (prevEndsValue && currStartsValue) {
          result.add(Token.Op('*'))
        }
      }
      result.add(current)
    }
    return result
  }

  private class Parser(
    private val tokens: List<Token>,
    private val angleUnit: AngleUnit,
  ) {
    private var pos = 0

    fun parse(): BigDecimal {
      val value = parseExpression()
      if (pos < tokens.size) throw CalcException(CalcErrorType.SYNTAX)
      return value
    }

    private fun peek(): Token? = tokens.getOrNull(pos)

    private fun consume(): Token = tokens[pos++]

    private fun parseExpression(): BigDecimal {
      var left = parseTerm()
      while (true) {
        val token = peek()
        if (token is Token.Op && (token.symbol == '+' || token.symbol == '-')) {
          consume()
          val right = parseTerm()
          left =
            if (token.symbol == '+') {
              left.add(right, MC)
            } else {
              left.subtract(right, MC)
            }
        } else {
          break
        }
      }
      return left
    }

    private fun parseTerm(): BigDecimal {
      var left = parseUnary()
      while (true) {
        val token = peek()
        if (token is Token.Op && (token.symbol == '*' || token.symbol == '/')) {
          consume()
          val right = parseUnary()
          left =
            if (token.symbol == '*') {
              left.multiply(right, MC)
            } else {
              if (right.compareTo(BigDecimal.ZERO) == 0) {
                throw CalcException(CalcErrorType.DIVIDE_BY_ZERO)
              }
              left.divide(right, MC)
            }
        } else {
          break
        }
      }
      return left
    }

    private fun parseUnary(): BigDecimal {
      val token = peek()
      if (token is Token.Op && (token.symbol == '+' || token.symbol == '-')) {
        consume()
        val value = parseUnary()
        return if (token.symbol == '-') value.negate(MC) else value
      }
      return parsePower()
    }

    private fun parsePower(): BigDecimal {
      val base = parsePostfix()
      val token = peek()
      if (token is Token.Op && token.symbol == '^') {
        consume()
        val exponent = parseUnary()
        return computePower(base, exponent)
      }
      return base
    }

    private fun parsePostfix(): BigDecimal {
      var value = parsePrimary()
      while (true) {
        when (peek()) {
          Token.Percent -> {
            consume()
            value = value.divide(BD_100, MC)
          }
          Token.Factorial -> {
            consume()
            value = computeFactorial(value)
          }
          else -> break
        }
      }
      return value
    }

    private fun parsePrimary(): BigDecimal {
      val token = peek() ?: throw CalcException(CalcErrorType.SYNTAX)
      return when (token) {
        is Token.Number -> {
          consume()
          token.value
        }
        is Token.Constant -> {
          consume()
          when (token.name) {
            "pi" -> BD_PI
            "e" -> BD_E
            else -> throw CalcException(CalcErrorType.SYNTAX)
          }
        }
        Token.LParen -> {
          consume()
          val inner = parseExpression()
          if (peek() != Token.RParen) throw CalcException(CalcErrorType.SYNTAX)
          consume()
          inner
        }
        is Token.Func -> {
          consume()
          val arg =
            if (peek() == Token.LParen) {
              consume()
              val inner = parseExpression()
              if (peek() != Token.RParen) throw CalcException(CalcErrorType.SYNTAX)
              consume()
              inner
            } else {
              parseUnary()
            }
          applyFunction(token.name, arg)
        }
        else -> throw CalcException(CalcErrorType.SYNTAX)
      }
    }

    private fun applyFunction(name: String, arg: BigDecimal): BigDecimal {
      val d = arg.toDouble()
      return when (name) {
        "sqrt" -> {
          if (arg < BigDecimal.ZERO) throw CalcException(CalcErrorType.DOMAIN)
          bigDecimalSqrt(arg)
        }
        "abs" -> arg.abs()
        "ln" -> {
          if (d <= 0.0 || d.isInfinite() || d.isNaN()) throw CalcException(CalcErrorType.DOMAIN)
          fromDouble(ln(d))
        }
        "log" -> {
          if (d <= 0.0 || d.isInfinite() || d.isNaN()) throw CalcException(CalcErrorType.DOMAIN)
          fromDouble(log10(d))
        }
        "exp" -> {
          val res = exp(d)
          fromDouble(res)
        }
        "sin" -> {
          if (angleUnit == AngleUnit.DEG) {
            val mod180 = ((d % 180.0) + 180.0) % 180.0
            if (abs(mod180) < 1e-12 || abs(mod180 - 180.0) < 1e-12) return BigDecimal.ZERO
            fromDouble(sin(Math.toRadians(d)))
          } else {
            fromDouble(sin(d))
          }
        }
        "cos" -> {
          if (angleUnit == AngleUnit.DEG) {
            val mod180 = ((d % 180.0) + 180.0) % 180.0
            if (abs(mod180 - 90.0) < 1e-12) return BigDecimal.ZERO
            fromDouble(cos(Math.toRadians(d)))
          } else {
            fromDouble(cos(d))
          }
        }
        "tan" -> {
          if (angleUnit == AngleUnit.DEG) {
            val mod180 = ((d % 180.0) + 180.0) % 180.0
            if (abs(mod180 - 90.0) < 1e-12) throw CalcException(CalcErrorType.DOMAIN)
            if (abs(mod180) < 1e-12 || abs(mod180 - 180.0) < 1e-12) return BigDecimal.ZERO
            fromDouble(tan(Math.toRadians(d)))
          } else {
            val c = cos(d)
            if (abs(c) < 1e-14) throw CalcException(CalcErrorType.DOMAIN)
            fromDouble(tan(d))
          }
        }
        "asin" -> {
          if (d < -1.0 || d > 1.0) throw CalcException(CalcErrorType.DOMAIN)
          val rad = asin(d)
          fromDouble(if (angleUnit == AngleUnit.DEG) Math.toDegrees(rad) else rad)
        }
        "acos" -> {
          if (d < -1.0 || d > 1.0) throw CalcException(CalcErrorType.DOMAIN)
          val rad = acos(d)
          fromDouble(if (angleUnit == AngleUnit.DEG) Math.toDegrees(rad) else rad)
        }
        "atan" -> {
          val rad = atan(d)
          fromDouble(if (angleUnit == AngleUnit.DEG) Math.toDegrees(rad) else rad)
        }
        else -> throw CalcException(CalcErrorType.SYNTAX)
      }
    }

    private fun computePower(base: BigDecimal, exponent: BigDecimal): BigDecimal {
      if (base.compareTo(BigDecimal.ZERO) == 0 && exponent < BigDecimal.ZERO) {
        throw CalcException(CalcErrorType.DIVIDE_BY_ZERO)
      }
      if (base.compareTo(BigDecimal.ZERO) == 0 && exponent.compareTo(BigDecimal.ZERO) == 0) {
        return BigDecimal.ONE
      }

      val strippedExp = exponent.stripTrailingZeros()
      if (strippedExp.scale() <= 0) {
        try {
          val intExp = strippedExp.intValueExact()
          if (intExp in -999..999) {
            return base.pow(intExp, MC)
          }
        } catch (_: ArithmeticException) {
          // Fall through to double pow
        }
      }

      val b = base.toDouble()
      val e = exponent.toDouble()
      if (b < 0 && strippedExp.scale() > 0) {
        throw CalcException(CalcErrorType.DOMAIN)
      }
      val res = b.pow(e)
      return fromDouble(res)
    }

    private fun computeFactorial(value: BigDecimal): BigDecimal {
      val stripped = value.stripTrailingZeros()
      if (stripped < BigDecimal.ZERO || stripped.scale() > 0) {
        throw CalcException(CalcErrorType.DOMAIN)
      }
      val n =
        try {
          stripped.intValueExact()
        } catch (_: ArithmeticException) {
          throw CalcException(CalcErrorType.OVERFLOW)
        }
      if (n > 170) throw CalcException(CalcErrorType.OVERFLOW)
      var acc = BigDecimal.ONE
      for (k in 2..n) {
        acc = acc.multiply(BigDecimal.valueOf(k.toLong()), MC)
      }
      return acc
    }

    private fun bigDecimalSqrt(x: BigDecimal): BigDecimal {
      if (x.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO
      val initial = sqrt(x.toDouble())
      if (initial.isNaN() || initial.isInfinite()) {
        throw CalcException(CalcErrorType.OVERFLOW)
      }
      var current = BigDecimal.valueOf(initial)
      val two = BigDecimal.valueOf(2)
      repeat(8) {
        val next = current.add(x.divide(current, MC), MC).divide(two, MC)
        if (next.compareTo(current) == 0) return next
        current = next
      }
      return current
    }

    private fun fromDouble(value: Double): BigDecimal {
      if (value.isNaN()) throw CalcException(CalcErrorType.DOMAIN)
      if (value.isInfinite()) throw CalcException(CalcErrorType.OVERFLOW)
      if (abs(value) < 1e-14) return BigDecimal.ZERO
      // Snap values super close to integers (e.g. 0.49999999999999994 or 2.0000000000000004)
      val rounded = value.roundToLong()
      if (abs(value - rounded) < 1e-13 && abs(value) < 1e15) {
        return BigDecimal.valueOf(rounded)
      }
      return BigDecimal(value.toString(), MC)
    }
  }

  private fun normalizeResult(value: BigDecimal): BigDecimal {
    val rounded = value.round(DISPLAY_MC)
    if (rounded.abs() < BigDecimal("1e-13")) return BigDecimal.ZERO
    val stripped = rounded.stripTrailingZeros()
    return if (stripped.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else stripped
  }

  fun toRawString(value: BigDecimal): String {
    if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
    val absVal = value.abs()
    return if (absVal >= MAX_SCI_THRESHOLD || absVal < MIN_SCI_THRESHOLD) {
      value.round(MathContext(9, RoundingMode.HALF_UP)).stripTrailingZeros().toString()
    } else {
      value.toPlainString()
    }
  }

  fun formatForDisplay(value: BigDecimal): String {
    if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
    val absVal = value.abs()
    if (absVal >= MAX_SCI_THRESHOLD || absVal < MIN_SCI_THRESHOLD) {
      return value.round(MathContext(8, RoundingMode.HALF_UP)).stripTrailingZeros().toString()
    }
    val plain = value.toPlainString()
    val negative = plain.startsWith("-")
    val unsigned = if (negative) plain.substring(1) else plain
    val dotIdx = unsigned.indexOf('.')
    val intPart = if (dotIdx >= 0) unsigned.substring(0, dotIdx) else unsigned
    val fracPart = if (dotIdx >= 0) unsigned.substring(dotIdx) else ""
    val groupedInt = groupThousands(intPart)
    return buildString {
      if (negative) append('−')
      append(groupedInt)
      append(fracPart)
    }
  }

  private fun groupThousands(digits: String): String {
    if (digits.length <= 3) return digits
    val sb = StringBuilder(digits.length + digits.length / 3)
    var count = 0
    for (i in digits.length - 1 downTo 0) {
      sb.append(digits[i])
      count++
      if (count % 3 == 0 && i > 0) {
        sb.append(',')
      }
    }
    return sb.reverse().toString()
  }
}
