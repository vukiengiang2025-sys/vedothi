package com.example.math

import android.util.Log
import kotlin.math.*

class MathParser(expressionStr: String) {
    private val normalizedExpr: String

    init {
        this.normalizedExpr = preprocess(expressionStr)
        Log.d("MathParser", "Original: '$expressionStr' -> Preprocessed: '${this.normalizedExpr}'")
    }

    private fun preprocess(expr: String): String {
        var str = expr.replace("\\s+".toRegex(), "") // Remove all spaces
        str = str.replace("π", "pi")
        str = str.replace("PI", "pi")
        str = str.replace("E", "e")

        // Add implicit multiplication:
        // 1. Number next to variable/parenthesis: "2x" -> "2*x", "3(" -> "3*("
        str = str.replace("(\\d+(?:\\.\\d+)?)([a-zA-Z\\(])".toRegex(), "$1*$2")
        
        // 2. Variable next to parenthesis: "x(" -> "x*(" , "y(" -> "y*("
        str = str.replace("([xyXY])([\\(])".toRegex(), "$1*$2")
        
        // 3. Parenthesis next to variable/number/parenthesis: ")x" -> ")*x", ")3" -> ")*3", ")(" -> ")*("
        str = str.replace("(\\))([0-9a-zA-Z\\(])".toRegex(), "$1*$2")

        // 4. Consecutive variables: "xy" -> "x*y" (only if they aren't part of keywords like sin, cos, etc.)
        // To keep it simple and safe, we can manually change "xy" or "yx" to "x*y" since our variables are only x and y
        str = str.replace("xy", "x*y")
        str = str.replace("yx", "y*x")
        str = str.replace("xY", "x*y")
        str = str.replace("Yx", "y*x")
        str = str.replace("Xy", "x*y")
        str = str.replace("yX", "y*x")
        str = str.replace("XY", "x*y")
        str = str.replace("YX", "y*x")

        return str
    }

    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        pos++
        ch = if (pos < normalizedExpr.length) normalizedExpr[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) nextChar()
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(x: Double, y: Double = 0.0): Double {
        pos = -1
        ch = 0
        nextChar()
        val v = parseExpression(x, y)
        if (pos < normalizedExpr.length) {
            throw IllegalArgumentException("Kú pháp toán học sai tại vị trí $pos cho kí tự '" + normalizedExpr[pos] + "'")
        }
        return v
    }

    // Grammmer:
    // Expression = Term (+/- Term)*
    // Term = Factor (* / Factor)*
    // Factor = Power (^ Factor)*   (Right-associative)
    // Primary = + Primary | - Primary | ( Expression ) | Constant | Variable | Function(Expression)

    private fun parseExpression(x: Double, y: Double): Double {
        var v = parseTerm(x, y)
        while (true) {
            if (eat('+'.code)) v += parseTerm(x, y)
            else if (eat('-'.code)) v -= parseTerm(x, y)
            else break
        }
        return v
    }

    private fun parseTerm(x: Double, y: Double): Double {
        var v = parseFactor(x, y)
        while (true) {
            if (eat('*'.code)) {
                v *= parseFactor(x, y)
            } else if (eat('/'.code)) {
                val next = parseFactor(x, y)
                v = if (next == 0.0) Double.NaN else v / next
            } else {
                break
            }
        }
        return v
    }

    private fun parseFactor(x: Double, y: Double): Double {
        var v = parsePrimary(x, y)
        if (eat('^'.code)) {
            v = v.pow(parseExpression(x, y)) // right-associative power
        }
        return v
    }

    private fun parsePrimary(x: Double, y: Double): Double {
        if (eat('+'.code)) return parsePrimary(x, y)
        if (eat('-'.code)) return -parsePrimary(x, y)

        var v: Double
        val startPos = this.pos

        if (eat('('.code)) {
            v = parseExpression(x, y)
            if (!eat(')'.code)) {
                throw IllegalArgumentException("Thiếu dấu ngoặc đóng ')'")
            }
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                nextChar()
            }
            v = normalizedExpr.substring(startPos, this.pos).toDouble()
        } else if ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code)) {
            while ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code)) {
                nextChar()
            }
            val name = normalizedExpr.substring(startPos, this.pos).lowercase()
            when (name) {
                "x" -> v = x
                "y" -> v = y
                "pi" -> v = Math.PI
                "e" -> v = Math.E
                else -> {
                    // It must be a math function of form name(arg) or name primary
                    // Let's check for parentheses for functions. If there are parentheses, fine. If not, parse primary.
                    val arg = parsePrimary(x, y)
                    v = when (name) {
                        "sin" -> sin(arg)
                        "cos" -> cos(arg)
                        "tan" -> tan(arg)
                        "cot" -> 1.0 / tan(arg)
                        "sqrt" -> if (arg < 0) Double.NaN else sqrt(arg)
                        "abs" -> abs(arg)
                        "exp" -> exp(arg)
                        "ln" -> if (arg <= 0) Double.NaN else ln(arg)
                        "log" -> if (arg <= 0) Double.NaN else log10(arg)
                        "asin" -> asin(arg)
                        "acos" -> acos(arg)
                        "atan" -> atan(arg)
                        else -> throw IllegalArgumentException("Hàm số hoặc hằng số không xác định: '$name'")
                    }
                }
            }
        } else {
            throw IllegalArgumentException("Kí tự không hợp lệ: '" + ch.toChar() + "'")
        }

        return v
    }
}
